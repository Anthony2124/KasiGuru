package com.kasiguru.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kasiguru.BuildConfig
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.ui.components.brand.Jepjep
import com.kasiguru.ui.components.brand.JepjepPose
import com.kasiguru.ui.components.clay.ClayButton
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
import com.kasiguru.ui.theme.Surface as SurfaceColor
import com.kasiguru.util.update.UpdateDownload
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Where the official app is installed from; the release workflow deploys it with every release. */
private const val DOWNLOAD_PAGE = "https://kasiguru-download.vercel.app"

/**
 * A new version is out: a pop-up when it is first found, over whatever screen is open.
 *
 * An optional update shows once per version and can be put off with "Later"; Home then keeps a slim
 * [AppUpdateBanner] until it is installed. A required update cannot be dismissed — no "Later", no
 * Back, no tap outside — so it blocks every screen until it is installed, rather than sitting at the
 * top of Home where the rest of the app stayed usable on a build that was meant to be retired.
 */
@Composable
fun AppUpdateDialog(
    release: AppReleaseDto,
    onLater: () -> Unit,
    updates: AppUpdateViewModel = hiltViewModel()
) {
    val actions = rememberUpdateActions(release, updates)
    val download by updates.state.collectAsState()
    val signedByAnotherKey by updates.signedByAnotherKey.collectAsState()
    val forced = release.forceUpdate
    val notes = remember(release.releaseNotes) { releaseNoteLines(release.releaseNotes) }
    val released = remember(release.releasedAt) { releaseDate(release.releasedAt) }

    Dialog(
        onDismissRequest = { if (!forced) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !forced,
            dismissOnClickOutside = !forced,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = Shapes.panel,
            color = SurfaceColor,
            border = BorderStroke(1.dp, if (forced) Lime else BorderHairline),
            shadowElevation = 16.dp,
            modifier = Modifier
                .padding(horizontal = Space.gutter)
                .fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth()) {
                // The colour band: Jepjep with the news, the version change beneath him.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LimeTint)
                        .padding(top = Space.lg, bottom = Space.md)
                ) {
                    Jepjep(pose = if (forced) JepjepPose.Pointing else JepjepPose.Celebrating, height = 104.dp)
                    Spacer(Modifier.height(Space.sm))
                    Text(
                        text = if (forced) "Update required" else "A new version is here",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Space.xs))
                    VersionChange(from = actions.installedVersion, to = release.versionName)
                    if (released != null) {
                        Spacer(Modifier.height(Space.xxs))
                        Text("Released $released", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }

                Column(Modifier.padding(horizontal = Space.lg, vertical = Space.md)) {
                    Text(
                        text = if (forced) "This version replaces the one you have. Your progress is kept when you update."
                            else "Your progress is kept when you update.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (forced) Ink else Muted
                    )

                    if (notes.isNotEmpty()) {
                        Spacer(Modifier.height(Space.md))
                        Text("What's new", style = MaterialTheme.typography.titleSmall, color = Ink)
                        Spacer(Modifier.height(Space.xs))
                        // Long notes scroll inside the card, so the buttons never leave the screen.
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Space.xs),
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            notes.forEach { line -> NoteLine(line) }
                        }
                    }

                    Spacer(Modifier.height(Space.lg))
                    DownloadProgress(download)
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
                        label = when (val state = download) {
                            is UpdateDownload.Downloading -> state.percent?.let { "Downloading… $it%" } ?: "Downloading…"
                            UpdateDownload.Ready -> if (blocked) "Open the download page" else "Install update"
                            UpdateDownload.Failed -> "Try again"
                            UpdateDownload.Idle -> "Update now"
                        },
                        onClick = when {
                            blocked -> actions.openDownloadPage
                            download == UpdateDownload.Ready -> actions.install
                            else -> actions.startDownload
                        },
                        enabled = download !is UpdateDownload.Downloading,
                        modifier = Modifier.fillMaxWidth(),
                        leading = {
                            Icon(
                                painter = painterResource(id = Iconsax.ArrowDown),
                                contentDescription = null,
                                tint = OnLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    if (download == UpdateDownload.Failed) {
                        TextButton(onClick = actions.openInBrowser, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Download in the browser instead", color = LimeText, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (!forced) {
                        TextButton(onClick = onLater, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Later", color = Muted, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

/** "1.23.0 → 1.24.0": what the learner has and what they will get. */
@Composable
private fun VersionChange(from: String, to: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(Shapes.pill)
            .background(SurfaceColor)
            .padding(horizontal = Space.sm, vertical = Space.xxs)
    ) {
        Text(from, style = MaterialTheme.typography.labelLarge, color = Muted)
        Icon(
            painter = painterResource(id = Iconsax.ArrowRight),
            contentDescription = "to",
            tint = Muted,
            modifier = Modifier.padding(horizontal = Space.xxs).size(14.dp)
        )
        Text(to, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Ink)
    }
}

/** The download bar, only while a download is running. */
@Composable
internal fun DownloadProgress(download: UpdateDownload) {
    val progress = download as? UpdateDownload.Downloading ?: return
    val percent = progress.percent
    if (percent != null) {
        LinearProgressIndicator(
            progress = { percent / 100f },
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

/** What the update surfaces do, shared so the pop-up and the Home banner download the same way. */
internal class UpdateActions(
    val installedVersion: String,
    val startDownload: () -> Unit,
    val install: () -> Unit,
    val openInBrowser: () -> Unit,
    val openDownloadPage: () -> Unit
)

/**
 * Downloads in the app rather than in a browser tab: Chrome kept every update link as a tab and
 * offered the old APKs again on the next update. See UpdateDownloader.
 */
@Composable
internal fun rememberUpdateActions(release: AppReleaseDto, updates: AppUpdateViewModel): UpdateActions {
    val context = LocalContext.current
    val targetUrl = release.apkUrl.ifBlank { "https://kasiguru.web.app/download.html" }
    // Only http/https update links (defence against javascript:/intent:/etc. URLs in release metadata).
    val safeUrl = Uri.parse(targetUrl).scheme.let { it == "https" || it == "http" }
    val openInBrowser: () -> Unit = {
        if (safeUrl) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))
        else Log.w("AppUpdate", "Blocked update URL with unsafe scheme: $targetUrl")
    }

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
            Log.w("AppUpdate", "Blocked update URL with unsafe scheme: $targetUrl")
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
            // existing package"; the pop-up explains instead.
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
    val installed = remember(context) { installedVersionName(context) }
    return UpdateActions(installed, startDownload, install, openInBrowser, openDownloadPage)
}

/**
 * Asked of the installed package, not BuildConfig: a BuildConfig string is inlined into this file
 * when it compiles, so an incremental build can show the version it had then.
 */
private fun installedVersionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull() ?: BuildConfig.VERSION_NAME

/** The notes as lines, without the "-" or "•" the release file writes them with. */
internal fun releaseNoteLines(notes: String): List<String> = notes.lineSequence()
    .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
    .filter { it.isNotEmpty() }
    .toList()

/** "2 Oct 2026", or null for a release without a real timestamp. */
internal fun releaseDate(releasedAt: Long): String? {
    if (releasedAt <= 0L) return null
    return Instant.ofEpochMilli(releasedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
}
