package com.kayoi.pixelplayerdesktop.data.repository

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.metadata.DesktopMetadataEditor
import com.kayoi.pixelplayerdesktop.data.metadata.EditableSongMetadata
import com.kayoi.pixelplayerdesktop.data.model.Song
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MusicRepositoryMetadataTest {

    @Test
    fun testRepositoryDelegatesToMetadataEditor() = runTest {
        val tempDb = File.createTempFile("test_repo_db", ".db")
        tempDb.deleteOnExit()
        val db = PixelPlayerDatabase(tempDb)
        val musicDao = MusicDao(db)
        val favoritesDao = FavoritesDao(db)
        val playlistDao = LocalPlaylistDao(db)
        val editor = DesktopMetadataEditor(musicDao)

        val repo = MusicRepositoryImpl(
            database = db,
            musicDao = musicDao,
            favoritesDao = favoritesDao,
            playlistDao = playlistDao,
            metadataEditor = editor
        )

        val nonExistentSong = Song(
            id = "test-123",
            title = "Old Song",
            artist = "Old Artist",
            path = "/non/existent/audio.mp3"
        )

        val result = repo.updateSongMetadata(
            song = nonExistentSong,
            metadata = EditableSongMetadata(
                title = "Updated Song",
                artist = "Updated Artist",
                album = "Updated Album"
            )
        )

        assertFalse(result.success)
        assertNotNull(result.error)

        val readResult = repo.readSongMetadataDetails(File(nonExistentSong.path))
        assertTrue(readResult.isFailure)
    }
}
