package com.kasiguru.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.BuildConfig
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.ClayButtonTone
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Lime
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Release-note bullets shown before "Show all": enough to say what changed, short enough to scan. */
private const val NOTES_PREVIEW = 3

/**
 * A new version of the app is out: what it is against what the learner has, when it shipped, what
 * changed, and the download.
 *
 * Optional updates sit at the top of Home but stay quiet - a sunken Download button, so Home's lime
 * Continue is still the one thing to do - and can be put off with "Later". A forced update carries
 * the lime button, a lime border and no "Later", because it outranks the day's work.
 *
 * The release notes are the bullets written for the release (docs/releases/<version>.md, copied into
 * app_releases by the release workflow); the first three show, the rest behind "Show all".
 */
@Composable
fun AppUpdateBanner(
    release: AppReleaseDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val download: () -> Unit = {
        val targetUrl = release.apkUrl.ifBlank { "https://kasiguru.web.app/download.html" }
        val uri = Uri.parse(targetUrl)
        // Only allow http/https update links (defense against
        // javascript:/intent:/etc. URLs in release metadata).
        if (uri.scheme == "https" || uri.scheme == "http") {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } else {
            Log.w("AppUpdateBanner", "Blocked update URL with unsafe scheme: ${uri.scheme}")
        }
    }
    // Asked of the installed package, not BuildConfig: a BuildConfig string is inlined into this file
    // when it compiles, so an incremental build can show the version it had then.
    val installed = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: BuildConfig.VERSION_NAME
    }
    val notes = remember(release.releaseNotes) { releaseNoteLines(release.releaseNotes) }
    val released = remember(release.releasedAt) { releaseDate(release.releasedAt) }
    var showAll by rememberSaveable(release.versionCode) { mutableStateOf(false) }
    val forced = release.forceUpdate

    SoftCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = Shapes.panel,
        border = if (forced) Lime else BorderHairline
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LimeTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.ArrowDown),
                    contentDescription = null,
                    tint = LimeText,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (forced) "Update required" else "Update available",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Text(
                    text = "Version ${release.versionName} · you have $installed",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
                if (released != null) {
                    Text(text = "Released $released", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }

        if (forced) {
            Spacer(Modifier.height(Space.sm))
            Text(
                text = "Please install this update now. Your progress is kept when you update.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink
            )
        }

        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(Space.md))
            Text(text = "What's new", style = MaterialTheme.typography.labelLarge, color = Ink)
            Spacer(Modifier.height(Space.xxs))
            val shown = if (showAll) notes else notes.take(NOTES_PREVIEW)
            Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                shown.forEach { line -> NoteLine(line) }
            }
            if (notes.size > NOTES_PREVIEW) {
                TextButton(
                    onClick = { showAll = !showAll },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp)
                ) {
                    Text(
                        text = if (showAll) "Show less" else "Show all ${notes.size} changes",
                        style = MaterialTheme.typography.labelLarge,
                        color = LimeText
                    )
                }
            }
        }

        Spacer(Modifier.height(Space.md))
        ClayButton(
            label = "Download update",
            onClick = download,
            modifier = Modifier.fillMaxWidth(),
            tone = if (forced) ClayButtonTone.Primary else ClayButtonTone.Quiet,
            leading = {
                Icon(
                    painter = painterResource(id = Iconsax.ArrowDown),
                    contentDescription = null,
                    tint = if (forced) OnLime else Ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        if (!forced) {
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Later", color = Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** One release-note bullet: a small lime dot, then the line. */
@Composable
private fun NoteLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Lime)
        )
        Spacer(Modifier.width(Space.xs))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

/** The notes as lines, without the "-" or "•" the release file writes them with. */
internal fun releaseNoteLines(notes: String): List<String> = notes.lineSequence()
    .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
    .filter { it.isNotEmpty() }
    .toList()

/** "2 Oct 2026", or null for a release without a real timestamp. */
private fun releaseDate(releasedAt: Long): String? {
    if (releasedAt <= 0L) return null
    return Instant.ofEpochMilli(releasedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}
