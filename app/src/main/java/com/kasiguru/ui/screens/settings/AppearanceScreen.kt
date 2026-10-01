package com.kasiguru.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.domain.preferences.*
import com.kasiguru.ui.components.clay.*
import com.kasiguru.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(onBack: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val mode by viewModel.mode.collectAsState()
    val size by viewModel.textSize.collectAsState()
    GroundScaffold("Appearance", onBack = onBack, compactTitle = true, pattern = GroundPattern.None) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.gutter), verticalArrangement = Arrangement.spacedBy(Space.lg)) {
            com.kasiguru.ui.components.clay.GroundTitleBlock(title = "Appearance", subtitle = "Theme and text size")
            Column {
                SectionHeading("Theme")
                Spacer(Modifier.height(Space.sm))
                SegmentedToggle(listOf("System", "Light", "Dark"), mode.ordinal,
                    { viewModel.setMode(AppearanceMode.entries[it]) }, Modifier.fillMaxWidth())
                Text("System follows your phone's theme.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Column {
                SectionHeading("Text size")
                Slider(value = TextSize.steps.indexOf(size).coerceAtLeast(0).toFloat(), valueRange = 0f..3f, steps = 2,
                    onValueChange = { viewModel.setTextSize(TextSize.steps[it.roundToInt().coerceIn(0, 3)]) })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextSize.steps.forEach { Text("$it%", color = if (it == size) BrandLime else Muted, style = MaterialTheme.typography.labelMedium) }
                }
                Text("Adds to the text size set on your phone.", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            SoftCard(Modifier.fillMaxWidth()) {
                Text("Preview · $size%", color = Muted, style = MaterialTheme.typography.labelLarge)
                Text("Learning at your pace", color = Ink, style = MaterialTheme.typography.headlineMedium)
                Text("Words, meanings and stories should be comfortable to read.", color = Muted, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Space.md))
                ClayButton("Continue", onBack, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
