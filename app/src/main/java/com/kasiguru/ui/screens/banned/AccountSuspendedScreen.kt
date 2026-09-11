package com.kasiguru.ui.screens.banned

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen "Account Suspended" wall shown when the admin has placed a ban on the
 * current signed-in account. The user can read the reason and sign out; they cannot
 * navigate anywhere else while this screen is active.
 *
 * Design: same clay/violet system used throughout KasiGuru, with a red/warning
 * accent to communicate the severity clearly.
 */
@Composable
fun AccountSuspendedScreen(
    reason: String,
    bannedAt: Long,
    onSignOut: () -> Unit,
    isSigningOut: Boolean = false
) {
    // Subtle entrance scale for the icon
    val iconScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, animationSpec = tween(durationMillis = 380))
    }

    val bannedDate = remember(bannedAt) {
        if (bannedAt > 0L) {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(bannedAt))
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
                    .size(100.dp)
                    .scale(iconScale.value)
                    .clip(CircleShape)
                    .background(Red.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.InfoCircle),
                    contentDescription = "Account suspended",
                    tint = Red,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(Modifier.height(Space.lg))

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

            // ── Appeal Note ───────────────────────────────────────────────
            Surface(
                shape = Shapes.tile,
                color = Violet.copy(alpha = 0.07f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs)
                ) {
                    Icon(
                        painter = painterResource(id = Iconsax.InfoCircle),
                        contentDescription = null,
                        tint = Violet,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "If you believe this is a mistake, please contact the KasiGuru moderation team for an appeal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Violet,
                        lineHeight = 20.sp
                    )
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
}
