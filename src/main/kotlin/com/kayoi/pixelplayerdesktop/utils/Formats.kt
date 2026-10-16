package com.kayoi.pixelplayerdesktop.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatListeningDurationLong(milliseconds: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return when {
        hours > 0 && minutes > 0 -> String.format(Locale.US, "%d h %02d m", hours, minutes)
        hours > 0 -> String.format(Locale.US, "%d h", hours)
        minutes > 0 -> String.format(Locale.US, "%d m", minutes)
        else -> String.format(Locale.US, "%d s", seconds)
    }
}

fun formatListeningDurationCompact(milliseconds: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return when {
        hours > 0 && minutes > 0 -> String.format(Locale.US, "%dh %02dm", hours, minutes)
        hours > 0 -> String.format(Locale.US, "%dh", hours)
        minutes > 0 -> String.format(Locale.US, "%dm", minutes)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}
