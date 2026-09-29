package com.kasiguru.ui.components.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Space

/**
 * The one empty state: a Jepjep pose, one line, one short explanation and at most one action.
 *
 * Pick the pose by meaning — [JepjepPose.Sleeping] for "nothing here yet, all quiet",
 * [JepjepPose.Curious] for "nothing matched", [JepjepPose.Reading] for stories, [JepjepPose.Worried]
 * for offline or failed loads. The action is a Quiet button: an empty screen is not the place for the
 * one lime button unless the action is the whole point of the screen, so pass [primaryAction] = true
 * only then.
 */
@Composable
fun EmptyState(
    pose: JepjepPose,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    primaryAction: Boolean = false,
    poseHeight: Dp = 132.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Jepjep(pose = pose, height = poseHeight, breathe = true)
        Spacer(Modifier.height(Space.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Space.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.lg))
            ClayButton(
                label = actionLabel,
                onClick = onAction,
                tone = if (primaryAction) ClayButtonTone.Primary else ClayButtonTone.Quiet
            )
        }
    }
}

/** Something failed to load. Jepjep looks worried; the action retries. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    retryLabel: String = "Try again",
    onRetry: (() -> Unit)? = null
) {
    EmptyState(
        pose = JepjepPose.Worried,
        title = title,
        message = message,
        modifier = modifier,
        actionLabel = if (onRetry != null) retryLabel else null,
        onAction = onRetry
    )
}

/** A centred spinner in lime, labelled for TalkBack. Prefer a skeleton where the layout is known. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String = "Loading") {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Space.xxl)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Lime)
    }
}
