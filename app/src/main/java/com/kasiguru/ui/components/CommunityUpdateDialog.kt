package com.kasiguru.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kasiguru.domain.contribute.CommunityEvent
import com.kasiguru.domain.contribute.CommunityUpdates
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Gold
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.RewardInk
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Surface as SurfaceColor

/**
 * The in-app pop-up for news about the dictionary: a word the learner sent was approved (with its
 * XP) or rejected, or words were added, edited or removed. The same news reaches the phone's
 * notifications when the app is closed; see `CommunityUpdatesRepository`.
 */
@Composable
fun CommunityUpdateDialog(event: CommunityEvent, onDismiss: () -> Unit) {
    val pose = when (event) {
        is CommunityEvent.Approved -> JepjepPose.Celebrating
        is CommunityEvent.Rejected -> JepjepPose.Encouraging
        is CommunityEvent.DictionaryUpdated -> JepjepPose.Reading
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = Shapes.panel,
            color = SurfaceColor,
            border = BorderStroke(1.dp, BorderHairline),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = Space.gutter)
                .fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (event is CommunityEvent.Rejected) SurfaceSunken else LimeTint)
                        .padding(top = Space.lg, bottom = Space.md, start = Space.lg, end = Space.lg)
                ) {
                    Jepjep(pose = pose, height = 104.dp)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = CommunityUpdates.title(event),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    if (event is CommunityEvent.Approved) {
                        Spacer(Modifier.height(Space.xs))
                        Text(
                            text = "+${event.xp} XP",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = RewardInk,
                            modifier = Modifier
                                .background(Gold, Shapes.pill)
                                .padding(horizontal = Space.sm, vertical = Space.xxs)
                        )
                    }
                }
                Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                    Text(
                        text = bodyOf(event),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (event is CommunityEvent.DictionaryUpdated) Ink else Muted
                    )
                    Spacer(Modifier.height(Space.lg))
                    ClayButton(
                        label = if (event is CommunityEvent.Approved) "Great!" else "OK",
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** The body without the XP, which the badge above already shows. */
private fun bodyOf(event: CommunityEvent): String = when (event) {
    is CommunityEvent.Approved -> "\"${event.word}\" is now in the dictionary, thanks to you."
    else -> CommunityUpdates.body(event)
}
