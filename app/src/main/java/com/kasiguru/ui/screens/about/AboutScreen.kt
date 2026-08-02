package com.kasiguru.ui.screens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { Text("About & Help") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🌾", fontSize = 40.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("KasiGuru", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextWhite)
                    Text("Version 2.0.0 (Build 2026)", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Empowering indigenous language preservation through gamified learning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 20.sp
                    )
                }
            }

            // FAQ Section
            Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)

            faqs.forEach { faq ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.question, color = TextWhite, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Secondary
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(faq.answer, style = MaterialTheme.typography.bodyMedium, color = TextGray, lineHeight = 20.sp)
                        }
                    }
                }
            }

            // Privacy & Legal
            Text("Privacy & Legal", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Success)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Privacy Policy & Terms", color = TextWhite, fontWeight = FontWeight.Bold)
                            Text("KasiGuru respects your privacy. No personal data is sold.", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    }
                }
            }
        }
    }
}
