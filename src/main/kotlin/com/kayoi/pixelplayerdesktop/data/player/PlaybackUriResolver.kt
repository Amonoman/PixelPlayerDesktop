package com.kayoi.pixelplayerdesktop.data.player

import com.kayoi.pixelplayerdesktop.data.model.Song
import com.kayoi.pixelplayerdesktop.domain.repository.JellyfinRepository
import com.kayoi.pixelplayerdesktop.domain.repository.NavidromeRepository

class PlaybackUriResolver(
    private val navidromeRepository: NavidromeRepository,
    private val jellyfinRepository: JellyfinRepository
) {

    fun resolve(song: Song): String {
        val navId = song.navidromeId ?: song.path.takeIf { it.startsWith("navidrome://") }?.removePrefix("navidrome://")
        if (navId != null) {
            val streamUrl = navidromeRepository.getStreamUrl(navId)
            if (streamUrl != null) return streamUrl
        }

        val jfId = song.jellyfinId ?: song.path.takeIf { it.startsWith("jellyfin://") }?.removePrefix("jellyfin://")
        if (jfId != null) {
            val streamUrl = jellyfinRepository.getStreamUrl(jfId)
            if (streamUrl != null) return streamUrl
        }

        return song.path
    }
}
