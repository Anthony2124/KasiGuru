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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.ui.components.CoastPillButton
import com.kasiguru.ui.components.PillVariant
import com.kasiguru.ui.theme.*
import com.kasiguru.ui.theme.Iconsax

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitWordScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubmitWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = "Contribute a Word",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextHeadingBlack,
                        letterSpacing = (-0.3).sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = Iconsax.ArrowLeft),
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
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
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Success.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = Iconsax.TickCircle),
                            contentDescription = "Success",
                            tint = Success,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Submission Sent for Review!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoastInk,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you for helping preserve the Kasiguranin heritage. Your contribution is queued for verification by cultural language experts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoastMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    CoastPillButton(
                        label = "Submit Another Word",
                        onClick = { viewModel.resetSuccess() },
                        variant = PillVariant.Purple
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = onNavigateBack) {
                        Text(
                            "Return to Dashboard",
                            fontWeight = FontWeight.Bold,
                            color = CoastMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Information Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(VocabSea, VocabSeaDark)
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        color = Color.White.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = "PRESERVATION MISSION",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Help Expand Kasiguranin",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = Color.White,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Share native words, phrases, or local expressions to be verified and published to KasiGuru.",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        lineHeight = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    painter = painterResource(id = Iconsax.BookBold),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Surface(
                            color = ErrorLight,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = Iconsax.InfoCircle),
                                    contentDescription = null,
                                    tint = Error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = uiState.errorMessage!!,
                                    color = Error,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Section 1: Core Translations Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
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
                                color = CoastInk,
                                letterSpacing = (-0.2).sp
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
                                        tint = VocabSea,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                                        tint = VocabSea,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                                        tint = CoastMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Section 2: Category & Details Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
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
                                color = CoastInk,
                                letterSpacing = (-0.2).sp
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
                                            tint = VocabSea,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VocabSea,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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

                            // Part of Speech Dropdown
                            var expandedPartOfSpeechDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedPartOfSpeechDropdown,
                                onExpandedChange = { expandedPartOfSpeechDropdown = !expandedPartOfSpeechDropdown },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = uiState.partOfSpeech.ifEmpty { "(Select Part of Speech)" },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Part of Speech") },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(id = Iconsax.HashtagDown),
                                            contentDescription = null,
                                            tint = VocabSea,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPartOfSpeechDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VocabSea,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPartOfSpeechDropdown,
                                    onDismissRequest = { expandedPartOfSpeechDropdown = false }
                                ) {
                                    viewModel.partsOfSpeech.forEach { pos ->
                                        DropdownMenuItem(
                                            text = { Text(pos, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                viewModel.onPartOfSpeechChanged(pos)
                                                expandedPartOfSpeechDropdown = false
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
                                        tint = CoastMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
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
                                        tint = CoastMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Section 3: Verb Tenses (Optional)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "3. Verb Tenses (Optional)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = CoastInk,
                                letterSpacing = (-0.2).sp
                            )

                            // Past Tense
                            OutlinedTextField(
                                value = uiState.pastTense,
                                onValueChange = { viewModel.onPastTenseChanged(it) },
                                label = { Text("Past Tense") },
                                placeholder = { Text("e.g. naglakaw") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )

                            // Present Tense
                            OutlinedTextField(
                                value = uiState.presentTense,
                                onValueChange = { viewModel.onPresentTenseChanged(it) },
                                label = { Text("Present Tense") },
                                placeholder = { Text("e.g. nagalakaw") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )

                            // Future Tense
                            OutlinedTextField(
                                value = uiState.futureTense,
                                onValueChange = { viewModel.onFutureTenseChanged(it) },
                                label = { Text("Future Tense") },
                                placeholder = { Text("e.g. magalakaw") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Section 4: Contributor Credit Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "4. Contributor Credit",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = CoastInk,
                                letterSpacing = (-0.2).sp
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
                                        tint = VocabSea,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VocabSea,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Submit Button
                    Button(
                        onClick = { viewModel.submitWord() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayPurpleStart,
                            disabledContainerColor = PlayPurpleStart.copy(alpha = 0.4f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = Iconsax.AddCircle),
                                contentDescription = "Submit",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit Entry for Verification",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
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
