package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kayoi.pixelplayerdesktop.presentation.components.ExpressiveSwitch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.utils.DirectoryPicker
import com.kayoi.pixelplayerdesktop.utils.isDirectoryOrParentExcluded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

private val supportedAudioExts = setOf("mp3", "flac", "ogg", "opus", "m4a", "wav", "aac", "wma")

fun countAudioFilesDeep(dir: File, excludedDirectories: List<String> = emptyList()): Int {
    if (!dir.exists() || !dir.isDirectory) return 0
    return try {
        var count = 0
        val rootPath = dir.toPath()
        Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dirPath: Path, attrs: BasicFileAttributes): FileVisitResult {
                val abs = dirPath.toFile().absolutePath
                val isBlocked = isDirectoryOrParentExcluded(abs, excludedDirectories)
                return if (isBlocked && abs != rootPath.toFile().absolutePath) {
                    FileVisitResult.SKIP_SUBTREE
                } else {
                    FileVisitResult.CONTINUE
                }
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val ext = file.fileName.toString().substringAfterLast('.', "").lowercase()
                if (ext in supportedAudioExts) {
                    count++
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        })
        count
    } catch (_: Exception) {
        0
    }
}

@Composable
fun rememberAudioCount(
    dir: File,
    allSongs: List<Song>,
    excludedDirectories: List<String>
): Int {
    val dirPath = dir.absolutePath
    val isExcluded = isDirectoryOrParentExcluded(dirPath, excludedDirectories)

    val indexedCount = remember(allSongs, dirPath) {
        val dirPrefix = if (dirPath.endsWith(File.separator)) dirPath else dirPath + File.separator
        allSongs.count { song ->
            val path = song.path
            path.startsWith(dirPrefix) || File(path).parentFile?.absolutePath == dirPath
        }
    }

    val countState by produceState(initialValue = indexedCount, key1 = dirPath, key2 = allSongs.size, key3 = isExcluded) {
        if (indexedCount > 0 && !isExcluded) {
            value = indexedCount
        } else {
            val deepCount = withContext(Dispatchers.IO) {
                countAudioFilesDeep(dir, excludedDirectories)
            }
            value = deepCount
        }
    }

    return countState
}

@Composable
fun MusicFoldersDetailScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val musicDirectories by viewModel.musicDirectories.collectAsState()
    val excludedDirectories by viewModel.excludedDirectories.collectAsState()
    val allSongs by viewModel.songs.collectAsState()
    var currentDirectory by remember { mutableStateOf<File?>(null) }

    val isAtRoot = currentDirectory == null
    val rootFiles = remember(musicDirectories) { musicDirectories.map { File(it) }.filter { it.exists() && it.isDirectory } }

    val currentSubFolders = remember(currentDirectory, excludedDirectories) {
        currentDirectory?.let { dir ->
            try {
                dir.listFiles { file -> file.isDirectory && !file.isHidden && !file.name.startsWith(".") }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    val breadcrumbs = remember(currentDirectory, musicDirectories) {
        val current = currentDirectory
        if (current == null) {
            emptyList()
        } else {
            val list = mutableListOf<File>()
            var cursor: File? = current
            val rootPaths = musicDirectories.map { File(it).absolutePath }
            while (cursor != null) {
                list.add(cursor)
                if (cursor.absolutePath in rootPaths) break
                cursor = cursor.parentFile
            }
            list.reversed()
        }
    }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4 }
    }

    LaunchedEffect(currentDirectory) {
        listState.scrollToItem(0)
    }

    val folderSubtitle = if (isAtRoot) {
        if (musicDirectories.isEmpty()) "No folders configured"
        else if (excludedDirectories.isEmpty()) "${musicDirectories.size} configured ${if (musicDirectories.size == 1) "folder" else "folders"}"
        else "${musicDirectories.size} configured • ${excludedDirectories.size} excluded"
    } else {
        currentDirectory?.name.orEmpty()
    }

    DesktopBackHandler(enabled = currentDirectory != null) {
        val parent = currentDirectory?.parentFile
        val isParentConfiguredRoot = parent != null && musicDirectories.any { File(it).absolutePath == parent.absolutePath }
        val isCurrentConfiguredRoot = musicDirectories.any { File(it).absolutePath == currentDirectory?.absolutePath }
        if (isCurrentConfiguredRoot) {
            currentDirectory = null
        } else if (parent != null && musicDirectories.any { currentDirectory?.absolutePath?.startsWith(File(it).absolutePath) == true }) {
            currentDirectory = parent
        } else {
            currentDirectory = null
        }
        true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Music Folders",
                subtitle = folderSubtitle,
                horizontalPadding = horizontalPadding
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

        if (!isAtRoot && breadcrumbs.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { currentDirectory = null },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = "Root directories",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )

                        LazyRow(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(breadcrumbs.size, key = { breadcrumbs[it].absolutePath }) { idx ->
                                val folder = breadcrumbs[idx]
                                val isLast = idx == breadcrumbs.lastIndex
                                Surface(
                                    onClick = { currentDirectory = folder },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLast) colors.primaryContainer else Color.Transparent
                                ) {
                                    Text(
                                        text = folder.name.ifEmpty { folder.path },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isLast) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                                if (!isLast) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isAtRoot) {
            item {
                SettingsSubsection(title = "ADD DIRECTORY") {
                    Surface(
                        shape = getGroupShape(0, 1),
                        color = colors.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isCompact = maxWidth < 460.dp

                            if (isCompact) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CreateNewFolder,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Add Music Folder",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Choose a local folder to scan songs and folder tree",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            DirectoryPicker.chooseDirectory { selectedFolder ->
                                                viewModel.addMusicDirectory(selectedFolder.absolutePath)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Folder", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CreateNewFolder,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Add Music Folder",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "Choose a local folder to scan songs and folder tree",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            DirectoryPicker.chooseDirectory { selectedFolder ->
                                                viewModel.addMusicDirectory(selectedFolder.absolutePath)
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Folder", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SettingsSubsection(title = "CONFIGURED DIRECTORIES") {
                    if (rootFiles.isEmpty()) {
                        Surface(
                            shape = getGroupShape(0, 1),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No directories configured. Click 'Add Folder' to add folders to your library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    } else {
                        rootFiles.forEachIndexed { index, folder ->
                            val isBlocked = isDirectoryOrParentExcluded(folder.absolutePath, excludedDirectories)
                            val audioCount = rememberAudioCount(folder, allSongs, excludedDirectories)

                            FileExplorerDirectoryItem(
                                file = folder,
                                audioCount = audioCount,
                                isBlocked = isBlocked,
                                onNavigate = { currentDirectory = folder },
                                onToggleAllowed = {
                                    if (isBlocked) {
                                        val match = excludedDirectories.firstOrNull { File(it).absolutePath == folder.absolutePath }
                                            ?: excludedDirectories.firstOrNull { folder.absolutePath.startsWith(File(it).absolutePath) }
                                        if (match != null) viewModel.removeExcludedDirectory(match)
                                    } else {
                                        viewModel.addExcludedDirectory(folder.absolutePath)
                                    }
                                },
                                onDelete = { viewModel.removeMusicDirectory(folder.absolutePath) },
                                shape = getGroupShape(index, rootFiles.size)
                            )
                        }
                    }
                }
            }
        } else {
            item {
                val currentDir = currentDirectory ?: return@item
                val isCurrentBlocked = isDirectoryOrParentExcluded(currentDir.absolutePath, excludedDirectories)
                val audioCount = rememberAudioCount(currentDir, allSongs, excludedDirectories)

                SettingsSubsection(title = "CURRENT DIRECTORY") {
                    FileExplorerDirectoryItem(
                        file = currentDir,
                        audioCount = audioCount,
                        isBlocked = isCurrentBlocked,
                        onNavigate = {},
                        canNavigate = false,
                        onToggleAllowed = {
                            if (isCurrentBlocked) {
                                val match = excludedDirectories.firstOrNull { File(it).absolutePath == currentDir.absolutePath }
                                    ?: excludedDirectories.firstOrNull { currentDir.absolutePath.startsWith(File(it).absolutePath) }
                                if (match != null) viewModel.removeExcludedDirectory(match)
                            } else {
                                viewModel.addExcludedDirectory(currentDir.absolutePath)
                            }
                        },
                        shape = getGroupShape(0, 1)
                    )
                }
            }

            item {
                SettingsSubsection(title = "SUBFOLDERS") {
                    if (currentSubFolders.isEmpty()) {
                        Surface(
                            shape = getGroupShape(0, 1),
                            color = colors.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No subfolders found in this directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    } else {
                        currentSubFolders.forEachIndexed { index, subFolder ->
                            val isBlocked = isDirectoryOrParentExcluded(subFolder.absolutePath, excludedDirectories)
                            val audioCount = rememberAudioCount(subFolder, allSongs, excludedDirectories)

                            FileExplorerDirectoryItem(
                                file = subFolder,
                                audioCount = audioCount,
                                isBlocked = isBlocked,
                                onNavigate = { currentDirectory = subFolder },
                                onToggleAllowed = {
                                    if (isBlocked) {
                                        val match = excludedDirectories.firstOrNull { File(it).absolutePath == subFolder.absolutePath }
                                            ?: excludedDirectories.firstOrNull { subFolder.absolutePath.startsWith(File(it).absolutePath) }
                                        if (match != null) viewModel.removeExcludedDirectory(match)
                                    } else {
                                        viewModel.addExcludedDirectory(subFolder.absolutePath)
                                    }
                                },
                                shape = getGroupShape(index, currentSubFolders.size)
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
}

@Composable
fun FileExplorerDirectoryItem(
    file: File,
    audioCount: Int,
    isBlocked: Boolean,
    onNavigate: () -> Unit,
    onToggleAllowed: () -> Unit,
    shape: Shape,
    canNavigate: Boolean = true,
    onDelete: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    val containerColor = if (isBlocked) {
        colors.errorContainer.copy(alpha = 0.22f)
    } else {
        colors.surfaceContainer
    }

    val contentColor = if (isBlocked) {
        colors.onSurface.copy(alpha = 0.75f)
    } else {
        colors.onSurface
    }

    val badgeColor = if (isBlocked) {
        colors.error
    } else {
        colors.secondary
    }

    Surface(
        onClick = onNavigate,
        enabled = canNavigate,
        shape = shape,
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isBlocked) colors.errorContainer.copy(alpha = 0.4f) else colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBlocked) Icons.Rounded.FolderOff else Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = if (isBlocked) colors.error else colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = file.name.ifEmpty { file.path },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = when {
                                audioCount == 0 -> "No songs"
                                audioCount == 1 -> "1 song"
                                audioCount > 99 -> "99+ songs"
                                else -> "$audioCount songs"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (isBlocked) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Excluded",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Material 3 Expressive Switch for Include / Exclude
                ExpressiveSwitch(
                    checked = !isBlocked,
                    onCheckedChange = { onToggleAllowed() }
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = colors.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove directory",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                if (canNavigate) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Open folder",
                        tint = colors.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
