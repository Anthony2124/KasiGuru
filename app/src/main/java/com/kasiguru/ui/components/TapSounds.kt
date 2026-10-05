package com.kasiguru.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import com.kasiguru.util.audio.LocalSoundEffects
import com.kasiguru.util.audio.Sfx
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val NoTapSoundKey = SemanticsPropertyKey<Unit>("NoTapSound")

/** For a control whose tap already plays its own sound (an answer, a card flip), so it does not also click. */
fun Modifier.noTapSound(): Modifier = semantics { this[NoTapSoundKey] = Unit }

/**
 * Plays the tap sound for every tappable thing inside this window, without each button having to
 * ask for it. It watches taps without consuming them and, on release, checks the semantics tree
 * for something clickable under the finger: empty space, text and scrolling stay silent.
 *
 * Apply it once at each window's root: the activity content, and inside a dialog, sheet or menu,
 * which are separate windows with their own tree.
 */
@Composable
fun Modifier.tapSounds(): Modifier {
    val sounds = LocalSoundEffects.current ?: return this
    val root = LocalView.current as? RootForTest ?: return this
    val config = LocalViewConfiguration.current
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    return this
        .onGloballyPositioned { coordinates = it }
        .pointerInput(sounds, root) {
            awaitEachGesture {
                // Initial pass: seen before the control handles it, so navigation cannot get there first.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
                val heldTooLong = up.uptimeMillis - down.uptimeMillis > config.longPressTimeoutMillis
                val moved = (up.position - down.position).getDistance() > config.touchSlop
                if (heldTooLong || moved) return@awaitEachGesture
                val inWindow = coordinates?.localToWindow(up.position) ?: return@awaitEachGesture
                if (tapsSomething(root.semanticsOwner.rootSemanticsNode, inWindow)) sounds.play(Sfx.Tap)
            }
        }
}

/**
 * Finds the front-most clickable node under [point]. Later siblings draw over earlier ones, so the
 * last match in traversal order is the one the finger is on.
 */
private fun tapsSomething(root: SemanticsNode, point: Offset): Boolean {
    var target: SemanticsNode? = null
    fun visit(node: SemanticsNode) {
        if (!node.boundsInWindow.contains(point)) return
        val config = node.config
        val clickable = config.getOrNull(SemanticsActions.OnClick) != null &&
            config.getOrNull(SemanticsProperties.Disabled) == null &&
            // A text field's click only moves the cursor; typing should not click.
            config.getOrNull(SemanticsActions.SetText) == null
        if (clickable) target = node
        node.children.forEach(::visit)
    }
    visit(root)
    // The opt-out belongs to the control it marks, not to separate buttons nested inside it.
    return target?.config?.getOrNull(NoTapSoundKey) == null && target != null
}

/**
 * [Dialog] with tap sounds. A dialog is its own window with its own semantics tree, so the
 * activity-level [tapSounds] cannot see taps inside it.
 */
@Composable
fun TapSoundDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Box(Modifier.tapSounds(), propagateMinConstraints = true) { content() }
    }
}
