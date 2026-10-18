package com.kayoi.pixelplayerdesktop.di

import com.kayoi.pixelplayerdesktop.data.database.FavoritesDao
import com.kayoi.pixelplayerdesktop.data.database.ListenBrainzDao
import com.kayoi.pixelplayerdesktop.data.database.LocalPlaylistDao
import com.kayoi.pixelplayerdesktop.data.database.LyricsDao
import com.kayoi.pixelplayerdesktop.data.database.MusicDao
import com.kayoi.pixelplayerdesktop.data.database.PixelPlayerDatabase
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.JellyfinApiClient
import com.kayoi.pixelplayerdesktop.data.network.jellyfin.JellyfinRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.ListenBrainzApiClient
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.ListenBrainzRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.network.listenbrainz.ScrobbleManager
import com.kayoi.pixelplayerdesktop.data.network.navidrome.NavidromeApiClient
import com.kayoi.pixelplayerdesktop.data.network.navidrome.NavidromeRepositoryImpl
import com.kayoi.pixelplayerdesktop.domain.repository.JellyfinRepository
import com.kayoi.pixelplayerdesktop.domain.repository.ListenBrainzRepository
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository
import com.kayoi.pixelplayerdesktop.data.network.RemoteArtworkResolver
import com.kayoi.pixelplayerdesktop.data.player.PlaybackUriResolver
import com.kayoi.pixelplayerdesktop.data.player.QueueManager
import com.kayoi.pixelplayerdesktop.data.player.VlcjAudioEngine
import com.kayoi.pixelplayerdesktop.data.metadata.DesktopMetadataEditor
import com.kayoi.pixelplayerdesktop.data.network.lyrics.LrcLibClient
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.MusicBrainzClient
import com.kayoi.pixelplayerdesktop.data.repository.LyricsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.MusicRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.repository.SettingsRepositoryImpl
import com.kayoi.pixelplayerdesktop.data.scanner.DesktopMusicScanner
import com.kayoi.pixelplayerdesktop.domain.player.AudioEngine
import com.kayoi.pixelplayerdesktop.domain.repository.LyricsRepository
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import com.kayoi.pixelplayerdesktop.domain.repository.SettingsRepository
import com.kayoi.pixelplayerdesktop.platform.mediasession.MediaSessionBridge
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.DesktopListeningStatsTracker
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.LibraryStateHolder
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlaybackStateHolder
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.StatsStateHolder
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.ThemeStateHolder
import com.kayoi.pixelplayerdesktop.utils.DesktopPathProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val coreModule: Module = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single<Json> {
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

val settingsModule: Module = module {
    single<SettingsRepository> { SettingsRepositoryImpl(ioDispatcher = get(), json = get()) }
}

val databaseModule: Module = module {
    single { PixelPlayerDatabase(DesktopPathProvider.databaseFile) }
    single { MusicDao(database = get(), ioDispatcher = get()) }
    single { LocalPlaylistDao(database = get(), ioDispatcher = get(), json = get()) }
    single { FavoritesDao(database = get(), ioDispatcher = get()) }
    single { LyricsDao(database = get(), ioDispatcher = get(), json = get()) }
    single { ListenBrainzDao(database = get(), ioDispatcher = get()) }
}

val repositoryModule: Module = module {
    single { MusicBrainzClient(ioDispatcher = get()) }
    single<MusicRepository> {
        MusicRepositoryImpl(
            database = get(),
            musicDao = get(),
            favoritesDao = get(),
            playlistDao = get(),
            metadataEditor = get(),
            musicBrainzClient = get(),
            ioDispatcher = get()
        )
    }
    single { LrcLibClient(ioDispatcher = get()) }
    single<LyricsRepository> {
        LyricsRepositoryImpl(
            lyricsDao = get(),
            lrcLibClient = get(),
            settingsRepository = get(),
            ioDispatcher = get()
        )
    }
    single { ListenBrainzApiClient(okHttpClient = get()) }
    single<ListenBrainzRepository> { ListenBrainzRepositoryImpl(apiClient = get(), dao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { NavidromeApiClient(okHttpClient = get(), json = get()) }
    single<NavidromeRepository> { NavidromeRepositoryImpl(apiClient = get(), musicDao = get(), playlistDao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { JellyfinApiClient(okHttpClient = get(), json = get()) }
    single<JellyfinRepository> { JellyfinRepositoryImpl(apiClient = get(), musicDao = get(), playlistDao = get(), settingsRepository = get(), coroutineScope = get()) }
    single { ScrobbleManager(listenBrainzRepository = get(), navidromeRepository = get(), settingsRepository = get(), coroutineScope = get()) }
    single { RemoteArtworkResolver(navidromeRepository = get(), jellyfinRepository = get(), okHttpClient = get()) }
    single { PlaybackUriResolver(navidromeRepository = get(), jellyfinRepository = get()) }
    single { DesktopMusicScanner(musicDao = get(), lyricsDao = get(), ioDispatcher = get()) }
    single { DesktopMetadataEditor(musicDao = get(), ioDispatcher = get()) }
}

val playerModule: Module = module {
    single<AudioEngine> { VlcjAudioEngine(coroutineScope = get()) }
    single { QueueManager() }
}

val stateModule: Module = module {
    single { DesktopListeningStatsTracker(musicRepository = get(), scrobbleManager = get(), coroutineScope = get()) }
    single {
        PlaybackStateHolder(
            audioEngine = get(),
            queueManager = get(),
            musicRepository = get(),
            lyricsRepository = get(),
            settingsRepository = get(),
            listeningStatsTracker = get(),
            playbackUriResolver = get(),
            coroutineScope = get()
        )
    }
    single {
        LibraryStateHolder(
            musicRepository = get(),
            settingsRepository = get(),
            lyricsRepository = get(),
            scanner = get(),
            coroutineScope = get()
        )
    }
    single {
        ThemeStateHolder(
            settingsRepository = get(),
            coroutineScope = get()
        )
    }
    single {
        StatsStateHolder(
            musicRepository = get(),
            coroutineScope = get()
        )
    }
    single {
        MediaSessionBridge(
            playbackStateHolder = get(),
            coroutineScope = get()
        )
    }
    single {
        PlayerViewModel(
            playbackStateHolder = get(),
            libraryStateHolder = get(),
            themeStateHolder = get(),
            statsStateHolder = get(),
            mediaSessionBridge = get()
        )
    }
}

val appModules: List<Module> = listOf(
    coreModule,
    settingsModule,
    databaseModule,
    repositoryModule,
    playerModule,
    stateModule
)
