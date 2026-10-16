package com.kayoi.pixelplayerdesktop.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class StatsTimeRange(val displayName: String) {
    DAY("Today"),
    WEEK("Week to Date"),
    MONTH("Month to Date"),
    YEAR("Year to Date"),
    ALL("All Time");

    fun resolveBounds(
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<Long?, Long> {
        val safeNow = nowMillis.coerceAtLeast(0L)
        val zonedNow = Instant.ofEpochMilli(safeNow).atZone(zoneId)

        return when (this) {
            DAY -> {
                val start = zonedNow.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to safeNow
            }
            WEEK -> {
                val startOfWeek = zonedNow.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val start = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to safeNow
            }
            MONTH -> {
                val startOfMonth = zonedNow.toLocalDate().withDayOfMonth(1)
                val start = startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to safeNow
            }
            YEAR -> {
                val startOfYear = zonedNow.toLocalDate().withDayOfYear(1)
                val start = startOfYear.atStartOfDay(zoneId).toInstant().toEpochMilli()
                start to safeNow
            }
            ALL -> {
                null to safeNow
            }
        }
    }
}
