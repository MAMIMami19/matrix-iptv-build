package com.nuvio.tv.matrix.iptv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.domain.repository.WatchProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Small ViewModel for the IPTV destination: the only Hilt-backed operation
 * the IPTV screens need is clearing IPTV watch progress (the
 * "clear watch history on close" setting).
 */
@HiltViewModel
class IptvViewModel @Inject constructor(
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    /** Remove Continue Watching progress for the given IPTV items. */
    fun clearIptvProgress(keys: List<Pair<String, String>>) {
        if (keys.isEmpty()) return
        viewModelScope.launch {
            keys.forEach { (videoId, _) ->
                runCatching {
                    watchProgressRepository.removeProgress(videoId, null, null)
                }
            }
        }
    }
}
