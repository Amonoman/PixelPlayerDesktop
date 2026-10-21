package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.ui.graphics.vector.ImageVector
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import com.kayoi.pixelplayerdesktop.presentation.screens.LibrarySubDetail

data class SettingSearchItem(
    val title: String,
    val description: String,
    val section: SettingsSection,
    val subDetail: LibrarySubDetail = LibrarySubDetail.NONE,
    val keywords: List<String> = emptyList(),
    val icon: ImageVector? = null
)

object SettingsSearchCatalog {
    val allSettings: List<SettingSearchItem> = listOf(
        // --- LIBRARY: Structure & Details ---
        SettingSearchItem(
            title = "Music Folders",
            description = "Configure scanned audio directories and excluded paths",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.FOLDERS,
            keywords = listOf("directory", "folder", "scan", "paths", "exclude", "storage"),
            icon = Icons.Outlined.Folder
        ),
        SettingSearchItem(
            title = "Artist Delimiters & Formatting",
            description = "Multi-artist splitting delimiters and featuring phrase cleanup",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.ARTISTS,
            keywords = listOf("artist", "delimiters", "feat", "featuring", "split", "separator"),
            icon = Icons.Outlined.Person
        ),
        SettingSearchItem(
            title = "Duplicate Tracks",
            description = "Scan and resolve duplicate song files across folders",
            section = SettingsSection.LIBRARY,
            subDetail = LibrarySubDetail.DUPLICATES,
            keywords = listOf("duplicate", "clean", "clones", "same songs"),
            icon = Icons.Rounded.DeleteForever
        ),
        SettingSearchItem(
            title = "Minimum Song Duration",
            description = "Filter out audio files shorter than specified length (e.g. ringtones)",
            section = SettingsSection.LIBRARY,
            keywords = listOf("duration", "short", "filter", "seconds", "ringtone"),
            icon = Icons.Rounded.Tune
        ),
        SettingSearchItem(
            title = "Minimum Tracks per Album",
            description = "Hide incomplete albums or singles with fewer tracks than threshold",
            section = SettingsSection.LIBRARY,
            keywords = listOf("album", "tracks", "filter", "threshold", "singles"),
            icon = Icons.Rounded.Tune
        ),
        SettingSearchItem(
            title = "Rescan Music Library",
            description = "Scan folders to detect newly added, moved, or deleted audio files",
            section = SettingsSection.LIBRARY,
            keywords = listOf("rescan", "refresh", "scan", "sync", "reload", "library"),
            icon = Icons.Outlined.Sync
        ),
        SettingSearchItem(
            title = "Clear Album Art Cache",
            description = "Delete cached image thumbnails from disk to free up storage",
            section = SettingsSection.LIBRARY,
            keywords = listOf("cache", "clear", "artwork", "thumbnails", "storage", "disk"),
            icon = PixelPlayerIcons.Lyrics
        ),
        SettingSearchItem(
            title = "Deezer Artist Images",
            description = "Fetch high-resolution online artist profile pictures from Deezer",
            section = SettingsSection.LIBRARY,
            keywords = listOf("deezer", "artist", "pictures", "photos", "avatar", "online"),
            icon = Icons.Outlined.Person
        ),
        SettingSearchItem(
            title = "Online Lyrics Fetching",
            description = "Download synchronized and plain lyrics automatically from internet",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lyrics", "online", "download", "lrc", "synced", "internet"),
            icon = PixelPlayerIcons.Lyrics
        ),
        SettingSearchItem(
            title = "Lyrics Source Preference",
            description = "Choose priority between embedded audio tags or online lyrics providers",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lyrics", "priority", "embedded", "source", "provider"),
            icon = PixelPlayerIcons.Lyrics
        ),
        SettingSearchItem(
            title = "Auto-scan .lrc Files",
            description = "Automatically associate external .lrc sidecar files beside audio tracks",
            section = SettingsSection.LIBRARY,
            keywords = listOf("lrc", "sidecar", "lyrics", "external", "files"),
            icon = PixelPlayerIcons.Lyrics
        ),
        SettingSearchItem(
            title = "Lyrics Romanization",
            description = "Show romaji, pinyin, or transliteration for Japanese/CJK text",
            section = SettingsSection.LIBRARY,
            keywords = listOf("romanization", "romaji", "hiragana", "katakana", "kanji", "pinyin", "japanese"),
            icon = Icons.Rounded.AutoAwesome
        ),

        // --- APPEARANCE ---
        SettingSearchItem(
            title = "Dark Mode",
            description = "Toggle dark theme or light theme appearance",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("dark", "light", "theme", "mode", "color", "night"),
            icon = Icons.Rounded.DarkMode
        ),
        SettingSearchItem(
            title = "Dynamic Color",
            description = "Extract UI accents and theme colors from the currently playing album art",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("dynamic", "color", "accent", "album art", "monet", "material"),
            icon = Icons.Rounded.Brush
        ),
        SettingSearchItem(
            title = "Palette Style & Color Accuracy",
            description = "Fine-tune dynamic color scheme harmony style and sampling resolution",
            section = SettingsSection.APPEARANCE,
            subDetail = LibrarySubDetail.PALETTE_STYLE,
            keywords = listOf("palette", "tonal", "vibrant", "expressive", "accuracy", "sampling"),
            icon = Icons.Rounded.Palette
        ),
        SettingSearchItem(
            title = "Player File Info",
            description = "Display bitrate, format, sampling rate, and codec on the full-screen player",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("codec", "bitrate", "flac", "mp3", "sample rate", "audio info", "details"),
            icon = Icons.Rounded.Info
        ),
        SettingSearchItem(
            title = "Lyrics Text Alignment",
            description = "Align lyrics text to left, center, or right on the player screen",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("alignment", "center", "left", "right", "lyrics text"),
            icon = PixelPlayerIcons.Lyrics
        ),
        SettingSearchItem(
            title = "Compact Sidebar",
            description = "Collapse main navigation sidebar into icon-only compact rail",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("sidebar", "compact", "navigation", "rail", "narrow", "bar"),
            icon = Icons.Rounded.ViewWeek
        ),
        SettingSearchItem(
            title = "Default Startup Tab",
            description = "Select default landing page when application starts (Home, Songs, Albums...)",
            section = SettingsSection.APPEARANCE,
            keywords = listOf("default", "startup", "landing", "tab", "initial", "home"),
            icon = Icons.Rounded.Tab
        ),

        // --- PLAYBACK ---
        SettingSearchItem(
            title = "ReplayGain Volume Normalization",
            description = "Normalize track loudness across different recordings to eliminate volume jumps",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("replaygain", "volume", "normalize", "loudness", "gain", "peak"),
            icon = Icons.Rounded.Tune
        ),
        SettingSearchItem(
            title = "ReplayGain Mode",
            description = "Choose between Track Gain (individual track loudness) or Album Gain (album coherence)",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("replaygain", "mode", "track", "album gain"),
            icon = Icons.Rounded.Tune
        ),
        SettingSearchItem(
            title = "Track Crossfade",
            description = "Smoothly blend outgoing and incoming songs with an audio crossfade transition",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("crossfade", "fade", "transition", "blend", "gapless", "overlap"),
            icon = Icons.Rounded.MusicNote
        ),
        SettingSearchItem(
            title = "Crossfade Duration",
            description = "Set the crossfade overlap transition time in seconds",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("crossfade", "duration", "seconds", "length", "fade time"),
            icon = Icons.Rounded.Tune
        ),
        SettingSearchItem(
            title = "Show Queue History",
            description = "Display previously played tracks at the top of the queue side panel",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("history", "queue", "previous", "played", "tracks"),
            icon = Icons.AutoMirrored.Rounded.QueueMusic
        ),
        SettingSearchItem(
            title = "Close to System Tray",
            description = "Keep music playing in background system tray when window is closed",
            section = SettingsSection.PLAYBACK,
            keywords = listOf("tray", "background", "close", "minimize", "system tray", "exit"),
            icon = Icons.Rounded.Layers
        ),

        // --- EQUALIZER ---
        SettingSearchItem(
            title = "Equalizer",
            description = "10-band audio frequency equalizer with customizable slider levels",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("equalizer", "eq", "frequency", "bands", "audio", "sound", "bass", "treble"),
            icon = Icons.Rounded.GraphicEq
        ),
        SettingSearchItem(
            title = "Equalizer Presets",
            description = "Switch between audio presets (Flat, Bass Boost, Vocal, Rock, Electronic...)",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("presets", "bass boost", "vocal", "rock", "profiles", "sound profiles"),
            icon = Icons.Rounded.GraphicEq
        ),
        SettingSearchItem(
            title = "Equalizer Preamp",
            description = "Adjust global preamp compensation gain in decibels",
            section = SettingsSection.EQUALIZER,
            keywords = listOf("preamp", "gain", "decibels", "db", "boost", "loudness"),
            icon = Icons.Rounded.Tune
        ),

        // --- ACCOUNTS & STREAMING ---
        SettingSearchItem(
            title = "ListenBrainz Scrobbler",
            description = "Scrobble listened tracks and history to ListenBrainz profile",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("listenbrainz", "scrobble", "lastfm", "token", "history", "stats"),
            icon = Icons.Rounded.AccountCircle
        ),
        SettingSearchItem(
            title = "Navidrome Subsonic Server",
            description = "Connect to self-hosted Navidrome or Subsonic server to stream remote library",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("navidrome", "subsonic", "stream", "server", "remote", "cloud"),
            icon = Icons.Rounded.AccountCircle
        ),
        SettingSearchItem(
            title = "Jellyfin Media Server",
            description = "Connect to self-hosted Jellyfin server to stream remote music collection",
            section = SettingsSection.ACCOUNTS,
            keywords = listOf("jellyfin", "emby", "stream", "server", "remote", "cloud"),
            icon = Icons.Rounded.AccountCircle
        ),

        // --- ABOUT ---
        SettingSearchItem(
            title = "About PixelPlayer",
            description = "Application version, developer links, build details, and open-source licenses",
            section = SettingsSection.ABOUT,
            keywords = listOf("about", "version", "update", "github", "license", "credits"),
            icon = Icons.Rounded.Info
        ),

        // --- BACKUP & RESTORE ---
        SettingSearchItem(
            title = "Backup & Restore",
            description = "Export and restore playlists, favorites, lyrics, and settings with .pxpl files",
            section = SettingsSection.BACKUP,
            keywords = listOf("backup", "restore", "export", "import", "pxpl", "data", "playlists", "favorites", "migration"),
            icon = Icons.Rounded.SettingsBackupRestore
        )
    )

    fun search(query: String): List<SettingSearchItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val tokens = trimmed.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return allSettings.filter { item ->
            val corpus = "${item.title} ${item.description} ${item.section.title} ${item.keywords.joinToString(" ")}".lowercase()
            tokens.all { token -> corpus.contains(token) }
        }
    }
}
