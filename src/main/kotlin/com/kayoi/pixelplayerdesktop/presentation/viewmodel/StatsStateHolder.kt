package com.kayoi.pixelplayerdesktop.presentation.viewmodel

import com.kayoi.pixelplayerdesktop.data.model.PlaybackStatsSummary
import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.data.model.StatsTimeRange
import com.kayoi.pixelplayerdesktop.domain.PlaybackStatsCalculator
import com.kayoi.pixelplayerdesktop.domain.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StatsStateHolder(
    private val musicRepository: MusicRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val calculator = PlaybackStatsCalculator()

    private val _selectedRange = MutableStateFlow(StatsTimeRange.WEEK)
    val selectedRange: StateFlow<StatsTimeRange> = _selectedRange.asStateFlow()

    private val _homeOverview = MutableStateFlow<PlaybackStatsSummary?>(null)
    val homeOverview: StateFlow<PlaybackStatsSummary?> = _homeOverview.asStateFlow()

    private val _currentSummary = MutableStateFlow<PlaybackStatsSummary?>(null)
    val currentSummary: StateFlow<PlaybackStatsSummary?> = _currentSummary.asStateFlow()

    private val homeOverviewRanges = listOf(
        StatsTimeRange.WEEK,
        StatsTimeRange.MONTH,
        StatsTimeRange.DAY,
        StatsTimeRange.YEAR,
        StatsTimeRange.ALL
    )

    init {
        coroutineScope.launch {
            musicRepository.getPlaybackEventsFlow().collect {
                val songs = musicRepository.getAllSongs()
                refreshStats(songs)
            }
        }
    }

    fun selectRange(range: StatsTimeRange) {
        if (_selectedRange.value == range) return
        _selectedRange.value = range
        coroutineScope.launch {
            val songs = musicRepository.getAllSongs()
            val events = musicRepository.getPlaybackEvents()
            _currentSummary.value = calculator.calculateSummary(range, events, songs)
        }
    }

    suspend fun refreshStats(songs: List<Song>? = null) {
        val targetSongs = songs ?: musicRepository.getAllSongs()
        val events = musicRepository.getPlaybackEvents()

        var foundHomeSummary: PlaybackStatsSummary? = null
        for (range in homeOverviewRanges) {
            val summary = calculator.calculateSummary(range, events, targetSongs)
            if (summary.hasListeningActivity()) {
                foundHomeSummary = summary
                break
            }
        }
        _homeOverview.value = foundHomeSummary

        _currentSummary.value = calculator.calculateSummary(_selectedRange.value, events, targetSongs)
    }
}
