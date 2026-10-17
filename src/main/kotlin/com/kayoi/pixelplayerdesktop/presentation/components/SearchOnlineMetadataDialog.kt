package com.kayoi.pixelplayerdesktop.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.data.network.musicbrainz.OnlineTrackMetadata
import com.kayoi.pixelplayerdesktop.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOnlineMetadataDialog(
    initialTitle: String,
    initialArtist: String,
    viewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onMetadataSelected: (metadata: OnlineTrackMetadata, coverArtBytes: ByteArray?) -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("$initialTitle $initialArtist".trim()) }
    var isSearching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineTrackMetadata>>(emptyList()) }
    var selectedMatch by remember { mutableStateOf<OnlineTrackMetadata?>(null) }
    var isLoadingCoverArt by remember { mutableStateOf(false) }
    var coverArtBytes by remember { mutableStateOf<ByteArray?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    fun performSearch(query: String) {
        if (query.isBlank()) return
        isSearching = true
        selectedMatch = null
        coverArtBytes = null
        hasSearched = true
        coroutineScope.launch {
            val list = if (initialTitle.isNotBlank() && initialArtist.isNotBlank() && query == "$initialTitle $initialArtist".trim()) {
                val direct = viewModel.searchOnlineMetadata(initialTitle, initialArtist)
                if (direct.isNotEmpty()) direct else viewModel.searchOnlineMetadataByQuery(query)
            } else {
                viewModel.searchOnlineMetadataByQuery(query)
            }
            results = list
            selectedMatch = list.firstOrNull()
            isSearching = false

            if (selectedMatch?.releaseId != null) {
                isLoadingCoverArt = true
                coverArtBytes = viewModel.fetchOnlineCoverArt(selectedMatch!!.releaseId!!)
                isLoadingCoverArt = false
            }
        }
    }

    LaunchedEffect(Unit) {
        performSearch(searchQuery)
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(760.dp)
                .heightIn(max = 660.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MusicBrainz Auto-Tagger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Search online database for accurate track metadata and cover art",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search title, artist, or album") },
                        modifier = Modifier.weight(1f).trackInputFocus(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainer,
                            unfocusedContainerColor = colors.surfaceContainer
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { performSearch(searchQuery) },
                        enabled = !isSearching && searchQuery.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val listState = rememberLazyListState()
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        when {
                            isSearching -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                        Text("Searching MusicBrainz...", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                    }
                                }
                            }
                            results.isEmpty() && hasSearched -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No matches found. Try another query.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                                }
                            }
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(results) { item ->
                                        val isSelected = selectedMatch == item
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedMatch = item
                                                    if (item.releaseId != null) {
                                                        coroutineScope.launch {
                                                            isLoadingCoverArt = true
                                                            coverArtBytes = viewModel.fetchOnlineCoverArt(item.releaseId)
                                                            isLoadingCoverArt = false
                                                        }
                                                    } else {
                                                        coverArtBytes = null
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val subText = buildString {
                                                        append(item.artist)
                                                        if (!item.album.isNullOrBlank()) {
                                                            append(" • ")
                                                            append(item.album)
                                                        }
                                                        if (item.year != null && item.year > 0) {
                                                            append(" (${item.year})")
                                                        }
                                                    }
                                                    Text(
                                                        text = subText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSelected) colors.primary else colors.surfaceContainerHighest
                                                ) {
                                                    Text(
                                                        text = "${item.score}%",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                VerticalScrollbar(
                                    adapter = rememberScrollbarAdapter(listState),
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                                    style = defaultScrollbarStyle().copy(
                                        unhoverColor = colors.onSurfaceVariant.copy(alpha = 0.3f),
                                        hoverColor = colors.primary.copy(alpha = 0.8f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val match = selectedMatch
                        if (match == null) {
                            Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                                Text("Select a match to preview", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isLoadingCoverArt -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        coverArtBytes != null -> {
                                            val bmp = remember(coverArtBytes) {
                                                runCatching { SkiaImage.makeFromEncoded(coverArtBytes!!).toComposeImageBitmap() }.getOrNull()
                                            }
                                            if (bmp != null) {
                                                Image(
                                                    bitmap = bmp,
                                                    contentDescription = "Cover Art",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Rounded.Album, contentDescription = null, tint = colors.onSurfaceVariant, modifier = Modifier.size(42.dp))
                                            }
                                        }
                                        else -> Icon(Icons.Rounded.Album, contentDescription = null, tint = colors.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(42.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = match.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = match.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!match.album.isNullOrBlank()) {
                                        Text(
                                            text = "Album: ${match.album}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (match.year != null && match.year > 0) {
                                        Text(text = "Year: ${match.year}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                    if (match.trackNumber != null && match.trackNumber > 0) {
                                        Text(text = "Track: ${match.trackNumber}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                    if (!match.genre.isNullOrBlank()) {
                                        Text(text = "Genre: ${match.genre}", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val match = selectedMatch
                            if (match != null) {
                                onMetadataSelected(match, coverArtBytes)
                                onDismiss()
                            }
                        },
                        enabled = selectedMatch != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to Song")
                    }
                }
            }
        }
    }
}
