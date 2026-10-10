package com.kayoi.pixelplayerdesktop.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.domain.DailyMixManager
import com.kayoi.pixelplayerdesktop.presentation.components.AlbumArtCollage
import com.kayoi.pixelplayerdesktop.presentation.components.CollagePattern
import com.kayoi.pixelplayerdesktop.presentation.components.DailyMixSection
import com.kayoi.pixelplayerdesktop.presentation.components.RecentlyPlayedSection

@Composable
fun HomeScreen(
    songs: List<Song>,
    favoriteSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongSelect: (Song) -> Unit,
    onShuffleAll: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onPlayNext: ((Song) -> Unit)? = null,
    onAddToQueue: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onToggleFavorite: ((Song) -> Unit)? = null,
    onDelete: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val dailyMixManager = remember { DailyMixManager() }

    val favoriteIds = remember(favoriteSongs) {
        favoriteSongs.map { it.id }.toSet()
    }

    val yourMixSongs = remember(songs, favoriteIds) {
        val mix = dailyMixManager.generateYourMix(songs, favoriteIds, limit = 30)
        if (mix.isNotEmpty()) mix else songs
    }

    val dailyMixSongs = remember(songs) {
        dailyMixManager.generateDailyMix(songs, limit = 8)
    }

    val recentlyPlayedSongs = remember(songs) {
        songs.take(12)
    }

    val titleStyle = rememberYourMixTitleStyle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {

        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 760.dp

                    if (isNarrow) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.Start
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy((-18).dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.wrapContentWidth()) {
                                    Text(
                                        text = "Your\nMix",
                                        style = titleStyle,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = "Today's Mix for you",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 2.dp)
                                        .size(116.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onShuffleAll),
                                    shape = CircleShape,
                                    color = colors.tertiaryContainer,
                                    contentColor = colors.onTertiaryContainer,
                                    tonalElevation = 6.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle Play",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtCollage(
                                    songs = yourMixSongs,
                                    modifier = Modifier.width(360.dp).height(260.dp),
                                    height = 260.dp,
                                    pattern = CollagePattern.COSMIC_SWIRL,
                                    onSongClick = onSongSelect
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 36.dp, vertical = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 24.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy((-24).dp)
                            ) {
                                Column(modifier = Modifier.wrapContentWidth()) {
                                    Text(
                                        text = "Your\nMix",
                                        style = titleStyle,
                                        color = colors.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = "Today's Mix for you",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                Surface(
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .size(116.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onShuffleAll),
                                    shape = CircleShape,
                                    color = colors.tertiaryContainer,
                                    contentColor = colors.onTertiaryContainer,
                                    tonalElevation = 6.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Shuffle,
                                            contentDescription = "Shuffle Play",
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(380.dp)
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AlbumArtCollage(
                                    songs = yourMixSongs,
                                    modifier = Modifier.fillMaxSize(),
                                    height = 300.dp,
                                    pattern = CollagePattern.COSMIC_SWIRL,
                                    onSongClick = onSongSelect
                                )
                            }
                        }
                    }
                }
            }
        }

        if (dailyMixSongs.isNotEmpty()) {
            item {
                DailyMixSection(
                    songs = dailyMixSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = onSongSelect,
                    onPlayMix = {
                        if (dailyMixSongs.isNotEmpty()) onSongSelect(dailyMixSongs.first())
                    },
                    onSeeAllClick = onNavigateToLibrary,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onToggleFavorite = onToggleFavorite,
                    onDelete = onDelete
                )
            }
        }

        if (recentlyPlayedSongs.isNotEmpty()) {
            item {
                RecentlyPlayedSection(
                    songs = recentlyPlayedSongs,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = onSongSelect,
                    onSeeAllClick = onNavigateToLibrary
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberYourMixTitleStyle(): TextStyle {
    return remember {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    "font/gflex_variable.ttf",
                    weight = FontWeight(760),
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(636),
                        FontVariation.width(152f),
                        FontVariation.Setting("ROND", 50f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(760),
            fontSize = 78.sp,
            lineHeight = 74.sp
        )
    }
}
