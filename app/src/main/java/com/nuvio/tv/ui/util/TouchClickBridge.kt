package com.nuvio.tv.ui.util

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
import androidx.compose.ui.platform.LocalSemanticsOwner
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties

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
 * The library's tvClickable is internal, so the app cannot fix the
 * components themselves. Instead this bridge sits at the root of each
 * window (main activity + dialogs), detects taps, hit-tests the
 * semantics tree, and invokes the onClick action of the deepest node
 * under the finger — the same action a DPAD press would trigger.
 *
 * Scrolls and drags are unaffected: scrollable children consume the
 * gesture before detectTapGestures sees an up event, so only clean
 * taps fire. Long-press invokes the node's onLongClick action.
 * Disabled nodes are skipped. The tapped node is focused first so the
 * TV UI follows the tap (focus ring, focus-driven hero).
 */
@Composable
fun Modifier.touchClickBridge(): Modifier {
    val semanticsOwner = LocalSemanticsOwner.current
    var layoutCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return this
        .onGloballyPositioned { layoutCoordinates = it }
        .pointerInput(semanticsOwner) {
            detectTapGestures(
                onTap = { offset ->
                    val rootOffset = layoutCoordinates?.localToRoot(offset) ?: offset
                    semanticsOwner.invokeClickAt(rootOffset)
                },
                onLongPress = { offset ->
                    val rootOffset = layoutCoordinates?.localToRoot(offset) ?: offset
                    semanticsOwner.invokeLongClickAt(rootOffset)
                }
            )
        }
}

private fun SemanticsOwner.invokeClickAt(position: Offset) {
    val node = rootSemanticsNode.findClickableAt(position) ?: return
    node.config.getOrNull(SemanticsActions.RequestFocus)?.action?.invoke()
    node.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
}

private fun SemanticsOwner.invokeLongClickAt(position: Offset) {
    val node = rootSemanticsNode.findLongClickableAt(position) ?: return
    node.config.getOrNull(SemanticsActions.RequestFocus)?.action?.invoke()
    node.config.getOrNull(SemanticsActions.OnLongClick)?.action?.invoke()
}

/** Deepest enabled node at [position] with an onClick action (topmost wins). */
private fun SemanticsNode.findClickableAt(position: Offset): SemanticsNode? {
    if (!boundsInRoot.contains(position)) return null
    for (child in children.asReversed()) {
        child.findClickableAt(position)?.let { return it }
    }
    if (config.contains(SemanticsProperties.Disabled)) return null
    return if (config.contains(SemanticsActions.OnClick)) this else null
}

/** Deepest enabled node at [position] with an onLongClick action (topmost wins). */
private fun SemanticsNode.findLongClickableAt(position: Offset): SemanticsNode? {
    if (!boundsInRoot.contains(position)) return null
    for (child in children.asReversed()) {
        child.findLongClickableAt(position)?.let { return it }
    }
    if (config.contains(SemanticsProperties.Disabled)) return null
    return if (config.contains(SemanticsActions.OnLongClick)) this else null
}
