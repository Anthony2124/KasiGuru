package com.kasiguru.ui.screens.learn.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.domain.lesson.Mastery
import com.kasiguru.domain.lesson.TreeNode
import com.kasiguru.domain.lesson.TreeNodeState
import com.kasiguru.domain.lesson.TreeSection
import com.kasiguru.ui.theme.Clay
import com.kasiguru.ui.components.clay.ClayCircle
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.GoldDeep
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.TrackNeutral
import com.kasiguru.ui.theme.Violet
import com.kasiguru.ui.theme.VioletDeep
import com.kasiguru.ui.theme.VioletTint

/**
 * The learning tree, as a winding path down the Learn sheet.
 *
 * Written as a [LazyListScope] extension rather than its own scrollable, because Learn's sheet is
 * already a `LazyColumn`: a second scroll container inside it would fight the first one and defeat
 * the virtualisation that keeps 184 nodes cheap.
 *
 * Three decisions worth knowing before editing:
 *
 * 1. **Mastery is never carried by colour alone.** Each node wears a three-segment ring, and the
 *    number of *filled* segments is the tier. Colour agrees with it, but a learner who cannot
 *    separate violet from gold still reads one, two or three filled arcs, and the node's spoken
 *    description says the tier in words.
 * 2. **A locked section still shows itself.** Hiding it would hide the reason to come back; it
 *    states its own gate instead, in XP, naming the section that opens it.
 * 3. **The winding is arithmetic, not decoration.** One amplitude and one repeating offset pattern
 *    place both the node and the connector that reaches it, so the line always meets the circle.
 */
fun LazyListScope.learningPath(
    sections: List<TreeSection>,
    onOpenLesson: (unitId: String, lessonIndex: Int) -> Unit,
    onOpenMastery: (sectionId: String) -> Unit
) {
    sections.forEachIndexed { sectionIndex, section ->
        item(key = "section-${section.id}") {
            Spacer(Modifier.height(if (sectionIndex == 0) Space.lg else Space.xl))
            SectionHeader(
                section = section,
                previousTitle = sections.getOrNull(sectionIndex - 1)?.definition?.title
            )
            Spacer(Modifier.height(Space.md))
        }

        // A locked section shows its header and its gate, not its nodes. Rendering thirty
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
            item(key = "node-${nodeKey(node)}") {
                PathRow(
                    node = node,
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
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W800,
            color = Ink
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

// ── Section header ──────────────────────────────────────────────────────────────

/**
 * A section's title, the moment it names in the journey, and its gate.
 *
 * Deliberately not a card. Same-size rounded rectangles stacked down a screen is the page structure
 * DESIGN.md refuses, and a header that competes with the nodes below it would flatten the one thing
 * this screen is: a path with stops on it.
 */
@Composable
private fun SectionHeader(section: TreeSection, previousTitle: String?) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = section.definition.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W800,
            color = if (section.isUnlocked) Ink else Muted
        )
        Spacer(Modifier.height(Space.xxs))
        Text(
            text = section.definition.journeyLine,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted
        )

        Spacer(Modifier.height(Space.sm))

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
                    .background(Violet)
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

/** One stop on the path: the connector reaching it, then the node. */
@Composable
private fun PathRow(
    node: TreeNodeState,
    positionInPath: Int,
    isFirstInSection: Boolean,
    previousMastery: Mastery?,
    onOpenLesson: (String, Int) -> Unit,
    onOpenMastery: (String) -> Unit
) {
    val lean = WindOffsets[positionInPath % WindOffsets.size]

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
                modifier = Modifier.offset(x = WindAmplitude * lean),
                onClick = {
                    when (val kind = node.node) {
                        is TreeNode.Lesson -> onOpenLesson(kind.ref.unitId, kind.ref.lessonIndex)
                        is TreeNode.MasteryTest -> onOpenMastery(kind.sectionId)
                    }
                }
            )
        }
    }
}

/** The line between two stops. Dotted ahead of the learner, solid behind them. */
@Composable
private fun Connector(fromLean: Float, toLean: Float, walked: Boolean) {
    val colour = if (walked) Violet else Faint
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
            color = colour.copy(alpha = if (walked) 0.55f else 0.45f),
            start = start,
            end = end,
            strokeWidth = 3.dp.toPx(),
            pathEffect = if (walked) null else PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 6.dp.toPx())
            )
        )
    }
}

/**
 * One node: a clay disc wearing a three-segment mastery ring.
 *
 * Clay is correct here and nowhere near arbitrary — DESIGN.md reserves it for things you earn or
 * press, and a node is both. A locked node is deliberately *not* clay: it has not been earned and
 * cannot be pressed, so it drops to a flat, bordered disc, which is a difference in material rather
 * than in shade.
 */
@Composable
private fun PathNode(
    state: TreeNodeState,
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
        MasteryRing(
            mastery = state.mastery,
            isCurrent = state.isCurrent,
            modifier = Modifier.size(ringSize).align(Alignment.TopCenter)
        )

        if (state.isUnlocked) {
            ClayCircle(
                size = size,
                face = state.faceColour(),
                lipColor = state.lipColour(),
                onClick = onClick,
                modifier = Modifier.align(Alignment.TopCenter).padding(RingInset)
            ) {
                NodeGlyph(state = state, tint = state.contentColour())
            }
        } else {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(RingInset)
                    .size(size)
                    .clip(CircleShape)
                    .background(TrackNeutral)
                    .border(1.dp, Faint.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Lock),
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** The lesson's number, or the medal that marks a section's mastery test. */
@Composable
private fun NodeGlyph(state: TreeNodeState, tint: Color) {
    when (val node = state.node) {
        is TreeNode.Lesson -> Text(
            text = "${node.positionInSection}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W800,
            color = tint
        )

        is TreeNode.MasteryTest -> Icon(
            painter = painterResource(id = Iconsax.MedalStar),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Three arc segments around a node; the number filled is the mastery tier.
 *
 * This is the part that keeps the path readable without colour. One filled segment is Familiar, two
 * is Practicing, three is Mastered, and the count survives any colour-vision difference, any
 * screenshot in greyscale, and the difference between violet and gold that the fills also carry.
 */
@Composable
private fun MasteryRing(
    mastery: Mastery,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val filled = when (mastery) {
        Mastery.NONE -> 0
        Mastery.FAMILIAR -> 1
        Mastery.PRACTICING -> 2
        Mastery.MASTERED -> 3
    }
    val arcColour = if (mastery == Mastery.MASTERED) Gold else Violet
    val hereColour = Violet

    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)

        // "You are here" is a continuous ring; mastery is arc segments. Two different shapes rather
        // than two shades, so the node the learner should tap next is findable at a glance and still
        // findable without colour.
        if (isCurrent) {
            drawArc(
                color = hereColour.copy(alpha = 0.35f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // The track appears only once something has been earned. Drawn always, it put a faint halo
        // around every untouched node and read as a second edge on the disc; drawn never, a single
        // earned arc read as a stray flick rather than as one of three. Showing the remainder only
        // beside a filled segment is what makes the count legible: one solid, two faint, is "1 of 3".
        val sweep = 100f
        if (filled > 0) {
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
}

// ── State to visual ─────────────────────────────────────────────────────────────

@Composable
private fun TreeNodeState.faceColour(): Color = when (mastery) {
    Mastery.MASTERED -> Gold
    Mastery.NONE -> VioletTint
    else -> Violet
}

@Composable
private fun TreeNodeState.lipColour(): Color = when (mastery) {
    Mastery.MASTERED -> GoldDeep
    Mastery.NONE -> Violet
    else -> VioletDeep
}

/**
 * Content colour on the node face.
 *
 * Gold carries ink and never white: measured, white on gold fails and `RewardInk` on it is 9.00.
 * The untouched node's light violet tint carries ink for the same reason.
 */
@Composable
private fun TreeNodeState.contentColour(): Color = when (mastery) {
    Mastery.MASTERED -> RewardInk
    Mastery.NONE -> Ink
    else -> Color.White
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
    if (!isUnlocked) return "$what, locked"

    val tier = when (mastery) {
        Mastery.NONE -> "not started"
        Mastery.FAMILIAR -> "familiar"
        Mastery.PRACTICING -> "practising"
        Mastery.MASTERED -> "mastered"
    }
    return if (isCurrent) "$what, $tier, start here" else "$what, $tier"
}
