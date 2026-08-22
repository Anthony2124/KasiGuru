package com.kasiguru.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.data.remote.model.AnnouncementDto
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Violet

/**
 * A live, admin-authored system announcement - read from Firestore `announcements` while the
 * app is open (see [com.kasiguru.data.repository.AnnouncementRepository]). There is no per-user
 * dismiss: visibility is the admin's `active` flag, the same "the admin controls whether this
 * shows" model the app already uses for stories and releases, not a notification the learner
 * clears themselves.
 */
@Composable
fun AnnouncementBanner(announcement: AnnouncementDto, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Row {
            Icon(
                painter = painterResource(id = Iconsax.InfoCircle),
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(Space.sm))
            Column {
                if (announcement.title.isNotBlank()) {
                    Text(
                        text = announcement.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink
                    )
                    Spacer(Modifier.height(Space.xxs))
                }
                Text(
                    text = announcement.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        }
    }
}
