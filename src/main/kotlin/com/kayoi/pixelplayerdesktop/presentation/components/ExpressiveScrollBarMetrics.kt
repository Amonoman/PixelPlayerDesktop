package com.kayoi.pixelplayerdesktop.presentation.components

/**
 * Calculates the target scroll item index from touch/drag progress.
 */
internal fun resolveDragTargetIndex(
    progress: Float,
    maxScrollIndex: Int,
    totalItemsCount: Int
): Int {
    if (totalItemsCount <= 1) return 0

    val clampedProgress = progress.coerceIn(0f, 1f)
    val lastIndex = totalItemsCount - 1
    if (clampedProgress >= 1f) return lastIndex

    return (clampedProgress * maxScrollIndex.coerceAtLeast(1))
        .toInt()
        .coerceIn(0, lastIndex)
}

/**
 * Extracts leading uppercase alphanumeric character or '#' for numeric items.
 */
internal fun extractFastScrollGlyph(value: String?): String? {
    val leadingChar = value
        .orEmpty()
        .trim()
        .firstOrNull { it.isLetterOrDigit() }
        ?: return null

    return if (leadingChar.isDigit()) {
        "#"
    } else {
        leadingChar.uppercaseChar().toString()
    }
}
