package com.kayoi.pixelplayerdesktop.presentation.screens.settings

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import com.kayoi.pixelplayerdesktop.generated.resources.Res
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_article
import com.kayoi.pixelplayerdesktop.generated.resources.ic_automirrored_rounded_open_in_new
import com.kayoi.pixelplayerdesktop.generated.resources.ic_outlined_content_copy
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_balance
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_category
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_check
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_chevron_right
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_clear
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_close
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_code
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_computer
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_dataset
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_font_download
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_graphic_eq
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_palette
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_person
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_public
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_search
import com.kayoi.pixelplayerdesktop.generated.resources.ic_rounded_sort_by_alpha
import com.kayoi.pixelplayerdesktop.presentation.components.TrackDialogInput
import com.kayoi.pixelplayerdesktop.presentation.navigation.DesktopBackHandler
import com.kayoi.pixelplayerdesktop.utils.BrowserUtils

enum class LicenseCategory {
    ALL,
    CONTRIBUTORS,
    LIBRARIES,
    FONTS,
    RAW_NOTICE
}

private enum class LicenseTab(
    val title: String,
    val icon: DrawableResource,
    val category: LicenseCategory
) {
    ALL("All", Res.drawable.ic_rounded_category, LicenseCategory.ALL),
    CONTRIBUTORS("Contributors", Res.drawable.ic_rounded_person, LicenseCategory.CONTRIBUTORS),
    LIBRARIES("Libraries", Res.drawable.ic_rounded_code, LicenseCategory.LIBRARIES),
    FONTS("Fonts", Res.drawable.ic_rounded_font_download, LicenseCategory.FONTS),
    RAW_NOTICE("Raw notice", Res.drawable.ic_automirrored_rounded_article, LicenseCategory.RAW_NOTICE)
}

data class LicenseNotice(
    val id: String,
    val name: String,
    val source: String,
    val licenseName: String,
    val section: String,
    val category: LicenseCategory
)

private val TableSeparatorRegex = Regex("^:?-{3,}:?$")

private fun parseThirdPartyNotices(markdown: String): List<LicenseNotice> {
    val notices = mutableListOf<LicenseNotice>()
    var section = ""
    var inTable = false

    for (rawLine in markdown.lineSequence()) {
        val line = rawLine.trim()
        if (!line.startsWith("|")) {
            inTable = false
            when {
                line.startsWith("##") -> section = line.trimStart('#').trim()
                line.endsWith(":") && !line.startsWith("#") && line.length <= 60 ->
                    section = line.removeSuffix(":").trim()
            }
            continue
        }

        val cells = line.trim('|').split('|').map { it.replace("`", "").trim() }
        if (cells.all { TableSeparatorRegex.matches(it) }) continue
        if (!inTable) {
            inTable = true
            continue
        }
        if (cells.size < 2 || cells.first().isBlank()) continue

        val licenseCell = cells.last()
        val licenseName = licenseCell.substringBefore(", ").trim()
        val licenseEvidence = licenseCell.substringAfter(", ", "").trim()
        val source = (cells.subList(1, cells.lastIndex) + licenseEvidence)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val category = when {
            "contributor" in section.lowercase() -> LicenseCategory.CONTRIBUTORS
            "font" in section.lowercase() -> LicenseCategory.FONTS
            else -> LicenseCategory.LIBRARIES
        }

        notices += LicenseNotice(
            id = "notice_${notices.size}",
            name = cells.first(),
            source = source,
            licenseName = licenseName,
            section = section,
            category = category
        )
    }
    return notices
}

private fun getItemIcon(item: LicenseNotice): DrawableResource {
    val name = item.name.lowercase()
    return when {
        item.category == LicenseCategory.CONTRIBUTORS -> Res.drawable.ic_rounded_person
        item.category == LicenseCategory.FONTS -> Res.drawable.ic_rounded_font_download
        name.contains("mpv") || name.contains("jaudiotagger") || name.contains("vorbis") ->
            Res.drawable.ic_rounded_graphic_eq
        name.contains("compose") || name.contains("material") || name.contains("skiko") ->
            Res.drawable.ic_rounded_palette
        name.contains("okhttp") ->
            Res.drawable.ic_rounded_public
        name.contains("sqlite") ->
            Res.drawable.ic_rounded_dataset
        name.contains("kuromoji") || name.contains("pinyin") || name.contains("icu") ->
            Res.drawable.ic_rounded_sort_by_alpha
        name.contains("jna") || name.contains("dbus") ->
            Res.drawable.ic_rounded_computer
        else ->
            Res.drawable.ic_rounded_code
    }
}

private fun getLicenseText(item: LicenseNotice, fallbackOflText: String): String {
    val license = item.licenseName.lowercase()
    val path = when {
        item.category == LicenseCategory.CONTRIBUTORS && "mit" in license ->
            "licenses/PIXELPLAYER_CONTRIBUTORS.txt"
        "lgpl-3" in license -> "licenses/texts/LGPL-3.0.txt"
        "lgpl" in license -> "licenses/texts/LGPL-2.1.txt"
        "gpl" in license -> "licenses/texts/GPL-3.0.txt"
        "apache" in license -> "licenses/texts/Apache-2.0.txt"
        "mit" in license -> "licenses/texts/MIT.txt"
        "bsd" in license -> "licenses/texts/BSD-3-Clause.txt"
        "icu" in license || "unicode" in license -> "licenses/texts/ICU.txt"
        "ofl" in license || "font" in license || item.category == LicenseCategory.FONTS ->
            "licenses/texts/OFL-1.1.txt"
        else -> null
    }
    val loaded = path?.let { readResourceText(it) }?.trim().orEmpty()
    return if (loaded.isNotBlank()) loaded else fallbackOflText.trim()
}

private fun readResourceText(path: String): String {
    return runCatching {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
        stream?.bufferedReader()?.use { it.readText() } ?: ""
    }.getOrDefault("")
}

@Composable
fun LicensesDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var noticesText by remember { mutableStateOf("") }
    var oflText by remember { mutableStateOf("") }
    var notices by remember { mutableStateOf<List<LicenseNotice>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(LicenseCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var activeDetailItem by remember { mutableStateOf<LicenseNotice?>(null) }

    LaunchedEffect(Unit) {
        val rawNotices = readResourceText("licenses/THIRD_PARTY_NOTICES.md")
        val rawOfl = readResourceText("licenses/OFL.txt")
        noticesText = rawNotices
        oflText = rawOfl
        notices = parseThirdPartyNotices(rawNotices)
    }

    val filteredItems = remember(notices, selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        notices.filter { item ->
            val matchesCategory = when (selectedCategory) {
                LicenseCategory.ALL -> true
                LicenseCategory.RAW_NOTICE -> false
                else -> item.category == selectedCategory
            }
            val matchesQuery = query.isBlank() ||
                item.name.lowercase().contains(query) ||
                item.source.lowercase().contains(query) ||
                item.licenseName.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }

    if (activeDetailItem != null) {
        LicenseDetailDialog(
            item = activeDetailItem!!,
            oflText = oflText,
            onDismiss = { activeDetailItem = null }
        )
    }

    DesktopBackHandler(enabled = true) {
        onBack()
        true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isCompact = maxWidth < 800.dp
        val horizontalPadding = if (maxWidth < 540.dp) 16.dp else 28.dp

        Column(modifier = Modifier.fillMaxSize()) {
            SettingsHeader(
                title = "Licenses & attribution",
                subtitle = "Open-source libraries, fonts and bundled data",
                horizontalPadding = horizontalPadding
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedCategory != LicenseCategory.RAW_NOTICE) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search components, sources, or licenses...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_rounded_search),
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_rounded_clear),
                                        contentDescription = "Clear search",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surfaceContainer,
                            unfocusedContainerColor = colors.surfaceContainer,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(LicenseTab.entries.toTypedArray()) { tab ->
                        val selected = selectedCategory == tab.category
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = tab.category },
                            label = { Text(tab.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(tab.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primaryContainer,
                                selectedLabelColor = colors.onPrimaryContainer,
                                selectedLeadingIconColor = colors.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (selectedCategory == LicenseCategory.RAW_NOTICE) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = horizontalPadding, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        if (isCompact) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_automirrored_rounded_article),
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "THIRD_PARTY_NOTICES.md",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Bundled open-source notices and component licenses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val selection = StringSelection(noticesText)
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_outlined_content_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Notice")
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_automirrored_rounded_article),
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = "THIRD_PARTY_NOTICES.md",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = "Bundled open-source notices and component licenses",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val selection = StringSelection(noticesText)
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_outlined_content_copy),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy Notice")
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = colors.outlineVariant.copy(alpha = 0.5f)
                        )

                        FormattedMarkdownContent(
                            markdown = noticesText,
                            isCompact = isCompact,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 6.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No components match \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(filteredItems, key = { _, item -> item.id }) { index, item ->
                            LicenseNoticeRow(
                                item = item,
                                shape = getGroupShape(index, filteredItems.size),
                                onClick = { activeDetailItem = item }
                            )
                        }
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header1(val text: String) : MarkdownBlock
    data class Header2(val text: String) : MarkdownBlock
    data class Header3(val text: String) : MarkdownBlock
    data class SectionTitle(val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val rawLine = lines[i]
        val line = rawLine.trim()

        when {
            line.isEmpty() -> {
                i++
            }
            line.startsWith("# ") -> {
                blocks += MarkdownBlock.Header1(line.removePrefix("# ").trim())
                i++
            }
            line.startsWith("## ") -> {
                blocks += MarkdownBlock.Header2(line.removePrefix("## ").trim())
                i++
            }
            line.startsWith("### ") -> {
                blocks += MarkdownBlock.Header3(line.removePrefix("### ").trim())
                i++
            }
            line == "---" -> {
                blocks += MarkdownBlock.Divider
                i++
            }
            line.startsWith("|") -> {
                val tableLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    tableLines.add(lines[i].trim())
                    i++
                }
                if (tableLines.size >= 2) {
                    val headers = tableLines[0].trim('|').split('|').map { it.trim() }
                    val dataRows = tableLines.drop(2).mapNotNull { tableRow ->
                        val cells = tableRow.trim('|').split('|').map { it.trim() }
                        if (cells.isNotEmpty() && !cells.all { TableSeparatorRegex.matches(it) }) cells else null
                    }
                    blocks += MarkdownBlock.Table(headers, dataRows)
                }
            }
            line.endsWith(":") && line.length <= 80 -> {
                blocks += MarkdownBlock.SectionTitle(line)
                i++
            }
            else -> {
                blocks += MarkdownBlock.Paragraph(line)
                i++
            }
        }
    }
    return blocks
}

@Composable
private fun FormattedMarkdownContent(
    markdown: String,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    SelectionContainer(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (block in blocks) {
                when (block) {
                    is MarkdownBlock.Header1 -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    is MarkdownBlock.Header2 -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    is MarkdownBlock.Header3 -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is MarkdownBlock.SectionTitle -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                    is MarkdownBlock.Paragraph -> {
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                    is MarkdownBlock.Table -> {
                        MarkdownTable(
                            headers = block.headers,
                            rows = block.rows,
                            isCompact = isCompact
                        )
                    }
                    is MarkdownBlock.Divider -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = colors.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isCompact) {
                rows.forEachIndexed { rowIndex, rowCells ->
                    val nameCell = rowCells.getOrNull(0)?.replace("`", "")?.trim().orEmpty()
                    val sourceCell = rowCells.getOrNull(1)?.replace("`", "")?.trim().orEmpty()
                    val licenseCell = rowCells.getOrNull(2)?.replace("`", "")?.trim().orEmpty()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nameCell,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            if (licenseCell.isNotBlank()) {
                                LicenseBadge(licenseName = licenseCell)
                            }
                        }

                        if (sourceCell.isNotBlank()) {
                            if (sourceCell.startsWith("http")) {
                                Text(
                                    text = sourceCell,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = colors.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable { BrowserUtils.openBrowser(sourceCell) }
                                )
                            } else {
                                Text(
                                    text = sourceCell,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    color = colors.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (rowIndex < rows.lastIndex) {
                        HorizontalDivider(
                            color = colors.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.forEachIndexed { colIndex, headerText ->
                        val weight = when (colIndex) {
                            0 -> 0.35f
                            1 -> 0.45f
                            else -> 0.20f
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            modifier = Modifier.weight(weight)
                        )
                    }
                }

                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.35f))

                rows.forEachIndexed { rowIndex, rowCells ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowCells.forEachIndexed { colIndex, rawCellText ->
                            val cellText = rawCellText.replace("`", "").trim()
                            val weight = when (colIndex) {
                                0 -> 0.35f
                                1 -> 0.45f
                                else -> 0.20f
                            }
                            Box(modifier = Modifier.weight(weight)) {
                                when {
                                    colIndex == 2 -> {
                                        LicenseBadge(licenseName = cellText)
                                    }
                                    colIndex == 1 && cellText.startsWith("http") -> {
                                        Text(
                                            text = cellText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            ),
                                            color = colors.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.clickable { BrowserUtils.openBrowser(cellText) }
                                        )
                                    }
                                    colIndex == 1 -> {
                                        Text(
                                            text = cellText,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            ),
                                            color = colors.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = cellText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (rowIndex < rows.lastIndex) {
                        HorizontalDivider(
                            color = colors.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseNoticeRow(
    item: LicenseNotice,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val itemIcon = getItemIcon(item)

    Surface(
        shape = shape,
        color = colors.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(itemIcon),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
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
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.source.ifBlank { item.section },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LicenseBadge(licenseName = item.licenseName)

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(Res.drawable.ic_rounded_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LicenseBadge(licenseName: String) {
    val colors = MaterialTheme.colorScheme
    val (bgColor, textColor) = when {
        licenseName.contains("Apache", ignoreCase = true) ->
            colors.primaryContainer to colors.onPrimaryContainer
        licenseName.contains("MIT", ignoreCase = true) ->
            colors.secondaryContainer to colors.onSecondaryContainer
        licenseName.contains("GPL", ignoreCase = true) || licenseName.contains("BSD", ignoreCase = true) ->
            colors.tertiaryContainer to colors.onTertiaryContainer
        licenseName.contains("OFL", ignoreCase = true) || licenseName.contains("Font", ignoreCase = true) ->
            colors.surfaceContainerHighest to colors.onSurfaceVariant
        else ->
            colors.surfaceContainerHigh to colors.onSurface
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = licenseName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .widthIn(max = 140.dp)
        )
    }
}

@Composable
private fun LicenseDetailDialog(
    item: LicenseNotice,
    oflText: String,
    onDismiss: () -> Unit
) {
    TrackDialogInput()
    val colors = MaterialTheme.colorScheme
    val itemIcon = getItemIcon(item)
    var copied by remember { mutableStateOf(false) }

    val fullLicenseText = remember(item, oflText) { getLicenseText(item, oflText) }

    val noticeText = remember(item, fullLicenseText) {
        buildString {
            appendLine(item.name)
            if (item.section.isNotBlank()) appendLine("Category: ${item.section}")
            if (item.source.isNotBlank()) appendLine("Source / Repository: ${item.source}")
            append("License: ${item.licenseName}")
            if (fullLicenseText.isNotBlank()) {
                append("\n\n")
                append(fullLicenseText)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(620.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(itemIcon),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = item.section,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LicenseBadge(licenseName = item.licenseName)

                    val url = item.source.split(" · ").firstOrNull { it.startsWith("http") }
                    if (url != null) {
                        TextButton(onClick = { BrowserUtils.openBrowser(url) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_automirrored_rounded_open_in_new),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Repository")
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceContainerHighest.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = noticeText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            color = colors.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selection = StringSelection(noticeText)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                    copied = true
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    painter = painterResource(if (copied) Res.drawable.ic_rounded_check else Res.drawable.ic_outlined_content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (copied) "Copied!" else "Copy Notice")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Close")
            }
        }
    )
}
