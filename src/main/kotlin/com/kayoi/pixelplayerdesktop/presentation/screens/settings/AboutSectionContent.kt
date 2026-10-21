package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import com.kayoi.pixelplayerdesktop.BuildConfig
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kayoi.pixelplayerdesktop.presentation.icons.PixelPlayerIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI

private data class ProjectLink(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val url: String
)

private data class Contributor(
    val id: String,
    val displayName: String,
    val role: String,
    val detail: String,
    val badge: String,
    val avatarUrl: String,
    val githubUrl: String? = null,
    val telegramUrl: String? = null
)

private val MaintainersList = listOf(
    Contributor(
        id = "adlifarizi",
        displayName = "@adlifarizi",
        role = "Desktop Maintainer",
        detail = "Maintains PixelPlayer Desktop. GitHub and Telegram: @adlifarizi.",
        badge = "Desktop app",
        avatarUrl = "https://github.com/adlifarizi.png",
        githubUrl = "https://github.com/adlifarizi",
        telegramUrl = "https://t.me/adlifarizi"
    ),
    Contributor(
        id = "theovilardo",
        displayName = "@theovilardo",
        role = "Author / Original Creator",
        detail = "Author and creator of the original Google Play PixelPlayer app.",
        badge = "Original app",
        avatarUrl = "https://github.com/theovilardo.png",
        githubUrl = "https://github.com/theovilardo"
    )
)

private val ProjectLinks = listOf(
    ProjectLink(
        id = "source",
        title = "Source code",
        subtitle = "github.com/adlifarizi/PixelPlayerDesktop",
        icon = PixelPlayerIcons.GitHub,
        url = "https://github.com/adlifarizi/PixelPlayerDesktop"
    ),
    ProjectLink(
        id = "releases",
        title = "Releases & Updates",
        subtitle = "Check latest desktop builds and changelog",
        icon = Icons.Rounded.SystemUpdate,
        url = "https://github.com/adlifarizi/PixelPlayerDesktop/releases"
    ),
    ProjectLink(
        id = "upstream_foss",
        title = "PixelPlayer OSS (Android)",
        subtitle = "Original mobile upstream edition by lostf1sh",
        icon = Icons.Rounded.PhoneAndroid,
        url = "https://github.com/lostf1sh/PixelPlayerOSS"
    )
)

private val avatarMemoryCache = mutableMapOf<String, ImageBitmap>()

private suspend fun fetchAvatarBitmap(url: String): ImageBitmap? {
    avatarMemoryCache[url]?.let { return it }
    return withContext(Dispatchers.IO) {
        runCatching {
            val uri = URI(url)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.setRequestProperty("User-Agent", "PixelPlayerDesktop")
            val bytes = connection.inputStream.use { it.readBytes() }
            val bitmap = SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
            avatarMemoryCache[url] = bitmap
            bitmap
        }.getOrNull()
    }
}

private fun openBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        } else {
            Runtime.getRuntime().exec(arrayOf("xdg-open", url))
        }
    } catch (_: Exception) {
        runCatching { Runtime.getRuntime().exec(arrayOf("xdg-open", url)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutSectionContent(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer
                    ) {
                        Icon(
                            imageVector = PixelPlayerIcons.AppLogo,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PixelPlayer Desktop",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Local-first music playback crafted with Material 3 expressive design.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = colors.tertiaryContainer
                ) {
                    Text(
                        text = "Version ${BuildConfig.APP_VERSION_NAME}",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onTertiaryContainer
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val signals = listOf(
                        "Open source" to Icons.Rounded.Public,
                        "Community-first" to Icons.Rounded.AutoAwesome,
                        "Material 3 expressive" to Icons.Rounded.Palette
                    )
                    signals.forEach { (label, icon) ->
                        Surface(
                            shape = CircleShape,
                            color = colors.surfaceContainerHigh.copy(alpha = 0.9f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsSubsection(title = "PROJECT") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                ProjectLinks.forEachIndexed { index, link ->
                    Surface(
                        shape = getGroupShape(index, ProjectLinks.size),
                        color = colors.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openBrowser(link.url) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.primaryContainer
                            ) {
                                Icon(
                                    imageVector = link.icon,
                                    contentDescription = null,
                                    tint = colors.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = link.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = link.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        SettingsSubsection(title = "MAINTAINERS & AUTHORS") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                MaintainersList.forEachIndexed { index, contributor ->
                    ContributorCard(
                        contributor = contributor,
                        shape = getGroupShape(index, MaintainersList.size),
                        onOpenUrl = ::openBrowser
                    )
                }
            }
        }
    }
}

@Composable
private fun ContributorCard(
    contributor: Contributor,
    shape: androidx.compose.ui.graphics.Shape,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var avatarBitmap by remember(contributor.avatarUrl) { mutableStateOf(avatarMemoryCache[contributor.avatarUrl]) }

    LaunchedEffect(contributor.avatarUrl) {
        if (avatarBitmap == null) {
            avatarBitmap = fetchAvatarBitmap(contributor.avatarUrl)
        }
    }

    Surface(
        shape = shape,
        color = colors.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = contributor.githubUrl != null) {
                contributor.githubUrl?.let(onOpenUrl)
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = colors.surfaceContainerHigh
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap!!,
                        contentDescription = contributor.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val initial = contributor.displayName.removePrefix("@").firstOrNull()?.uppercase() ?: "?"
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = contributor.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.primary
                )
                Text(
                    text = contributor.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Surface(
                    shape = CircleShape,
                    color = colors.secondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = contributor.badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSecondaryContainer
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (contributor.githubUrl != null) {
                    IconButton(
                        onClick = { onOpenUrl(contributor.githubUrl) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = PixelPlayerIcons.GitHub,
                            contentDescription = "GitHub profile",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (contributor.telegramUrl != null) {
                    IconButton(
                        onClick = { onOpenUrl(contributor.telegramUrl) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = PixelPlayerIcons.Telegram,
                            contentDescription = "Telegram profile",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
