package com.kasiguru.ui.screens.learn.tree

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.lesson.Mastery
import com.kasiguru.domain.lesson.TreeNode
import com.kasiguru.domain.lesson.TreeNodeState
import com.kasiguru.domain.lesson.TreeSection
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.BrandLime
import com.kasiguru.ui.theme.Clay
import com.kasiguru.ui.theme.Cream
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Ground
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeLip
import com.kasiguru.ui.theme.LocalReducedMotion
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Olive
import com.kasiguru.ui.theme.OliveDeep
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Scenery
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.TrackNeutral

/**
 * The learning tree, as a winding path down the Learn tab.
 *
 * Written as a [LazyListScope] extension rather than its own scrollable, because the Learn tab is
 * already a `LazyColumn`: a second scroll container inside it would fight the first one and defeat
 * the virtualisation that keeps 184 nodes cheap.
 *
 * Decisions worth knowing before editing:
 *
 * 1. **Every section opens on its place.** A section starts with a Casiguran scene - the learner is
 *    arriving somewhere - titled in Tagalog with the English beneath it. A locked section's scene is
 *    drained of colour and dimmed; a finished one carries a Complete chip.
 * 2. **State is never carried by colour alone.** A finished node shows a check, the next one carries
 *    Jepjep and a "Start" label, a locked one a lock - every lesson past the next one, until the
 *    lesson before it is finished - and mastery is a three-segment ring whose
 *    number of *filled* segments is the tier. The node's spoken description says all of it in words.
 * 3. **A locked section still shows itself.** Hiding it would hide the reason to come back; it
 *    states its own gate instead, in XP, naming the section that opens it.
 * 4. **The winding is arithmetic, not decoration.** One amplitude and one repeating offset pattern
 *    place both the node and the connector that reaches it, so the line always meets the circle.
 *
 * [learningPathCurrentItemIndex] mirrors the items emitted here; change one and change the other.
 */
fun LazyListScope.learningPath(
    sections: List<TreeSection>,
    onOpenLesson: (unitId: String, lessonIndex: Int) -> Unit,
    onOpenMastery: (sectionId: String) -> Unit
) {
    // Every open section has its own next node, and more than one section can be open at once. Jepjep
    // stands only at the first of them - the one Home's Continue also points at - because he is never
    // on screen twice.
    val guideKey = sections
        .firstNotNullOfOrNull { section -> section.nodes.firstOrNull { it.isCurrent && it.isUnlocked } }
        ?.let(::nodeKey)

    sections.forEachIndexed { sectionIndex, section ->
        item(key = "section-${section.id}") {
            Spacer(Modifier.height(if (sectionIndex == 0) Space.xs else Space.xl))
            SectionBanner(section = section)
            Spacer(Modifier.height(Space.sm))
            SectionGate(
                section = section,
                previousTitle = sections.getOrNull(sectionIndex - 1)?.definition?.title
            )
            Spacer(Modifier.height(Space.md))
        }

        // A locked section shows its banner and its gate, not its nodes. Rendering thirty
        // untouchable circles below a lock says nothing the gate line has not already said, and
        // buries the section the learner *can* work on under a screen of dead ends.
        if (!section.isUnlocked) return@forEachIndexed

        // One lazy item per node, keyed so a completed lesson does not re-key the rows after it.
        section.nodes.forEachIndexed { nodeIndex, node ->
            // The optional tail announces itself once, where it begins. Without this the deep-dive
            // lessons are indistinguishable from the required ones and the two tiers may as well not
            // exist -- the learner would read a fourteen-node stage as fourteen nodes of homework.
            if (node.isDeepDive && section.nodes.getOrNull(nodeIndex - 1)?.isDeepDive != true) {
                item(key = "deepdive-${section.id}") {
                    DeepDiveHeader(remaining = section.deepDiveNodeCount)
                }
            }
            val key = nodeKey(node)
            item(key = "node-$key") {
                PathRow(
                    node = node,
                    showGuide = key == guideKey,
                    positionInPath = nodeIndex,
                    isFirstInSection = nodeIndex == 0,
                    previousMastery = section.nodes.getOrNull(nodeIndex - 1)?.mastery,
                    onOpenLesson = onOpenLesson,
                    onOpenMastery = onOpenMastery
                )
            }
        }
    }
}

/**
 * Position of the first current node among the items [learningPath] emits, or null when there is none.
 *
 * Lets the Learn tab open on the node the learner should tap next rather than on the first section,
 * which after a few weeks is several screens of finished work above it. Walks the sections exactly as
 * [learningPath] does - one banner item per section, then, for an open section, an optional
 * deep-dive header and one item per node.
 */
fun learningPathCurrentItemIndex(sections: List<TreeSection>): Int? {
    var index = 0
    sections.forEach { section ->
        index++ // the banner
        if (!section.isUnlocked) return@forEach
        section.nodes.forEachIndexed { nodeIndex, node ->
            if (node.isDeepDive && section.nodes.getOrNull(nodeIndex - 1)?.isDeepDive != true) index++
            if (node.isCurrent) return index
            index++
        }
    }
    return null
}

/**
 * The line between what a stage asks of a learner and what it offers them.
 *
 * Stated in words rather than drawn as a subtler node, because "you may stop here" is the single
 * most useful thing this screen can tell someone looking at a stage with fourteen lessons in it.
 */
@Composable
private fun DeepDiveHeader(remaining: Int) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = Space.lg, bottom = Space.sm)
    ) {
        Text(
            text = "Going deeper",
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = if (remaining == 1) {
                "One more lesson in this stage, whenever you want it. The next stage is already open."
            } else {
                "$remaining more lessons in this stage, whenever you want them. The next stage is already open."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
    }
}

private fun nodeKey(state: TreeNodeState): String = when (val node = state.node) {
    is TreeNode.Lesson -> "${node.ref.unitId}#${node.ref.lessonIndex}"
    is TreeNode.MasteryTest -> "mastery#${node.sectionId}"
}

// ── Section banner ──────────────────────────────────────────────────────────────

private val BannerHeight = 136.dp

/** Drains a locked section's scene of colour, so "not yet" reads without relying on the dimming. */
private val Greyscale: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * The place a section happens in: its Casiguran scene, the Tagalog title, the English beneath it.
 *
 * The scenes are bright daylight paintings, so the text sits on a night scrim that deepens toward the
 * bottom edge where the title is - white on the scrim's dark end, never on raw sky.
 */
@Composable
private fun SectionBanner(section: TreeSection) {
    val scenery = Scenery.forSection(section.id)
    val locked = !section.isUnlocked
    val status = when {
        locked -> "locked"
        section.isComplete -> "complete"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BannerHeight)
            .clip(Shapes.panel)
            .background(SurfaceSunken)
            .border(1.dp, BorderHairline, Shapes.panel)
            .clearAndSetSemantics {
                heading()
                contentDescription = buildString {
                    append(section.definition.title)
                    append(", ")
                    append(section.definition.gloss)
                    if (status != null) append(", ").append(status)
                }
            }
    ) {
        Image(
            painter = painterResource(id = scenery.res),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // The scenes are portrait; a little above centre keeps each place's landmark - the
            // lighthouse, the church on Ermita Hill - inside the landscape crop.
            alignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.25f),
            alpha = if (locked) 0.5f else 1f,
            colorFilter = if (locked) Greyscale else null,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Ground.copy(alpha = 0.35f),
                        1f to Ground.copy(alpha = 0.9f)
                    )
                )
        )

        when {
            locked -> BannerChip(
                iconRes = Iconsax.Lock,
                label = "Locked",
                tint = Muted,
                modifier = Modifier.align(Alignment.TopEnd).padding(Space.sm)
            )
            section.isComplete -> BannerChip(
                iconRes = Iconsax.TickCircle,
                label = "Complete",
                tint = BrandLime,
                modifier = Modifier.align(Alignment.TopEnd).padding(Space.sm)
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = Space.md, vertical = Space.sm)
        ) {
            Text(
                text = section.definition.title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (locked) Muted else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = section.definition.gloss,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A small status chip in a banner's corner. Always an icon and a word, never a colour on its own. */
@Composable
private fun BannerChip(iconRes: Int, label: String, tint: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(Shapes.pill)
            .background(Ground.copy(alpha = 0.78f))
            .padding(horizontal = Space.sm, vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.size(Space.xxs))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

/** The journey line, and either how close the section is to opening the next or what opens it. */
@Composable
private fun SectionGate(section: TreeSection, previousTitle: String?) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = section.definition.journeyLine,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
        Spacer(Modifier.height(Space.xs))
        if (section.isUnlocked) {
            GateMeter(section = section)
        } else {
            LockedGate(section = section, previousTitle = previousTitle)
        }
    }
}

/**
 * How close this section is to opening the next one.
 *
 * The number is stated, not just drawn: a bar on its own is the "progress ring standing in for
 * content" this project refuses, and "180 / 300 XP" is the content.
 */
@Composable
private fun GateMeter(section: TreeSection) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(Shapes.pill)
                .background(TrackNeutral)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(section.gateFraction)
                    .height(6.dp)
                    .clip(Shapes.pill)
                    .background(BrandLime)
            )
        }
        Spacer(Modifier.size(Space.sm))
        Text(
            text = "${section.earnedXp} / ${section.requiredXp} XP",
            style = MaterialTheme.typography.labelMedium,
            color = Muted
        )
    }
}

/** What a learner sees at a section they have not opened: the lock, and exactly what opens it. */
@Composable
private fun LockedGate(section: TreeSection, previousTitle: String?) {
    val remaining = (section.requiredXp - section.earnedXp).coerceAtLeast(0)
    val opener = previousTitle ?: "the section before this one"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = Iconsax.Lock),
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.size(Space.xs))
        Text(
            text = if (remaining > 0) "Earn $remaining more XP in $opener to open this"
            else "Finish $opener to open this",
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )
    }
}

// ── The path itself ─────────────────────────────────────────────────────────────

/** How far a node leans from the centre line, cycling down the path. */
private val WindOffsets = listOf(0f, 0.55f, 0.85f, 0.55f, 0f, -0.55f, -0.85f, -0.55f)

/** Half the width of the wind. Keeps the widest node clear of the gutter on a 360 dp screen. */
private val WindAmplitude = 64.dp

private val NodeSize = 60.dp
private val CurrentNodeSize = 72.dp
private val ConnectorHeight = 26.dp
private val RingInset = 10.dp

/** Jepjep's height beside the current node, and how far his centre sits from the node's edge. */
private val GuideHeight = 80.dp
private val GuideGap = 40.dp

/** What a node looks like. Worked out once from its state so the face, glyph and words agree. */
private enum class NodeLook {
    /** Not reachable yet: a flat dark disc with a lock. */
    Locked,

    /** The one node to tap next: lime, with the halo and Jepjep. */
    Current,

    /** Finished: olive with a cream check, and the mastery ring around it. */
    Done,

    /**
     * Open but not the suggested next step: the next deep-dive lesson. Core lessons after the current
     * one are [Locked] until the one before them is finished (LearningTree.openLessons).
     */
    Open,

    /** The section's mastery test, open. Gold, because it is the section's reward. */
    Test,

    /** The mastery test, passed. */
    TestPassed
}

private fun TreeNodeState.look(): NodeLook {
    val isTest = node is TreeNode.MasteryTest
    return when {
        !isUnlocked -> NodeLook.Locked
        isTest && mastery >= Mastery.FAMILIAR -> NodeLook.TestPassed
        isTest -> NodeLook.Test
        isCurrent -> NodeLook.Current
        mastery >= Mastery.FAMILIAR -> NodeLook.Done
        else -> NodeLook.Open
    }
}

/** One stop on the path: the connector reaching it, then the node, then its label if it has one. */
@Composable
private fun PathRow(
    node: TreeNodeState,
    showGuide: Boolean,
    positionInPath: Int,
    isFirstInSection: Boolean,
    previousMastery: Mastery?,
    onOpenLesson: (String, Int) -> Unit,
    onOpenMastery: (String) -> Unit
) {
    val lean = WindOffsets[positionInPath % WindOffsets.size]
    val look = node.look()
    val nodeOffset = WindAmplitude * lean

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!isFirstInSection) {
            val previousLean = WindOffsets[(positionInPath - 1) % WindOffsets.size]
            Connector(
                fromLean = previousLean,
                toLean = lean,
                // The line is solid behind ground already covered and dotted ahead of it, so the
                // path reads as walked-and-remaining without a second colour doing the work.
                walked = (previousMastery ?: Mastery.NONE) >= Mastery.FAMILIAR
            )
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PathNode(
                state = node,
                look = look,
                modifier = Modifier.offset(x = nodeOffset),
                onClick = {
                    when (val kind = node.node) {
                        is TreeNode.Lesson -> onOpenLesson(kind.ref.unitId, kind.ref.lessonIndex)
                        is TreeNode.MasteryTest -> onOpenMastery(kind.sectionId)
                    }
                }
            )

            // Jepjep walks the path with the learner: he stands beside the node that is theirs to
            // take next, on the side the path is leaning away from so he never covers the line.
            if (showGuide) {
                val ringSize = CurrentNodeSize + RingInset * 2
                val side = if (lean >= 0f) -1f else 1f
                Jepjep(
                    pose = JepjepPose.WithBackpack,
                    height = GuideHeight,
                    modifier = Modifier.offset(x = nodeOffset + (ringSize / 2 + GuideGap) * side)
                )
            }
        }

        NodeCaption(node = node, look = look, offset = nodeOffset)
    }
}

/**
 * One line under the nodes that need one: what the current node starts, and what a locked mastery
 * test is waiting for. Every other node says enough with its face.
 */
@Composable
private fun NodeCaption(node: TreeNodeState, look: NodeLook, offset: Dp) {
    val isTest = node.node is TreeNode.MasteryTest
    val text = when {
        look == NodeLook.Current -> "Start"
        look == NodeLook.Test && node.isCurrent -> "Take the test"
        look == NodeLook.Locked && isTest -> "Finish the lessons above to unlock"
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = if (look == NodeLook.Locked) Faint else Ink,
        textAlign = TextAlign.Center,
        // The node's own description already says this; reading it twice is noise.
        modifier = Modifier
            .offset(x = offset)
            .padding(top = Space.xxs)
            .clearAndSetSemantics { }
    )
}

/** The line between two stops. Dotted ahead of the learner, solid behind them. */
@Composable
private fun Connector(fromLean: Float, toLean: Float, walked: Boolean) {
    val colour = if (walked) BrandLime else Faint
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(ConnectorHeight)
            .clearAndSetSemantics { }
    ) {
        val amplitude = WindAmplitude.toPx()
        val start = Offset(size.width / 2f + fromLean * amplitude, 0f)
        val end = Offset(size.width / 2f + toLean * amplitude, size.height)
        drawLine(
            color = colour.copy(alpha = if (walked) 0.7f else 0.45f),
            start = start,
            end = end,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (walked) null else PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 6.dp.toPx())
            )
        )
    }
}

/**
 * One node: a clay disc inside its mastery ring.
 *
 * Clay is correct here - it is reserved for things you earn or press, and a node is both. A locked
 * node is deliberately *not* clay: it has not been earned and cannot be pressed, so it drops to a
 * flat, bordered disc, which is a difference in material rather than in shade.
 */
@Composable
private fun PathNode(
    state: TreeNodeState,
    look: NodeLook,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val size = if (state.isCurrent) CurrentNodeSize else NodeSize
    val ringSize = size + RingInset * 2

    Box(
        modifier = modifier
            .size(width = ringSize, height = ringSize + Clay.lip)
            .clearAndSetSemantics { contentDescription = state.spokenDescription() }
    ) {
        if (state.isCurrent && state.isUnlocked) {
            Halo(modifier = Modifier.size(ringSize).align(Alignment.TopCenter))
        }

        MasteryRing(
            mastery = state.mastery,
            modifier = Modifier.size(ringSize).align(Alignment.TopCenter)
        )

        if (look == NodeLook.Locked) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(RingInset)
                    .size(size)
                    .clip(CircleShape)
                    .background(Surface)
                    .border(1.dp, BorderHairline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Lock),
                    contentDescription = null,
                    tint = Faint,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            ClayCircle(
                size = size,
                face = look.face(),
                lipColor = look.lip(),
                onClick = onClick,
                modifier = Modifier.align(Alignment.TopCenter).padding(RingInset)
            ) {
                Box(Modifier.align(Alignment.Center)) {
                    NodeGlyph(state = state, look = look)
                }
            }
        }
    }
}

/**
 * The lime light behind the node to tap next. It breathes slowly so the eye finds it; with reduced
 * motion it simply stays lit.
 */
@Composable
private fun Halo(modifier: Modifier = Modifier) {
    val alpha = if (LocalReducedMotion.current) {
        0.35f
    } else {
        rememberInfiniteTransition(label = "NodeHalo").animateFloat(
            initialValue = 0.22f,
            targetValue = 0.42f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "NodeHaloAlpha"
        ).value
    }
    Canvas(modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to Lime.copy(alpha = alpha),
                0.7f to Lime.copy(alpha = alpha * 0.5f),
                1f to Color.Transparent,
                center = center,
                radius = size.minDimension / 2f
            ),
            radius = size.minDimension / 2f
        )
    }
}

/** The lesson's number, a check once it is done, or the medal that marks a section's mastery test. */
@Composable
private fun NodeGlyph(state: TreeNodeState, look: NodeLook) {
    val tint = look.content()
    when {
        state.node is TreeNode.MasteryTest -> Icon(
            painter = painterResource(id = Iconsax.MedalStar),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )

        look == NodeLook.Done -> Icon(
            painter = painterResource(id = Iconsax.TickCircle),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )

        else -> Text(
            text = "${(state.node as TreeNode.Lesson).positionInSection}",
            style = MaterialTheme.typography.titleLarge,
            color = tint
        )
    }
}

/**
 * Three arc segments around a node; the number filled is the mastery tier.
 *
 * This is the part that keeps the path readable without colour. One filled segment is Familiar, two
 * is Practicing, three is Mastered, and the count survives any colour-vision difference and any
 * screenshot in greyscale.
 */
@Composable
private fun MasteryRing(
    mastery: Mastery,
    modifier: Modifier = Modifier
) {
    val filled = when (mastery) {
        Mastery.NONE -> 0
        Mastery.FAMILIAR -> 1
        Mastery.PRACTICING -> 2
        Mastery.MASTERED -> 3
    }
    // The track appears only once something has been earned. Drawn always, it put a faint halo
    // around every untouched node and read as a second edge on the disc; drawn never, a single earned
    // arc read as a stray flick rather than as one of three.
    if (filled == 0) return
    val arcColour = if (mastery == Mastery.MASTERED) Gold else BrandLime

    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val sweep = 100f
        repeat(3) { index ->
            drawArc(
                color = if (index < filled) arcColour else arcColour.copy(alpha = 0.22f),
                startAngle = -90f + index * 120f + (120f - sweep) / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

// ── Look to colour ──────────────────────────────────────────────────────────────

private fun NodeLook.face(): Color = when (this) {
    NodeLook.Current -> Lime
    NodeLook.Done -> Olive
    NodeLook.Open -> SurfaceSunken
    NodeLook.Test, NodeLook.TestPassed -> Gold
    NodeLook.Locked -> Surface
}

private fun NodeLook.lip(): Color = when (this) {
    NodeLook.Current -> LimeLip
    NodeLook.Done -> OliveDeep
    NodeLook.Open -> BorderHairline
    NodeLook.Test, NodeLook.TestPassed -> GoldDeep
    NodeLook.Locked -> Surface
}

/**
 * Content colour on the node face. Lime and gold are bright, so they carry dark ink (7.7 and 11.7);
 * white fails on both. Olive carries Jepjep's cream, and the dark open face carries white.
 */
private fun NodeLook.content(): Color = when (this) {
    NodeLook.Current -> OnLime
    NodeLook.Done -> Cream
    NodeLook.Open -> Ink
    NodeLook.Test, NodeLook.TestPassed -> RewardInk
    NodeLook.Locked -> Faint
}

/**
 * What a screen reader says at this node.
 *
 * The tier is spoken as a word. Everything the ring and the fill say visually has to be available to
 * someone who is hearing the screen rather than looking at it, and "Lesson 4" alone would tell them
 * nothing about whether they have already learned it.
 */
private fun TreeNodeState.spokenDescription(): String {
    val what = when (val node = node) {
        is TreeNode.Lesson -> "Lesson ${node.positionInSection}"
        is TreeNode.MasteryTest -> "Section mastery test"
    }
    if (!isUnlocked) {
        return if (node is TreeNode.MasteryTest) "$what, locked. Finish the lessons above to unlock"
        else "$what, locked"
    }

    val tier = when (mastery) {
        Mastery.NONE -> "not started"
        Mastery.FAMILIAR -> "familiar"
        Mastery.PRACTICING -> "practising"
        Mastery.MASTERED -> "mastered"
    }
    return if (isCurrent) "$what, $tier, start here" else "$what, $tier"
}
