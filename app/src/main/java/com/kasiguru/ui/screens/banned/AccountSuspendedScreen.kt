package com.kasiguru.ui.screens.banned

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen "Account Suspended" wall shown when the admin has placed a ban on the
 * current signed-in account.
 *
 * Provides:
 * - Clear explanation of the suspension reason and date.
 * - Interactive appeal submission and real-time review status tracking.
 * - Safe guest sign-out option with local data wipe.
 */
@Composable
fun AccountSuspendedScreen(
    reason: String,
    bannedAt: Long,
    appealText: String? = null,
    appealSubmittedAt: Long? = null,
    appealStatus: String? = null,
    appealReviewNotes: String? = null,
    appealReviewedAt: Long? = null,
    isSubmittingAppeal: Boolean = false,
    appealError: String? = null,
    onSubmitAppeal: (String) -> Unit = {},
    onClearAppealError: () -> Unit = {},
    onSignOut: () -> Unit,
    isSigningOut: Boolean = false
) {
    // Subtle entrance scale for the icon
    val iconScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, animationSpec = tween(durationMillis = 380))
    }

    var showAppealDialog by remember { mutableStateOf(false) }

    // Optimistic local state: holds the submitted text immediately after the user taps
    // "Submit Appeal", so the UI transitions to the pending card right away without
    // waiting for the Firestore real-time listener to echo the write back. Cleared as
    // soon as Firestore confirms (i.e. appealText becomes non-null).
    var localPendingAppealText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(appealText) {
        if (appealText != null) localPendingAppealText = null
    }
    val effectiveAppealText   = appealText   ?: localPendingAppealText
    val effectiveAppealStatus = when {
        appealStatus != null         -> appealStatus
        localPendingAppealText != null -> "pending"
        else                         -> null
    }

    val bannedDate = remember(bannedAt) {
        if (bannedAt > 0L) {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(bannedAt))
        } else null
    }

    val appealSubmittedDate = remember(appealSubmittedAt) {
        if (appealSubmittedAt != null && appealSubmittedAt > 0L) {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(appealSubmittedAt))
        } else null
    }

    val appealReviewedDate = remember(appealReviewedAt) {
        if (appealReviewedAt != null && appealReviewedAt > 0L) {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(appealReviewedAt))
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(vertical = Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Warning Icon ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .scale(iconScale.value)
                    .clip(CircleShape)
                    .background(Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.InfoCircle),
                    contentDescription = "Account suspended",
                    tint = Red,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(Space.md))

            // ── Headline ──────────────────────────────────────────────────
            Text(
                text = "Account Suspended",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Ink,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Space.xs))

            Text(
                text = "Your KasiGuru account has been suspended by an administrator.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(Space.lg))

            // ── Reason Card ───────────────────────────────────────────────
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(Space.sm)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Red.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.InfoCircle),
                            contentDescription = null,
                            tint = Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Reason for suspension",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                            lineHeight = 22.sp
                        )
                        if (bannedDate != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Suspended on $bannedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.md))

            // ── Appeal Section ────────────────────────────────────────────
            when (effectiveAppealStatus) {
                "pending" -> {
                    // Appeal is pending moderation review
                    Surface(
                        shape = Shapes.tile,
                        color = AmberTint.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Space.md),
                            verticalArrangement = Arrangement.spacedBy(Space.xs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.xs)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Calendar),
                                    contentDescription = null,
                                    tint = Amber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Appeal Under Review",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber
                                )
                            }
                            if (appealSubmittedDate != null) {
                                Text(
                                    text = "Submitted on $appealSubmittedDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                            if (!effectiveAppealText.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.65f))
                                        .padding(Space.sm)
                                ) {
                                    Text(
                                        text = "“$effectiveAppealText”",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Ink,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Text(
                                text = "Our moderation team is reviewing your appeal. Your account will automatically reactivate if approved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink.copy(alpha = 0.75f),
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        onClearAppealError()
                                        showAppealDialog = true
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Edit),
                                        contentDescription = null,
                                        tint = Violet,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Update Appeal",
                                        color = Violet,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                "rejected" -> {
                    // Appeal was reviewed and declined
                    Surface(
                        shape = Shapes.tile,
                        color = Red.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Red.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Space.md),
                            verticalArrangement = Arrangement.spacedBy(Space.xs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.xs)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.CloseCircle),
                                    contentDescription = null,
                                    tint = Red,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Appeal Declined",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Red
                                )
                            }
                            if (appealReviewedDate != null) {
                                Text(
                                    text = "Reviewed on $appealReviewedDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                            if (!appealReviewNotes.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.75f))
                                        .padding(Space.sm)
                                ) {
                                    Column {
                                        Text(
                                            text = "Moderator feedback:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Muted
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = appealReviewNotes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Ink,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "If you have additional clarification or new context, you may submit a revised appeal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        onClearAppealError()
                                        showAppealDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Violet,
                                        contentColor = Color.White
                                    ),
                                    shape = Shapes.pill,
                                    contentPadding = PaddingValues(horizontal = Space.md, vertical = 6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Edit),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Submit New Appeal",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {
                    // No appeal has been submitted yet
                    Surface(
                        shape = Shapes.tile,
                        color = Violet.copy(alpha = 0.07f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Violet.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(Space.md),
                            verticalArrangement = Arrangement.spacedBy(Space.xs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Space.xs)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = Violet,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Believe this is a mistake?",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Violet
                                )
                            }
                            Text(
                                text = "If you think your account was suspended unfairly or you'd like to provide context to our moderators, you can submit an appeal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink.copy(alpha = 0.8f),
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    onClearAppealError()
                                    showAppealDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Violet,
                                    contentColor = Color.White
                                ),
                                shape = Shapes.pill,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.Edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Appeal Suspension",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Space.xl))

            // ── Sign Out Button ───────────────────────────────────────────
            Button(
                onClick = onSignOut,
                enabled = !isSigningOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Red,
                    contentColor = Color.White,
                    disabledContainerColor = Red.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp),
                shape = Shapes.pill
            ) {
                Text(
                    text = if (isSigningOut) "Signing out…" else "Sign Out",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(Space.md))

            Text(
                text = "Signing out returns you to the app as a guest.\nYour suspended account's progress is preserved.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }

    // ── Appeal Input Dialog ───────────────────────────────────────────
    if (showAppealDialog) {
        var appealInput by remember { mutableStateOf(effectiveAppealText ?: "") }
        var hasAttemptedSubmit by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isSubmittingAppeal) showAppealDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Ground,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Space.sm)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Space.lg),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.xs)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Violet.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = Iconsax.Edit),
                                contentDescription = null,
                                tint = Violet,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (effectiveAppealStatus == "pending") "Update Your Appeal" else "Appeal Suspension",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Ink
                        )
                    }

                    Text(
                        text = "Please describe why you believe the suspension should be lifted. Be polite, concise, and provide any relevant context.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = appealInput,
                        onValueChange = {
                            if (it.length <= 1000) {
                                appealInput = it
                                onClearAppealError()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 130.dp, max = 220.dp),
                        placeholder = {
                            Text(
                                text = "Explain your situation here (at least 10 characters)...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                        isError = (hasAttemptedSubmit && appealInput.trim().length < 10) || appealError != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet,
                            unfocusedBorderColor = BorderHairline,
                            errorBorderColor = Red
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (appealError != null) {
                            Text(
                                text = appealError,
                                style = MaterialTheme.typography.bodySmall,
                                color = Red,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else if (hasAttemptedSubmit && appealInput.trim().length < 10) {
                            Text(
                                text = "At least 10 characters required",
                                style = MaterialTheme.typography.bodySmall,
                                color = Red
                            )
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        Text(
                            text = "${appealInput.length}/1000",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (appealInput.length >= 1000) Red else Muted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.sm, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAppealDialog = false },
                            enabled = !isSubmittingAppeal
                        ) {
                            Text(
                                text = "Cancel",
                                color = Muted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                hasAttemptedSubmit = true
                                if (appealInput.trim().length >= 10) {
                                    // Set optimistic state immediately so the pending card
                                    // shows the submitted text before Firestore echoes it back.
                                    localPendingAppealText = appealInput.trim()
                                    onSubmitAppeal(appealInput.trim())
                                    showAppealDialog = false
                                }
                            },
                            enabled = !isSubmittingAppeal && appealInput.trim().length >= 10,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Violet,
                                contentColor = Color.White,
                                disabledContainerColor = Violet.copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.6f)
                            ),
                            shape = Shapes.pill
                        ) {
                            if (isSubmittingAppeal) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Submitting…", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    painter = painterResource(id = Iconsax.TickCircle),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (effectiveAppealStatus == "pending") "Update Appeal" else "Submit Appeal",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
