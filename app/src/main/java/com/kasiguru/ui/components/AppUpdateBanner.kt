package com.kasiguru.ui.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kasiguru.data.remote.model.AppReleaseDto
import com.kasiguru.ui.components.clay.ClayButton
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.BorderHairline
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.LimeText
import com.kasiguru.ui.theme.LimeTint
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.OnLime
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space

/**
 * A new version of the app is out. One compact card: what it is, a line of its notes, and the
 * download. Optional updates can be put off with "Later"; a forced one cannot.
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

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.tile,
        border = BorderHairline
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LimeTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.ArrowDown),
                    contentDescription = null,
                    tint = LimeText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(Space.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Version ${release.versionName} is ready",
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (release.releaseNotes.isNotBlank()) {
                    Text(
                        text = release.releaseNotes.lineSequence()
                            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
                            .firstOrNull { it.isNotEmpty() }
                            .orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!release.forceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = Muted, style = MaterialTheme.typography.labelLarge)
                }
            }
            ClayButton(
                label = "Download",
                onClick = download,
                leading = {
                    Icon(
                        painter = painterResource(id = Iconsax.ArrowDown),
                        contentDescription = null,
                        tint = OnLime,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
