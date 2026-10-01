package com.kasiguru.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.*

/** Cover -> word -> answer. The paper keeps dark ink in both application themes. */
@Composable
fun FlashCard(word: VocabularyEntity, number: Int, side: Int, onFlip: () -> Unit,
    onAudio: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val coverAngle by animateFloatAsState(if (side == 0) 0f else -110f,
        tween(if (reduced) 0 else 450), label = "book cover")
    val paperInk = RewardInk
    val coverColors = listOf(Olive, Olive)
    val shape = RoundedCornerShape(28.dp)
    Box(modifier.padding(12.dp)) {
        Box(Modifier.matchParentSize().graphicsLayer { rotationZ = 8f; translationY = 8.dp.toPx() }
            .clip(shape).background(Cream.copy(alpha = .5f)).border(3.dp, Lime, shape))
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = -2f }.clip(shape)
            .background(Cream).border(3.dp, Lime, shape).clickable(onClick = onFlip)) {
            Crossfade(targetState = side == 2, animationSpec = tween(if (reduced) 0 else 180), label = "paper side") { answer ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(categoryDoodle(word.category)), null, tint = paperInk, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(20.dp))
                    if (answer) {
                        Text("Meaning", style = MaterialTheme.typography.labelLarge, color = paperInk)
                        Text(word.meaningEnglish.ifBlank { word.english }, style = MaterialTheme.typography.headlineSmall, color = paperInk)
                        Spacer(Modifier.height(12.dp))
                        Text("Tagalog: ${word.tagalog}", style = MaterialTheme.typography.bodyLarge, color = paperInk)
                        if (word.exampleSentence.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(word.exampleSentence, style = MaterialTheme.typography.bodyMedium, color = paperInk)
                            if (word.exampleTranslation.isNotBlank()) Text(word.exampleTranslation, color = paperInk)
                        }
                    } else {
                        Text(word.kasiguranin, style = MaterialTheme.typography.headlineLarge, color = paperInk)
                        if (word.ipaNotation.isNotBlank()) Text("[${word.ipaNotation}]", color = paperInk)
                        IconButton(onClick = onAudio, modifier = Modifier.size(48.dp)) {
                            Icon(painterResource(Iconsax.VolumeHigh), "Listen to the word", tint = paperInk)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(number.toString().padStart(2, '0'), style = MaterialTheme.typography.labelMedium, color = paperInk)
                }
            }
            if (side == 0 || (!reduced && coverAngle > -109f)) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    rotationY = coverAngle; transformOrigin = TransformOrigin(0f, .5f); cameraDistance = 24f * density
                }.clip(shape).background(Brush.linearGradient(coverColors))) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawOval(Lime, topLeft = androidx.compose.ui.geometry.Offset(size.width * .55f, -size.height * .08f), size = androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .45f))
                        drawOval(Lime.copy(alpha = .28f), topLeft = androidx.compose.ui.geometry.Offset(-size.width * .4f, size.height * .72f), size = androidx.compose.ui.geometry.Size(size.width * 1.2f, size.height * .5f))
                    }
                    Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(categoryDoodle(word.category)), null, tint = Cream, modifier = Modifier.size(88.dp))
                        Spacer(Modifier.height(24.dp))
                        Text(word.category, style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text("Tap to open", style = MaterialTheme.typography.bodyMedium, color = Cream)
                    }
                }
            }
        }
    }
}
