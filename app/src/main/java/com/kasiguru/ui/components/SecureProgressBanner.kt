package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.AmberText
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.RewardInk
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Warning

/**
 * Warns a guest that their progress only exists on this device. Shown once the
 * user has real progress to lose; dismissal is remembered.
 */
@Composable
fun SecureProgressBanner(
    onSecure: () -> Unit,
    onDismiss: () -> Unit,
    collapsed: Boolean = false
) {
    var expanded by remember { mutableStateOf(!collapsed) }
    if (!expanded) {
        SoftCard(modifier = Modifier.fillMaxWidth(), onClick = { expanded = true }, contentPadding = PaddingValues(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(Iconsax.Lock), null, tint = AmberText, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(Space.sm))
                Text("Secure your progress", color = Ink, modifier = Modifier.weight(1f))
                Icon(painterResource(Iconsax.ArrowRight), "Show account options", tint = AmberText, modifier = Modifier.size(20.dp))
            }
        }
        return
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = Iconsax.Lock),
                    contentDescription = null,
                    tint = AmberText,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Secure your progress",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
            }

            Text(
                text = "Your XP, streak and badges are saved only on this phone. Add an " +
                    "account so they come back if you reinstall or change device.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = Muted)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSecure,
                    colors = ButtonDefaults.buttonColors(containerColor = Warning)
                ) {
                    Text("Add account", color = RewardInk, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
