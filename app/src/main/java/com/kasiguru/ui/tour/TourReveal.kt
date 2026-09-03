package com.kasiguru.ui.tour

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Bringing an off-screen anchor into view before the spotlight tries to cut a hole around it.
 *
 * Several targets sit below the fold - Settings' replay row, Profile's Explore rows, a dictionary
 * category card. Without this the tour dims the screen and cuts nothing, which reads as broken.
 *
 * `BringIntoViewRequester` looks like the obvious answer and is the wrong one. It has to be attached
 * to the element itself, and an item scrolled far out of a `LazyColumn` **is not composed at all** -
 * so its requester does not exist and cannot be called. It can only rescue something composed but
 * clipped, which is the smaller half of the problem. Scroll state is the thing that can always reach
 * the target, so the reveal is driven from there instead.
 *
 * Each helper is one line in a screen and no-ops everywhere else.
 */

/** How much of the screen to leave above a revealed anchor, so the caption has somewhere to sit. */
private const val REVEAL_HEADROOM_FRACTION = 0.28f

/**
 * For a screen whose content is a `Modifier.verticalScroll` column.
 *
 * Needs no index map: a scrolling column composes all of its children, so an off-screen anchor still
 * reports an unclipped position and the distance is simple arithmetic.
 */
@Composable
fun TourRevealInScroll(state: ScrollState) {
    val anchors = LocalTourAnchors.current
    val target = anchors.pendingReveal
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    LaunchedEffect(target, anchors.active) {
        if (!anchors.active || target == null) return@LaunchedEffect
        val rect = anchors.unclippedBoundsOf(target) ?: return@LaunchedEffect

        val headroom = screenHeightPx * REVEAL_HEADROOM_FRACTION
        val visibleTop = 0f
        val visibleBottom = screenHeightPx

        // Already comfortably on screen: leave the scroll alone rather than nudging it for no reason.
        if (rect.top >= visibleTop + headroom && rect.bottom <= visibleBottom - headroom) {
            return@LaunchedEffect
        }

        val delta = (rect.top - headroom).toInt()
        val destination = (state.value + delta).coerceIn(0, state.maxValue)
        if (destination != state.value) state.animateScrollTo(destination)
    }
}

/**
 * For a screen built on `LazyColumn`.
 *
 * Takes a lookup from anchor to item index because a lazy list disposes what is off screen: the
 * anchor cannot report where it is, so the screen has to say which item holds it.
 */
@Composable
fun TourRevealInLazyList(state: LazyListState, indexOf: (TourAnchor) -> Int?) {
    val anchors = LocalTourAnchors.current
    val target = anchors.pendingReveal

    LaunchedEffect(target, anchors.active) {
        if (!anchors.active || target == null) return@LaunchedEffect
        val index = indexOf(target) ?: return@LaunchedEffect
        if (state.layoutInfo.visibleItemsInfo.any { it.index == index }) return@LaunchedEffect
        state.animateScrollToItem(index)
    }
}

/** As [TourRevealInLazyList], for a `LazyVerticalGrid`. */
@Composable
fun TourRevealInLazyGrid(state: LazyGridState, indexOf: (TourAnchor) -> Int?) {
    val anchors = LocalTourAnchors.current
    val target = anchors.pendingReveal

    LaunchedEffect(target, anchors.active) {
        if (!anchors.active || target == null) return@LaunchedEffect
        val index = indexOf(target) ?: return@LaunchedEffect
        if (state.layoutInfo.visibleItemsInfo.any { it.index == index }) return@LaunchedEffect
        state.animateScrollToItem(index)
    }
}
