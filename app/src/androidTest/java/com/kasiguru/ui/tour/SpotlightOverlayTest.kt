package com.kasiguru.ui.tour

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.KasiGuruTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SpotlightOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun scrimBlocksTapsAndDragsWhileCaptionControlsKeepWorking() {
        var underlyingClicks = 0
        var skips = 0
        val anchors = TourAnchorRegistry().apply {
            report(TourAnchor.ContinueAction, Rect(0f, 0f, 240f, 180f), Rect(0f, 0f, 240f, 180f))
        }
        compose.setContent {
            KasiGuruTheme {
                var step by remember { mutableIntStateOf(0) }
                Box(Modifier.fillMaxSize()) {
                    Button(onClick = { underlyingClicks++ },
                        modifier = Modifier.padding(16.dp).testTag("underlying")) { Text("Underlying action") }
                    SpotlightOverlay(
                        stop = TourStop(TourTarget.Fixed("home"), TourAnchor.ContinueAction,
                            "Step $step", "Tour instructions"),
                        chapterTitle = "Gesture test", stepIndex = step, stepCount = 3,
                        anchors = anchors, anchorVisible = true, bottomBlocked = 0.dp,
                        onBack = { step-- }, onSkip = { skips++ }, onNext = { step++ }
                    )
                }
            }
        }
        repeat(3) {
            compose.onNodeWithTag("underlying").performTouchInput {
                click(center)
                swipe(center, center.copy(x = center.x + 60f), 200)
            }
            compose.onNodeWithText("Next").performTouchInput { click() }
            compose.onNodeWithText("Step 1").assertExists()
            compose.onNodeWithText("Back").performTouchInput { click() }
            compose.onNodeWithText("Step 0").assertExists()
        }
        compose.onNodeWithText("Skip").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(0, underlyingClicks)
            assertEquals(1, skips)
        }
    }
}
