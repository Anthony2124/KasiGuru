package com.kasiguru.ui.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.ui.components.clay.CanopyBackButton
import com.kasiguru.ui.components.clay.CanopyScaffold
import com.kasiguru.ui.components.clay.SoftCard
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

data class FaqItem(
    val question: String,
    val answer: String
)

/**
 * The identity the old screen re-stated inside a second gradient card belongs in the canopy itself —
 * "who this app is" is exactly the canopy's job, so the logo/name/tagline moved there and the sheet is
 * left to do only the sheet's job: the FAQ list.
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val faqs = listOf(
        FaqItem(
            question = "What is KasiGuru?",
            answer = "KasiGuru is an offline-first mobile application designed to preserve, document, and teach the Kasiguranin language of Casiguran, Aurora."
        ),
        FaqItem(
            question = "Does KasiGuru work offline?",
            answer = "Yes! All 417 vocabulary entries, IPA transcriptions, example sentences, and mini-games are stored locally on your device in an SQLite database."
        ),
        FaqItem(
            question = "Where does the language data come from?",
            answer = "The linguistic corpus is based on the 2016 UP Diliman thesis 'A Grammatical Sketch of Kasiguranin' by Chiara Paola E. Supnet."
        ),
        FaqItem(
            question = "How do I report corrections or missing words?",
            answer = "We welcome community contributions! You can submit language feedback or contact our team via the support section."
        )
    )

    CanopyScaffold(
        canopyHeight = 220.dp,
        canopyContent = {
            CanopyBackButton(onClick = onNavigateBack)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = Color.White) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = Iconsax.Book),
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "KasiGuru",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = OnCanopy
                )
                Text(
                    text = "Preserving Casiguran, Aurora's heritage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnCanopy,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            Spacer(Modifier.weight(1f))
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Space.gutter),
                verticalArrangement = Arrangement.spacedBy(Space.sm)
            ) {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )

                faqs.forEach { faq -> FaqCard(faq = faq) }

                Spacer(Modifier.height(Space.navBarClearance))
            }
        }
    )
}

@Composable
private fun FaqCard(faq: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }

    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = faq.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painter = painterResource(id = if (isExpanded) Iconsax.ArrowUp1 else Iconsax.ArrowDown1),
                contentDescription = null,
                tint = Muted,
                modifier = Modifier.size(22.dp)
            )
        }

        if (isExpanded) {
            Spacer(Modifier.height(Space.sm))
            HorizontalDivider(color = Faint.copy(alpha = 0.3f))
            Spacer(Modifier.height(Space.sm))
            Text(
                text = faq.answer,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                lineHeight = 22.sp
            )
        }
    }
}
