package com.kasiguru.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kasiguru.R
import com.kasiguru.ui.theme.*

data class FaqItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
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
            answer = "Yes! All 487 vocabulary entries, IPA transcriptions, example sentences, and mini-games are stored locally on your device in an SQLite database."
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About & Help",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadingBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left),
                            contentDescription = "Back",
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(HeroCardStart, HeroCardEnd)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_book_outline),
                                    contentDescription = "Logo",
                                    tint = TextHeadingBlack,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("KasiGuru", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextHeadingBlack)
                        Text("Version 2.0.0 (Build 2026)", style = MaterialTheme.typography.bodySmall, color = TextHeadingBlack.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Kasiguranin Language Preservation & Learning Tool",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHeadingBlack.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // FAQ Section Header
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                color = TextHeadingBlack,
                fontWeight = FontWeight.Bold
            )

            faqs.forEach { faq ->
                FaqCard(item = faq)
            }

            // Privacy & Terms
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_info_circle),
                            contentDescription = null,
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Privacy Guidelines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextHeadingBlack)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "KasiGuru values your privacy. All user progress, learned vocabulary, and game scores remain 100% offline on your device. No personal data is transmitted without explicit user consent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtleGray,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FaqCard(item: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextHeadingBlack,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = if (isExpanded) R.drawable.ic_arrow_left else R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = TextSubtleGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtleGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
