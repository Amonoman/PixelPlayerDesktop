package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.scanner.ScanProgress
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistSettingsDetailScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val artistDelimiters by viewModel.artistDelimiters.collectAsState()
    val wordDelimiters by viewModel.wordDelimiters.collectAsState()
    val extractArtistFromTitle by viewModel.extractArtistFromTitle.collectAsState()
    val rescanRequired by viewModel.rescanRequired.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val isScanning = scanProgress is ScanProgress.Scanning

    var newCharDelimiter by remember { mutableStateOf("") }
    var newWordDelimiter by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = colors.surfaceContainerHighest.copy(alpha = 0.5f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Music Management",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
            }
        }

        item {
            androidx.compose.animation.AnimatedVisibility(
                visible = rescanRequired,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                RescanRequiredBanner(
                    isResyncing = isScanning,
                    onRescanClick = onRescanLibrary
                )
            }
        }

        item {
            SettingsSubsection(title = "MULTI-ARTIST PARSING") {
                // 1. Character Delimiters Card
                Surface(
                    shape = getGroupShape(0, 3),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Character Delimiters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Single symbols used to split multiple artists in song tags",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.resetArtistDelimiters() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            artistDelimiters.forEach { delimiter ->
                                InputChip(
                                    selected = true,
                                    onClick = {
                                        if (artistDelimiters.size > 1) {
                                            viewModel.removeArtistDelimiter(delimiter)
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = if (delimiter == " ") "Space ( )" else "\"$delimiter\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = if (artistDelimiters.size > 1) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove delimiter",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                        selectedTrailingIconColor = colors.onPrimaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newCharDelimiter,
                                onValueChange = { newCharDelimiter = it },
                                placeholder = {
                                    Text("Add character delimiter (e.g. / or ;)", style = MaterialTheme.typography.bodyMedium)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newCharDelimiter.isNotBlank()) {
                                            viewModel.addArtistDelimiter(newCharDelimiter)
                                            newCharDelimiter = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FilledIconButton(
                                onClick = {
                                    if (newCharDelimiter.isNotBlank()) {
                                        viewModel.addArtistDelimiter(newCharDelimiter)
                                        newCharDelimiter = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add character delimiter")
                            }
                        }
                    }
                }

                // 2. Word Delimiters Card
                Surface(
                    shape = getGroupShape(1, 3),
                    color = colors.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Word Delimiters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = "Phrases separating featured or collaborating artists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.resetWordDelimiters() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wordDelimiters.forEach { delimiter ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeWordDelimiter(delimiter) },
                                    label = {
                                        Text(
                                            text = "\"$delimiter\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove delimiter",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = colors.secondaryContainer,
                                        selectedLabelColor = colors.onSecondaryContainer,
                                        selectedTrailingIconColor = colors.onSecondaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newWordDelimiter,
                                onValueChange = { newWordDelimiter = it },
                                placeholder = {
                                    Text("Add word delimiter (e.g. feat. or with)", style = MaterialTheme.typography.bodyMedium)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newWordDelimiter.isNotBlank()) {
                                            viewModel.addWordDelimiter(newWordDelimiter)
                                            newWordDelimiter = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            FilledIconButton(
                                onClick = {
                                    if (newWordDelimiter.isNotBlank()) {
                                        viewModel.addWordDelimiter(newWordDelimiter)
                                        newWordDelimiter = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add word delimiter")
                            }
                        }
                    }
                }

                // 3. Extract Artists from Title Switch
                ExpressiveSwitchRow(
                    title = "Extract Artists from Title",
                    subtitle = "Extract featured artists from titles like 'Song (feat. Artist)' or 'Track [with Guest]'",
                    checked = extractArtistFromTitle,
                    onCheckedChange = { viewModel.setExtractArtistFromTitle(it) },
                    leadingIcon = {
                        Icon(Icons.Rounded.LibraryMusic, null, tint = colors.secondary, modifier = Modifier.size(22.dp))
                    },
                    shape = getGroupShape(2, 3)
                )
            }
        }

        item {
            SettingsSubsection(title = "EXAMPLES & TIPS") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = colors.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "How parsing works",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                        }

                        Text(
                            text = "To preserve a delimiter as part of an artist name without splitting, escape it with a double backslash (\\\\). Example: AC\\\\+DC with delimiter \"+\".",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        val samplePairs = listOf(
                            "Calvin Harris, Pharrell Williams" to "[\"Calvin Harris\", \"Pharrell Williams\"]",
                            "Feels (feat. Katy Perry, Big Sean)" to "[\"Katy Perry\", \"Big Sean\"]",
                            "AC\\\\+DC; Queen" to "[\"AC+DC\", \"Queen\"]"
                        )

                        samplePairs.forEach { (raw, parsed) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "• $raw",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                Text(
                                    text = parsed,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescanRequiredBanner(
    isResyncing: Boolean,
    onRescanClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.tertiaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = colors.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Library rescan required",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onTertiaryContainer
                )
                Text(
                    text = "Artist delimiter changes will apply after rescanning your media library",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onTertiaryContainer.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRescanClick,
                enabled = !isResyncing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.tertiary,
                    contentColor = colors.onTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isResyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.onTertiary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isResyncing) "Scanning..." else "Rescan",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
