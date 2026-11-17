package com.nuvio.tv.ui.util

import android.graphics.Rect
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView

/**
 * Matrix phone variant: restores touch taps across the whole TV UI.
 *
 * Root cause: every interactive element in NuvioTV is an
 * androidx.tv.material3 component, and its click path (tvClickable ->
 * handleDPadEnter) handles ONLY DPAD key events — there is no pointer
 * handling at all (verified in tv-material 1.1.0-rc01 sources:
 * tvClickable deliberately skips the foundation `clickable` modifier).
 * On a phone, taps are silently ignored everywhere.
 *
 * How the bridge works: it sits at the root of each window (main
 * activity + dialogs), detects taps, and forwards them through the
 * platform accessibility tree — the same path TalkBack uses to click
 * Compose buttons. A tap hit-tests the tree for the deepest clickable
 * node under the finger and performs ACTION_CLICK on it, which the
 * Compose accessibility delegate routes to the node's semantics
 * onClick — the same action a DPAD press would trigger. Long-press
 * performs ACTION_LONG_CLICK. Disabled and non-visible nodes are
 * skipped. The tapped node is accessibility-focused first so the TV
 * UI follows the tap (focus ring, focus-driven hero).
 *
 * Why accessibility instead of the semantics tree: Compose 1.11.2
 * exposes no public path to the SemanticsOwner (LocalSemanticsOwner
 * does not exist; Owner is internal), while the accessibility
 * provider is public Android API and reads the same tree.
 *
 * Scrolls and drags are unaffected: scrollable children consume the
 * gesture before detectTapGestures sees an up event, so only clean
 * taps fire. Components that already handle touch (foundation
 * `clickable`, the player's tap handlers) consume the tap first, so
 * the bridge never double-fires them.
 */
@Composable
fun Modifier.touchClickBridge(): Modifier {
    val view = LocalView.current
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return this
        .onGloballyPositioned { layoutCoordinates = it }
        .pointerInput(view) {
            detectTapGestures(
                onTap = { offset ->
                    view.performAccessibilityActionAt(
                        position = offset.toScreenPosition(view, layoutCoordinates),
                        action = AccessibilityNodeInfo.ACTION_CLICK,
                        isActionAvailable = { it.isClickable }
                    )
                },
                onLongPress = { offset ->
                    view.performAccessibilityActionAt(
                        position = offset.toScreenPosition(view, layoutCoordinates),
                        action = AccessibilityNodeInfo.ACTION_LONG_CLICK,
                        isActionAvailable = { it.isLongClickable }
                    )
                }
            )
        }
}

/** Tap offset (bridge-layout coords) -> screen coords for bounds hit-testing. */
private fun Offset.toScreenPosition(view: View, layoutCoordinates: LayoutCoordinates?): Offset {
    val rootOffset = layoutCoordinates?.localToRoot(this) ?: this
    val location = IntArray(2)
    view.getLocationOnScreen(location)
    return Offset(rootOffset.x + location[0], rootOffset.y + location[1])
}

/** Hit-test the accessibility tree and perform [action] on the deepest matching node. */
private fun View.performAccessibilityActionAt(
    position: Offset,
    action: Int,
    isActionAvailable: (AccessibilityNodeInfo) -> Boolean
): Boolean {
    val provider: AccessibilityNodeProvider = try {
        accessibilityNodeProvider
    } catch (_: Exception) {
        null
    } ?: return false
    val root = try {
        provider.createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID)
    } catch (_: Exception) {
        null
    } ?: return false
    val target = root.findDeepestAt(position, isActionAvailable)
    var performed = false
    try {
        if (target != null) {
            runCatching {
                target.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            }
            performed = runCatching { target.performAction(action) }.getOrDefault(false)
        }
    } finally {
        if (target != null) target.recycle()
        root.recycle()
    }
    return performed
}

/**
 * Deepest enabled, visible node at [position] for which [isActionAvailable]
 * holds (topmost wins). Returns a node owned by the caller (must recycle);
 * every other obtained node is recycled inside.
 */
private fun AccessibilityNodeInfo.findDeepestAt(
    position: Offset,
    isActionAvailable: (AccessibilityNodeInfo) -> Boolean
): AccessibilityNodeInfo? {
    val bounds = Rect()
    val x = position.x.toInt()
    val y = position.y.toInt()
    for (i in childCount - 1 downTo 0) {
        val child = getChild(i) ?: continue
        var result: AccessibilityNodeInfo? = null
        child.getBoundsInScreen(bounds)
        if (bounds.contains(x, y)) {
            result = child.findDeepestAt(position, isActionAvailable)
            if (result == null &&
                child.isEnabled && child.isVisibleToUser && isActionAvailable(child)
            ) {
                result = child
            }
        }
        if (result !== child) child.recycle()
        if (result != null) return result
    }
    return null
}
