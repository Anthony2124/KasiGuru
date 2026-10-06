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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kasiguru.util.update.UpdateDownload

/** Release-note bullets shown before "Show all": enough to say what changed, short enough to scan. */
private const val NOTES_PREVIEW = 3

/** Where the official app is installed from; the release workflow deploys it with every release. */
private const val DOWNLOAD_PAGE = "https://kasiguru-download.vercel.app"

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
    onDismiss: () -> Unit,
    updates: AppUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val targetUrl = release.apkUrl.ifBlank { "https://kasiguru.web.app/download.html" }
    // Only allow http/https update links (defense against
    // javascript:/intent:/etc. URLs in release metadata).
    val safeUrl = Uri.parse(targetUrl).scheme.let { it == "https" || it == "http" }
    val openInBrowser: () -> Unit = {
        if (safeUrl) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
        else Log.w("AppUpdateBanner", "Blocked update URL with unsafe scheme: $targetUrl")
    }

    // Downloaded in the app rather than in a browser tab: Chrome kept every update link as a tab and
    // offered the old APKs again on the next update. See UpdateDownloader.
    val download by updates.state.collectAsState()
    val signedByAnotherKey by updates.signedByAnotherKey.collectAsState()
    LaunchedEffect(release.versionCode) { updates.watch(release) }
    var installWhenReady by remember { mutableStateOf(false) }
    var waitingForPermission by remember { mutableStateOf(false) }
    val openDownloadPage: () -> Unit = {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_PAGE)))
    }
    val install: () -> Unit = {
        if (!updates.canInstall()) {
            // Android asks once whether KasiGuru may install updates; carry on when the learner is back.
            waitingForPermission = true
            context.startActivity(updates.installPermissionIntent())
        } else {
            updates.installIntent(release)?.let(context::startActivity) ?: openInBrowser()
        }
    }
    val startDownload: () -> Unit = {
        if (!safeUrl) {
            Log.w("AppUpdateBanner", "Blocked update URL with unsafe scheme: $targetUrl")
        } else if (updates.start(release)) {
            installWhenReady = true
        } else {
            openInBrowser()
        }
    }
    LaunchedEffect(download) {
        if (download is UpdateDownload.Ready && installWhenReady) {
            installWhenReady = false
            // Signed by another key, Android would refuse it with "package conflicts with an
            // existing package"; the card explains instead (see below).
            if (!signedByAnotherKey) install()
        }
    }
    LifecycleResumeEffect(waitingForPermission) {
        if (waitingForPermission && updates.canInstall()) {
            waitingForPermission = false
            install()
        }
        onPauseOrDispose { }
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
        val progress = download as? UpdateDownload.Downloading
        if (progress != null) {
            if (progress.percent != null) {
                LinearProgressIndicator(
                    progress = { progress.percent / 100f },
                    modifier = Modifier.fillMaxWidth().clip(Shapes.pill),
                    color = Lime,
                    trackColor = BorderHairline
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(Shapes.pill),
                    color = Lime,
                    trackColor = BorderHairline
                )
            }
            Spacer(Modifier.height(Space.sm))
        }
        val blocked = download == UpdateDownload.Ready && signedByAnotherKey
        if (blocked) {
            Text(
                text = "Android can't install this update over your copy of KasiGuru: it is a test build, " +
                    "signed differently from the official app. Make sure you are signed in so your progress " +
                    "is saved, uninstall KasiGuru, then install it from the download page. Updates after " +
                    "that install here as usual.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ink
            )
            Spacer(Modifier.height(Space.sm))
        }
        ClayButton(
            label = when {
                blocked -> "Open the download page"
                download is UpdateDownload.Downloading -> progress?.percent?.let { "Downloading… $it%" } ?: "Downloading…"
                download == UpdateDownload.Ready -> "Install update"
                download == UpdateDownload.Failed -> "Download failed · Try again"
                else -> "Download update"
            },
            onClick = when {
                blocked -> openDownloadPage
                download == UpdateDownload.Ready -> install
                else -> startDownload
            },
            enabled = progress == null,
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
        if (download == UpdateDownload.Failed) {
            TextButton(onClick = openInBrowser, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Download in the browser instead", color = LimeText, style = MaterialTheme.typography.labelLarge)
            }
        }
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
