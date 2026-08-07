package com.kasiguru.ui.screens.contribute

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitWordScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubmitWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    val tealGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0F5257), Color(0xFF168070))
    )
    val lightBgColor = Color(0xFFF8FAFC)
    val cardBorderColor = Color(0xFFE2E8F0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Contribute a Word",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(lightBgColor)
        ) {
            if (uiState.isSuccess) {
                // Success Screen Card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = "Success",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Submission Sent for Review! 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Thank you for helping preserve the Kasiguranin heritage. Your contribution is now queued for verification by our cultural language experts!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.resetSuccess() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168070))
                    ) {
                        Text("Submit Another Word", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Return to Home", fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Hero Information Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(tealGradient)
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "Preservation Mission 🌿",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Help Expand Kasiguranin",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Share native words, phrases, or local expressions to be verified and published to KasiGuru!",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                painter = painterResource(id = Iconsax.Book),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️ " + uiState.errorMessage!!,
                                    color = Color(0xFF991B1B),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Section 1: Core Translations Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "1. Core Translations",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )

                            // Kasiguranin Word Input
                            OutlinedTextField(
                                value = uiState.kasiguranin,
                                onValueChange = { viewModel.onKasiguraninChanged(it) },
                                label = { Text("Kasiguranin Word *") },
                                placeholder = { Text("e.g. apak, singët, lukag") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Book),
                                        contentDescription = null,
                                        tint = Color(0xFF168070),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // Tagalog Translation
                            OutlinedTextField(
                                value = uiState.tagalog,
                                onValueChange = { viewModel.onTagalogChanged(it) },
                                label = { Text("Tagalog Translation *") },
                                placeholder = { Text("e.g. daras, langgam, gising") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Global),
                                        contentDescription = null,
                                        tint = Color(0xFF168070),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // English Translation
                            OutlinedTextField(
                                value = uiState.english,
                                onValueChange = { viewModel.onEnglishChanged(it) },
                                label = { Text("English Definition / Translation") },
                                placeholder = { Text("e.g. adze, ant, awake") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Global),
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    // Section 2: Category & Details Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "2. Category & Usage Details",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )

                            // Category Dropdown
                            ExposedDropdownMenuBox(
                                expanded = expandedCategoryDropdown,
                                onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = uiState.category,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = Iconsax.Element4Outline),
                                            contentDescription = null,
                                            tint = Color(0xFF168070),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF168070),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCategoryDropdown,
                                    onDismissRequest = { expandedCategoryDropdown = false }
                                ) {
                                    viewModel.categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                viewModel.onCategoryChanged(category)
                                                expandedCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Root Word
                            OutlinedTextField(
                                value = uiState.rootForm,
                                onValueChange = { viewModel.onRootFormChanged(it) },
                                label = { Text("Root Word (Optional)") },
                                placeholder = { Text("e.g. apak") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Edit),
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // Example Sentence
                            OutlinedTextField(
                                value = uiState.exampleSentence,
                                onValueChange = { viewModel.onExampleSentenceChanged(it) },
                                label = { Text("Example Sentence (Optional)") },
                                placeholder = { Text("How is this word used in a sentence?") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Document),
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    // Section 3: Contributor Credit Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, cardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "3. Contributor Credit",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )

                            OutlinedTextField(
                                value = uiState.contributorName,
                                onValueChange = { viewModel.onContributorNameChanged(it) },
                                label = { Text("Your Name / Credit (Optional)") },
                                placeholder = { Text("Leave blank to submit anonymously") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = Iconsax.Profile),
                                        contentDescription = null,
                                        tint = Color(0xFF168070),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF168070),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button with Gradient & Shadow
                    Button(
                        onClick = { viewModel.submitWord() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(10.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF168070),
                            disabledContainerColor = Color(0xFF94A3B8)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = Iconsax.Flash),
                                contentDescription = "Submit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Submit Entry for Verification",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
