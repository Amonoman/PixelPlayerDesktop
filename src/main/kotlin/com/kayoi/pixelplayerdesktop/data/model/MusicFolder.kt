package com.kayoi.pixelplayerdesktop.data.model

data class MusicFolder(
    val path: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val subFolders: List<MusicFolder> = emptyList()
) {
    val totalSongCount: Int by lazy {
        songs.size + subFolders.sumOf { it.totalSongCount }
    }

    val totalSubFolderCount: Int by lazy {
        subFolders.size + subFolders.sumOf { it.totalSubFolderCount }
    }

    fun collectAllSongs(): List<Song> = songs + subFolders.flatMap { it.collectAllSongs() }
}
