package com.kasiguru.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.util.update.UpdateDownload

/**
 * The reminder an optional update leaves on Home after "Later" in [AppUpdateDialog]: one slim row,
 * so Home's lime Continue stays the one thing to do. "Update" opens the pop-up again, with the
 * notes and the download; the ✕ puts this version away for good.
 */
@Composable
fun AppUpdateBanner(
    release: AppReleaseDto,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    updates: AppUpdateViewModel = hiltViewModel()
) {
    val download by updates.state.collectAsState()
    LaunchedEffect(release.versionCode) { updates.watch(release) }

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline,
        contentPadding = PaddingValues(start = Space.sm, end = Space.xxs, top = Space.xs, bottom = Space.xs),
        onClick = onOpen
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(LimeTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.ArrowDown),
                    contentDescription = null,
                    tint = LimeText,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Version ${release.versionName} is ready",
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink
                )
                Text(
                    text = when (val state = download) {
                        is UpdateDownload.Downloading -> state.percent?.let { "Downloading… $it%" } ?: "Downloading…"
                        UpdateDownload.Ready -> "Downloaded. Tap to install"
                        UpdateDownload.Failed -> "Download failed. Tap to try again"
                        UpdateDownload.Idle -> "See what's new"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted
                )
            }
            TextButton(onClick = onOpen) {
                Text("Update", color = LimeText, style = MaterialTheme.typography.labelLarge)
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(id = Iconsax.CloseCircle),
                    contentDescription = "Dismiss update",
                    tint = Muted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (download is UpdateDownload.Downloading) {
            Spacer(Modifier.height(Space.xs))
            DownloadProgress(download)
        }
    }
}
