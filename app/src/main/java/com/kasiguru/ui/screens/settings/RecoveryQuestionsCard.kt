package com.kasiguru.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.components.KasiGuruTextField
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*

/**
 * Recovery questions as one settings row that opens into a short form.
 *
 * Closed, it says how many of the three are answered, so the settings list stays a list. Open, each
 * question is numbered and set above its own field (the old form put the whole question in a field
 * label, where long ones were cut off), a tick marks every answered one, and Save only lights up when
 * something has changed, then says "Saved" in place. A guest has no account to save to, so the row
 * offers sign-in instead of a form that could only fail.
 */
@Composable
fun RecoveryQuestionsCard(
    signedIn: Boolean,
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel
) {
    val answers by viewModel.securityAnswers.collectAsState()
    val saved by viewModel.savedSecurityAnswers.collectAsState()
    val saving by viewModel.securitySaving.collectAsState()
    val status by viewModel.securityQuestionsStatus.collectAsState()
    val statusIsError by viewModel.securityStatusIsError.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    LaunchedEffect(signedIn) { if (signedIn) viewModel.loadSecurityQuestions() }

    val questions = viewModel.securityQuestions
    val answeredCount = saved.count { it.isNotBlank() }
    val dirty = answers.map { it.trim() } != saved.map { it.trim() }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        // ── The row: what this is and how far along it is ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.chip)
                .clickable(
                    role = Role.Button,
                    onClickLabel = if (expanded) "Close recovery questions" else "Open recovery questions"
                ) { expanded = !expanded }
                .heightIn(min = Touch.minTarget),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Surface(shape = Shapes.chip, color = Lime.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = Iconsax.Lock),
                        contentDescription = null,
                        tint = LimeText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Recovery questions",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = Ink
                )
                Text(
                    text = when {
                        !signedIn -> "Sign in to set these up"
                        answeredCount == 0 -> "Not set up yet"
                        answeredCount == questions.size -> "All ${questions.size} answered"
                        else -> "$answeredCount of ${questions.size} answered"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (signedIn && answeredCount == questions.size) GreenText else Muted
                )
            }
            Icon(
                painter = painterResource(id = if (expanded) Iconsax.ArrowUp else Iconsax.ArrowDown),
                contentDescription = null,
                tint = Faint,
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = Space.md)) {
                Text(
                    text = "Simple answers that help confirm it's you if you ever ask us for help with " +
                        "your account. They aren't a password: anyone who unlocks this phone can read them.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                Spacer(Modifier.height(Space.md))

                if (!signedIn) {
                    ClayButton(
                        label = "Sign in to set them up",
                        onClick = onSignIn,
                        tone = ClayButtonTone.Quiet,
                        modifier = Modifier.fillMaxWidth()
                    )
                    return@Column
                }

                questions.forEachIndexed { index, question ->
                    val answer = answers.getOrElse(index) { "" }
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (answer.isNotBlank()) Lime else SurfaceSunken),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (answer.isNotBlank()) OnLime else Muted
                            )
                        }
                        Spacer(Modifier.width(Space.sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink
                            )
                            Spacer(Modifier.height(Space.xs))
                            KasiGuruTextField(
                                value = answer,
                                onValueChange = { viewModel.onSecurityAnswerChanged(index, it) },
                                placeholder = { Text("Your answer") },
                                singleLine = true,
                                enabled = !saving,
                                trailingIcon = if (answer.isNotBlank()) {
                                    {
                                        Icon(
                                            painter = painterResource(id = Iconsax.TickCircle),
                                            contentDescription = "Answered",
                                            tint = GreenText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = if (index == questions.lastIndex) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                                    onDone = {
                                        focus.clearFocus()
                                        if (dirty) viewModel.saveSecurityQuestions()
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                }

                ClayButton(
                    label = when {
                        saving -> "Saving…"
                        !dirty && answeredCount > 0 -> "Saved"
                        else -> "Save answers"
                    },
                    onClick = {
                        focus.clearFocus()
                        viewModel.saveSecurityQuestions()
                    },
                    enabled = dirty && !saving,
                    tone = ClayButtonTone.Primary,
                    modifier = Modifier.fillMaxWidth()
                )

                status?.let { message ->
                    Spacer(Modifier.height(Space.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = if (statusIsError) Iconsax.InfoCircle else Iconsax.TickCircle),
                            contentDescription = null,
                            tint = if (statusIsError) RedText else GreenText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(Space.xs))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusIsError) RedText else GreenText
                        )
                    }
                }
            }
        }
    }
}
