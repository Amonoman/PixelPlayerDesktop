package com.kayoi.pixelplayerdesktop.data.backup.module

import com.kayoi.pixelplayerdesktop.data.backup.model.BackupSongMetadata
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.model.Song

class BackupSongResolver(private val musicDao: MusicDao) {

    suspend fun metadataFor(songIds: Collection<String>): Map<String, BackupSongMetadata> {
        if (songIds.isEmpty()) return emptyMap()
        val songs = musicDao.getSongsByIds(songIds.distinct())
        return songs.associate { song ->
            song.id to BackupSongMetadata(
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration
            )
        }
    }

    suspend fun songsFor(songIds: Collection<String>): Map<String, Song> {
        if (songIds.isEmpty()) return emptyMap()
        return musicDao.getSongsByIds(songIds.distinct()).associateBy { it.id }
    }

    suspend fun library(): Library = Library(musicDao.getAllSongs())

    class Library(summaries: List<Song>) {
        private val byId = summaries.associateBy { it.id }
        private val byTitleArtist = summaries.groupBy { matchKey(it.title, it.artist) }
        private val byTitle = summaries.groupBy { normalize(it.title) }

        fun songIdsTitled(title: String): List<String> =
            byTitle[normalize(title)].orEmpty().map { it.id }

        fun resolve(backupSongId: String, meta: BackupSongMetadata?): String? {
            val directMatch = byId[backupSongId]
            if (directMatch != null && (meta == null || metadataMatches(meta, directMatch))) {
                return backupSongId
            }
            if (meta == null) return null

            val candidates = byTitleArtist[matchKey(meta.title, meta.artist)] ?: return null
            if (candidates.size == 1) return candidates[0].id

            val albumMatch = candidates.filter { normalize(it.album) == normalize(meta.album) }
            if (albumMatch.size == 1) return albumMatch[0].id

            val durationMatch = albumMatch.ifEmpty { candidates }.filter {
                kotlin.math.abs(it.duration - meta.duration) <= DURATION_TOLERANCE_MS
            }
            return durationMatch.singleOrNull()?.id
        }

        private fun metadataMatches(meta: BackupSongMetadata, song: Song): Boolean =
            normalize(meta.title) == normalize(song.title) && normalize(meta.artist) == normalize(song.artist)
    }

    companion object {
        const val DURATION_TOLERANCE_MS = 2000L
        fun normalize(text: String): String = text.trim().lowercase()
        fun matchKey(title: String, artist: String): String = "${normalize(title)}|${normalize(artist)}"
    }
}
