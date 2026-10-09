package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.model.MusicFolder
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.SortOption
import java.io.File

object FolderTreeBuilder {

    fun buildFolderTree(
        songs: List<Song>,
        rootDirectories: List<String> = emptyList()
    ): List<MusicFolder> {
        if (songs.isEmpty()) return emptyList()

        val validSongs = songs.filter { it.path.isNotBlank() }
        if (validSongs.isEmpty()) return emptyList()

        val folderMap = mutableMapOf<String, TempFolder>()
        val songParentPaths = mutableSetOf<String>()

        for (song in validSongs) {
            val parentPath = normalizePath(File(song.path).parent ?: "")
            if (parentPath.isNotBlank()) {
                songParentPaths.add(parentPath)
                val folder = getOrCreateTempFolder(parentPath, folderMap)
                folder.songs.add(song)
            }
        }

        val roots = if (rootDirectories.isNotEmpty()) {
            normalizeRootPaths(rootDirectories.toSet())
        } else {
            findCommonRoots(songParentPaths)
        }

        if (roots.isEmpty()) {
            return songParentPaths.mapNotNull { buildImmutableFolder(it, folderMap) }
                .filter { it.totalSongCount > 0 }
                .sortedBy { it.name.lowercase() }
        }

        for (rootPath in roots) {
            getOrCreateTempFolder(rootPath, folderMap)
        }

        for (parentPath in songParentPaths) {
            val matchingRoot = roots.firstOrNull { isPathAtOrUnder(parentPath, it) } ?: continue
            var currentPath = parentPath
            while (currentPath.length > matchingRoot.length && isPathAtOrUnder(currentPath, matchingRoot)) {
                val parentOfCurrent = getParentPath(currentPath) ?: break
                if (!isPathAtOrUnder(parentOfCurrent, matchingRoot)) break

                val parentFolder = getOrCreateTempFolder(parentOfCurrent, folderMap)
                parentFolder.subFolderPaths.add(currentPath)

                if (parentOfCurrent == matchingRoot) break
                currentPath = parentOfCurrent
            }
        }

        return roots.flatMap { rootPath ->
            val rootTemp = folderMap[rootPath] ?: return@flatMap emptyList()
            if (rootTemp.subFolderPaths.isNotEmpty()) {
                rootTemp.subFolderPaths.mapNotNull { buildImmutableFolder(it, folderMap) }
            } else if (rootTemp.songs.isNotEmpty()) {
                listOfNotNull(buildImmutableFolder(rootPath, folderMap))
            } else {
                emptyList()
            }
        }.filter { it.totalSongCount > 0 }.sortedBy { it.name.lowercase() }
    }

    fun flattenFolders(folders: List<MusicFolder>): List<MusicFolder> {
        return folders.flatMap { folder ->
            val current = if (folder.songs.isNotEmpty()) listOf(folder) else emptyList()
            current + flattenFolders(folder.subFolders)
        }
    }

    fun sortMusicFolders(folders: List<MusicFolder>, sortOption: SortOption): List<MusicFolder> {
        return when (sortOption) {
            SortOption.FolderNameAZ -> folders.sortedWith(
                compareBy<MusicFolder> { it.name.lowercase() }.thenBy { it.path }
            )
            SortOption.FolderNameZA -> folders.sortedWith(
                compareByDescending<MusicFolder> { it.name.lowercase() }.thenBy { it.path }
            )
            SortOption.FolderSongCountAsc -> folders.sortedWith(
                compareBy<MusicFolder> { it.totalSongCount }.thenBy { it.name.lowercase() }.thenBy { it.path }
            )
            SortOption.FolderSongCountDesc -> folders.sortedWith(
                compareByDescending<MusicFolder> { it.totalSongCount }.thenBy { it.name.lowercase() }.thenBy { it.path }
            )
            SortOption.FolderSubdirCountAsc -> folders.sortedWith(
                compareBy<MusicFolder> { it.totalSubFolderCount }.thenBy { it.name.lowercase() }.thenBy { it.path }
            )
            SortOption.FolderSubdirCountDesc -> folders.sortedWith(
                compareByDescending<MusicFolder> { it.totalSubFolderCount }.thenBy { it.name.lowercase() }.thenBy { it.path }
            )
            else -> folders.sortedWith(
                compareBy<MusicFolder> { it.name.lowercase() }.thenBy { it.path }
            )
        }
    }

    fun sortSongsForFolder(songs: List<Song>, sortOption: SortOption): List<Song> {
        return when (sortOption) {
            SortOption.SongTitleAZ -> songs.sortedWith(
                compareBy<Song> { it.title.lowercase() }.thenBy { it.trackNumber }
            )
            SortOption.SongTitleZA -> songs.sortedWith(
                compareByDescending<Song> { it.title.lowercase() }.thenBy { it.trackNumber }
            )
            SortOption.SongArtist -> songs.sortedWith(
                compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            SortOption.SongArtistDesc -> songs.sortedWith(
                compareByDescending<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            SortOption.SongAlbum -> songs.sortedWith(
                compareBy<Song> { it.album.lowercase() }.thenBy { it.trackNumber }
            )
            SortOption.SongAlbumDesc -> songs.sortedWith(
                compareByDescending<Song> { it.album.lowercase() }.thenBy { it.trackNumber }
            )
            SortOption.SongDuration -> songs.sortedByDescending { it.duration }
            SortOption.SongDurationAsc -> songs.sortedBy { it.duration }
            SortOption.SongDateAdded -> songs.sortedByDescending { it.dateAdded }
            SortOption.SongDateAddedAsc -> songs.sortedBy { it.dateAdded }
            else -> songs.sortedWith(
                compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    private fun findCommonRoots(paths: Set<String>): List<String> {
        if (paths.isEmpty()) return emptyList()
        val normalized = paths.map(::normalizePath).filter { it.isNotBlank() }

        val byPrefix = normalized.groupBy { getPathRootPrefix(it) }

        return byPrefix.values.mapNotNull { groupPaths ->
            findCommonParentPath(groupPaths)
        }.distinct()
    }

    private fun getPathRootPrefix(path: String): String {
        return if (path.length >= 2 && path[1] == ':') {
            path.substring(0, 2).uppercase()
        } else {
            "/"
        }
    }

    private fun findCommonParentPath(paths: List<String>): String? {
        if (paths.isEmpty()) return null
        if (paths.size == 1) {
            val parent = getParentPath(paths.first())
            return parent ?: paths.first()
        }

        val splitPaths = paths.map { p ->
            p.split('/', '\\').filter { it.isNotBlank() }
        }
        val minLen = splitPaths.minOf { it.size }
        val commonParts = mutableListOf<String>()

        for (i in 0 until minLen) {
            val part = splitPaths[0][i]
            if (splitPaths.all { it[i].equals(part, ignoreCase = true) }) {
                commonParts.add(part)
            } else {
                break
            }
        }

        if (commonParts.isEmpty()) {
            return if (paths.first().startsWith("/")) "/" else paths.first().take(3)
        }

        val isAbsoluteUnix = paths.first().startsWith("/")
        val prefix = if (isAbsoluteUnix) "/" + commonParts.joinToString("/") else commonParts.joinToString("/")
        return normalizePath(prefix)
    }

    private fun normalizeRootPaths(rootPaths: Set<String>): List<String> {
        val normalizedRoots = rootPaths.map(::normalizePath).filter { it.isNotBlank() }.distinct()
        return normalizedRoots.filterNot { candidate ->
            normalizedRoots.any { other ->
                other != candidate && isPathAtOrUnder(path = candidate, root = other)
            }
        }
    }

    private fun isPathAtOrUnder(path: String, root: String): Boolean {
        if (path == root) return true
        val prefix = if (root.endsWith(File.separator) || root.endsWith("/")) root else "$root${File.separator}"
        val altPrefix = if (root.endsWith("/")) root else "$root/"
        return path.startsWith(prefix) || path.startsWith(altPrefix)
    }

    private fun getOrCreateTempFolder(path: String, map: MutableMap<String, TempFolder>): TempFolder {
        return map.getOrPut(path) {
            TempFolder(path = path, name = getNameFromPath(path))
        }
    }

    private fun buildImmutableFolder(path: String, map: Map<String, TempFolder>): MusicFolder? {
        val temp = map[path] ?: return null
        val subFolders = temp.subFolderPaths
            .mapNotNull { subPath -> buildImmutableFolder(subPath, map) }
            .sortedBy { it.name.lowercase() }

        val sortedSongs = temp.songs.sortedWith(
            compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title.lowercase() }
        )

        return MusicFolder(
            path = temp.path,
            name = temp.name,
            songs = sortedSongs,
            subFolders = subFolders
        )
    }

    private fun getParentPath(path: String): String? {
        val file = File(path)
        return file.parent?.let(::normalizePath)
    }

    private fun getNameFromPath(path: String): String {
        val file = File(path)
        val name = file.name
        return if (name.isNotBlank()) name else path
    }

    private fun normalizePath(path: String): String {
        val p = path.trim()
        return if (p.length > 1 && (p.endsWith("/") || p.endsWith("\\"))) {
            p.dropLast(1)
        } else {
            p
        }
    }

    private class TempFolder(
        val path: String,
        val name: String,
        val songs: MutableList<Song> = ArrayList(),
        val subFolderPaths: MutableSet<String> = LinkedHashSet()
    )
}
