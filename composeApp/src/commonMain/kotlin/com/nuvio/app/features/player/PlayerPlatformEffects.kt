package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize

interface PlayerGestureController {
    fun currentBrightness(): Float?
    fun setBrightness(level: Float): Float?
    fun currentVolume(): PlayerAudioLevel?
    fun setVolume(level: Float): PlayerAudioLevel?
}

data class PlayerAudioLevel(
    val fraction: Float,
    val isMuted: Boolean,
)

@Composable
expect fun LockPlayerToLandscape()

/**
 * Phone player orientation mode. Sensor follows the device (portrait =
 * small player, landscape = big fullscreen); Portrait / Landscape are the
 * manual overrides set by the player rotate button. Only the Android phone
 * player applies this; TV, tablets and iOS are unaffected.
 */
enum class PlayerOrientationMode {
    Sensor,
    Portrait,
    Landscape,
}

object PlayerOrientationState {
    // Public setter on purpose: the Android controller assigns it directly
    // (a separate setMode() function would clash with the generated setter).
    var mode by mutableStateOf(PlayerOrientationMode.Sensor)

    fun reset() {
        mode = PlayerOrientationMode.Sensor
    }
}

/** Null on platforms/form factors without a manual rotate control. */
interface PlayerOrientationController {
    /** Flips small (portrait) <-> big (landscape) from the current orientation. */
    fun toggle()
}

@Composable
expect fun rememberPlayerOrientationController(): PlayerOrientationController?

@Composable
expect fun FullscreenPlayerDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
)

@Composable
expect fun HidePlayerSystemBars()

@Composable
expect fun EnterImmersivePlayerMode(keepScreenAwake: Boolean)

@Composable
expect fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    videoSize: IntSize,
)

@Composable
expect fun rememberIsInPictureInPicture(): Boolean

@Composable
fun rememberPlayerGestureController(restoreBrightness: Boolean): PlayerGestureController? {
    val controller = rememberPlatformPlayerGestureController() ?: return null
    DisposableEffect(controller, restoreBrightness) {
        if (restoreBrightness) {
            PlayerSettingsStorage.loadPlaybackBrightness()
                ?.takeIf { it in 0f..1f }
                ?.let(controller::setBrightness)
        }
        onDispose {}
    }
    return remember(controller) {
        object : PlayerGestureController by controller {
            override fun setBrightness(level: Float): Float? {
                if (!level.isFinite()) return null
                return controller.setBrightness(level.coerceIn(0f, 1f))
                    ?.also(PlayerSettingsStorage::savePlaybackBrightness)
            }
        }
    }
}

@Composable
internal expect fun rememberPlatformPlayerGestureController(): PlayerGestureController?
