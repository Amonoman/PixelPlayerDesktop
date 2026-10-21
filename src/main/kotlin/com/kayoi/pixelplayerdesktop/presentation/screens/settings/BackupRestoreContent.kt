package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.backup.BackupManager
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupOperationType
import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSection
import com.kayoi.pixelplayerdesktop.data.backup.model.RestorePlan
import com.kayoi.pixelplayerdesktop.data.backup.model.RestoreResult
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreContent(
    modifier: Modifier = Modifier,
    backupManager: BackupManager = koinInject()
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val progressUpdate by backupManager.progressFlow.collectAsState()

    var exportSections by remember { mutableStateOf(BackupSection.defaultSelection) }
    var exportPassword by remember { mutableStateOf("") }
    var isPasswordEnabled by remember { mutableStateOf(false) }

    var showExportSectionDialog by remember { mutableStateOf(false) }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }
    var currentRestorePlan by remember { mutableStateOf<RestorePlan?>(null) }
    var restorePassphrase by remember { mutableStateOf<String?>(null) }

    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackupInfoNoticeCard()

        SettingsSubsection(title = "CREATE BACKUP") {
            val summaryText = when {
                exportSections.isEmpty() -> "No sections selected"
                exportSections.size == BackupSection.defaultSelection.size -> "All default sections selected (${exportSections.size})"
                else -> "${exportSections.size} sections selected"
            }

            ExpressiveSettingsRow(
                title = "Backup Sections",
                subtitle = summaryText,
                leadingIcon = {
                    Icon(Icons.Outlined.FileUpload, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                },
                trailingIcon = {
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
                },
                shape = getGroupShape(0, 3),
                onClick = { showExportSectionDialog = true }
            )

            Surface(
                shape = getGroupShape(1, 3),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Password Protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Encrypt backup with AES-256-GCM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isPasswordEnabled,
                            onCheckedChange = { isPasswordEnabled = it }
                        )
                    }

                    AnimatedVisibility(visible = isPasswordEnabled) {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = { Text("Backup Passphrase") },
                            placeholder = { Text("Enter a secure password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }
                }
            }

            val canExport = exportSections.isNotEmpty() && !isProcessing && (!isPasswordEnabled || exportPassword.isNotBlank())

            Surface(
                shape = getGroupShape(2, 3),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            DirectoryPicker.saveBackupFile(
                                defaultFileName = "PixelPlayer_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pxpl"
                            ) { targetFile ->
                                coroutineScope.launch {
                                    isProcessing = true
                                    val passphrase = if (isPasswordEnabled && exportPassword.isNotBlank()) exportPassword else null
                                    val result = backupManager.export(targetFile, exportSections, passphrase)
                                    isProcessing = false
                                    if (result.isSuccess) {
                                        statusMessage = Pair(true, "Backup saved to: ${targetFile.name}")
                                    } else {
                                        statusMessage = Pair(false, "Export failed: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        },
                        enabled = canExport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                            disabledContainerColor = colors.surfaceContainerHighest,
                            disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.FileUpload,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (canExport) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export .pxpl File")
                    }
                }
            }
        }

        SettingsSubsection(title = "RESTORE BACKUP") {
            Surface(
                shape = getGroupShape(0, 1),
                color = colors.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            contentDescription = null,
                            tint = colors.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Restore from .pxpl File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Import playlists, favorites, lyrics, and settings",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            DirectoryPicker.chooseBackupFile { selectedFile ->
                                coroutineScope.launch {
                                    pendingRestoreFile = selectedFile
                                    if (backupManager.isBackupEncrypted(selectedFile)) {
                                        showPasswordPromptDialog = true
                                    } else {
                                        isProcessing = true
                                        val inspect = backupManager.inspectBackup(selectedFile, null)
                                        isProcessing = false
                                        if (inspect.isSuccess) {
                                            currentRestorePlan = inspect.getOrThrow()
                                        } else {
                                            statusMessage = Pair(false, "Failed to read backup: ${inspect.exceptionOrNull()?.message}")
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary,
                            disabledContainerColor = colors.surfaceContainerHighest,
                            disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                    ) {
                        Icon(
                            Icons.Outlined.FileDownload,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (!isProcessing) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Select .pxpl File")
                    }
                }
            }
        }
    }

    // Dialogs
    if (showExportSectionDialog) {
        BackupSectionSelectionDialog(
            selected = exportSections,
            onDismiss = { showExportSectionDialog = false },
            onConfirm = {
                exportSections = it
                showExportSectionDialog = false
            }
        )
    }

    if (showPasswordPromptDialog && pendingRestoreFile != null) {
        PasswordPromptDialog(
            onDismiss = {
                showPasswordPromptDialog = false
                pendingRestoreFile = null
            },
            onConfirm = { password ->
                showPasswordPromptDialog = false
                restorePassphrase = password
                val file = pendingRestoreFile ?: return@PasswordPromptDialog
                coroutineScope.launch {
                    isProcessing = true
                    val inspect = backupManager.inspectBackup(file, password)
                    isProcessing = false
                    if (inspect.isSuccess) {
                        currentRestorePlan = inspect.getOrThrow()
                    } else {
                        statusMessage = Pair(false, "Incorrect password or corrupted archive.")
                    }
                }
            }
        )
    }

    currentRestorePlan?.let { plan ->
        RestorePlanDialog(
            plan = plan,
            onDismiss = {
                currentRestorePlan = null
                pendingRestoreFile = null
                restorePassphrase = null
            },
            onConfirm = { updatedPlan ->
                val file = pendingRestoreFile ?: File(updatedPlan.backupFilePath)
                currentRestorePlan = null
                coroutineScope.launch {
                    isProcessing = true
                    val result = backupManager.restore(file, updatedPlan, restorePassphrase)
                    isProcessing = false
                    pendingRestoreFile = null
                    restorePassphrase = null

                    when (result) {
                        is RestoreResult.Success -> {
                            statusMessage = Pair(true, "All selected modules restored successfully!")
                        }
                        is RestoreResult.TotalFailure -> {
                            statusMessage = Pair(false, result.error)
                        }
                        is RestoreResult.PartialFailure -> {
                            statusMessage = Pair(false, "Restore partially failed: ${result.failed}")
                        }
                    }
                }
            }
        )
    }

    if (isProcessing && progressUpdate != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(progressUpdate?.title ?: "Processing...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        progressUpdate?.detail ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { progressUpdate?.progress ?: 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {}
        )
    }

    statusMessage?.let { (isSuccess, message) ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            icon = {
                Icon(
                    imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = if (isSuccess) colors.primary else colors.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(if (isSuccess) "Success" else "Notice") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun BackupInfoNoticeCard() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.secondaryContainer.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(24.dp).padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Desktop Backup & Restore (.pxpl)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSecondaryContainer
                )
                Text(
                    text = "Export and restore playlists, favorites, lyrics, equalizer presets, and settings across your PCs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSecondaryContainer.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun BackupSectionCard(
    section: BackupSection,
    selected: Boolean,
    onToggle: () -> Unit,
    detailText: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val borderColor = if (selected) colors.primary.copy(alpha = 0.8f) else colors.outlineVariant.copy(alpha = 0.4f)
    val borderWidth = if (selected) 2.dp else 1.dp
    val iconContainerColor = if (selected) colors.secondaryContainer else colors.surfaceContainerHighest
    val iconTint = if (selected) colors.primary else colors.onSurfaceVariant

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.surfaceContainerLow else colors.surfaceContainerLowest,
        border = BorderStroke(width = borderWidth, color = borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = iconContainerColor,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getSectionIcon(section),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = section.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (!detailText.isNullOrBlank()) {
                    Text(
                        text = detailText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.tertiary
                    )
                }
            }

            Switch(
                checked = selected,
                onCheckedChange = { onToggle() },
                thumbContent = {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                    checkedIconColor = colors.primary,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                    uncheckedTrackColor = colors.surfaceVariant,
                    uncheckedIconColor = colors.surfaceVariant
                )
            )
        }
    }
}

private fun getSectionIcon(section: BackupSection): ImageVector = when (section) {
    BackupSection.PLAYLISTS -> Icons.AutoMirrored.Rounded.QueueMusic
    BackupSection.GLOBAL_SETTINGS -> Icons.Rounded.Settings
    BackupSection.FAVORITES -> Icons.Rounded.Favorite
    BackupSection.LYRICS -> PixelPlayerIcons.Lyrics
    BackupSection.EQUALIZER -> Icons.Rounded.GraphicEq
    BackupSection.PLAYBACK_HISTORY -> Icons.Rounded.History
    BackupSection.ARTIST_IMAGES -> PixelPlayerIcons.Artist
    BackupSection.SEARCH_HISTORY -> Icons.Rounded.Search
    BackupSection.ENGAGEMENT_STATS -> Icons.Outlined.AutoGraph
    BackupSection.TRANSITIONS -> Icons.Rounded.Shuffle
    BackupSection.QUICK_FILL -> Icons.Rounded.Folder
    BackupSection.AUDIO_BOOKMARKS -> Icons.Rounded.Bookmark
}

@Composable
private fun BackupSectionSelectionDialog(
    selected: Set<BackupSection>,
    onDismiss: () -> Unit,
    onConfirm: (Set<BackupSection>) -> Unit
) {
    var currentSelection by remember { mutableStateOf(selected) }
    val availableSections = remember { BackupSection.defaultSelection.toList() }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Select Backup Sections",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentSelection.size} of ${availableSections.size} sections selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        currentSelection = if (currentSelection.size == availableSections.size) {
                            emptySet()
                        } else {
                            availableSections.toSet()
                        }
                    }
                ) {
                    Text(if (currentSelection.size == availableSections.size) "Deselect All" else "Select All")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableSections, key = { it.key }) { section ->
                    val isSelected = currentSelection.contains(section)
                    BackupSectionCard(
                        section = section,
                        selected = isSelected,
                        onToggle = {
                            currentSelection = if (isSelected) {
                                currentSelection - section
                            } else {
                                currentSelection + section
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentSelection) },
                enabled = currentSelection.isNotEmpty()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypted Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("This backup is protected with a password. Please enter the passphrase to inspect and restore it.")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RestorePlanDialog(
    plan: RestorePlan,
    onDismiss: () -> Unit,
    onConfirm: (RestorePlan) -> Unit
) {
    var selectedModules by remember { mutableStateOf(plan.selectedModules) }
    val formattedDate = remember(plan.manifest.createdAt) {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(plan.manifest.createdAt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore Inspection") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Created: $formattedDate", style = MaterialTheme.typography.bodySmall)
                        Text("App Version: ${plan.manifest.appVersion.ifBlank { "Unknown" }} (Schema v${plan.manifest.schemaVersion})", style = MaterialTheme.typography.bodySmall)
                        if (plan.manifest.deviceInfo.manufacturer.isNotBlank()) {
                            Text("Source Device: ${plan.manifest.deviceInfo.manufacturer} ${plan.manifest.deviceInfo.model}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Text("Available Modules to Restore:", fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(plan.availableModules.toList(), key = { it.key }) { section ->
                        val isSelected = selectedModules.contains(section)
                        val details = plan.moduleDetails[section]
                        val detailText = if (details != null && details.entryCount > 0) {
                            "${details.entryCount} items • ${details.sizeBytes / 1024} KB"
                        } else null

                        BackupSectionCard(
                            section = section,
                            selected = isSelected,
                            onToggle = {
                                selectedModules = if (isSelected) {
                                    selectedModules - section
                                } else {
                                    selectedModules + section
                                }
                            },
                            detailText = detailText
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(plan.copy(selectedModules = selectedModules)) },
                enabled = selectedModules.isNotEmpty()
            ) {
                Text("Restore Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
