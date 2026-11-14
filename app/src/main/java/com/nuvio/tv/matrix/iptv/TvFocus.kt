package com.nuvio.tv.matrix.iptv

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * TV remote (DPAD) focus support, ported from the 0.6.3-based Matrix build.
 *
 * - [tvFocusRing]: a clear violet outline + slight scale while focused.
 *   Apply it AFTER clickable/selectable so it draws on top.
 * - [rememberTvInitialFocus]: a FocusRequester that asks for focus on first
 *   composition (and retries while the target is still being composed).
 *   Attach the returned requester to the screen's first interactive element.
 */

/** Matrix violet accent, used for the TV focus ring. */
val TvFocusRingColor: Color = Color(0xFFA855F7)

@Composable
fun Modifier.tvFocusRing(
    cornerRadius: Dp = 12.dp,
    focusedScale: Float = 1.04f,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    return this
        .onFocusChanged { focused = it.isFocused }
        .graphicsLayer {
            val scale = if (focused) focusedScale else 1f
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (focused) {
                Modifier.border(3.dp, TvFocusRingColor, RoundedCornerShape(cornerRadius))
            } else {
                Modifier
            },
        )
}

@Composable
fun rememberTvInitialFocus(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(8) {
            delay(250)
            if (focusRequester.requestFocus()) return@LaunchedEffect
        }
    }
    return focusRequester
}
