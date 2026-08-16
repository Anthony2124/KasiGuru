package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.TextHeadingBlack
import com.kasiguru.ui.theme.TextSubtleGray
import com.kasiguru.ui.theme.Warning

/**
 * Warns a guest that their progress only exists on this device. Shown once the
 * user has real progress to lose; dismissal is remembered.
 */
@Composable
fun SecureProgressBanner(
    onSecure: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    tint = Warning,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Secure your progress",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHeadingBlack
                )
            }

            Text(
                text = "Your XP, streak and badges are saved only on this phone. Add an " +
                    "account so they come back if you reinstall or change device.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSubtleGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = TextSubtleGray)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSecure,
                    colors = ButtonDefaults.buttonColors(containerColor = Warning)
                ) {
                    Text("Add account", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
