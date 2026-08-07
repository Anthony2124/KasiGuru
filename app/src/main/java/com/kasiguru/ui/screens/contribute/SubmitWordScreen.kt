package com.kasiguru.ui.screens.contribute

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

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
                title = {
                    Text(
                        text = "Contribute a Word",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isSuccess) {
                // Success Dialog / Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Thank You for Contributing!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your submission has been sent to our linguistic review queue. Once verified by our administrators, it will be added to the official Kasiguranin dictionary!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetSuccess() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Another Word")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Home")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Information Banner
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Help Preserve Kasiguranin 🌿",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Know a Kasiguranin word, phrase, or local expression? Submit it here so our cultural language experts can verify and add it to KasiGuru!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Kasiguranin Word Input (Required)
                    OutlinedTextField(
                        value = uiState.kasiguranin,
                        onValueChange = { viewModel.onKasiguraninChanged(it) },
                        label = { Text("Kasiguranin Word *") },
                        placeholder = { Text("e.g. apak, singët, lukag") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Tagalog Translation (Required)
                    OutlinedTextField(
                        value = uiState.tagalog,
                        onValueChange = { viewModel.onTagalogChanged(it) },
                        label = { Text("Tagalog Translation *") },
                        placeholder = { Text("e.g. daras, langgam, gising") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // English Translation
                    OutlinedTextField(
                        value = uiState.english,
                        onValueChange = { viewModel.onEnglishChanged(it) },
                        label = { Text("English Definition / Translation") },
                        placeholder = { Text("e.g. adze, ant, awake") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Category Selection Dropdown
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCategoryDropdown,
                            onDismissRequest = { expandedCategoryDropdown = false }
                        ) {
                            viewModel.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        viewModel.onCategoryChanged(category)
                                        expandedCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Root Form
                    OutlinedTextField(
                        value = uiState.rootForm,
                        onValueChange = { viewModel.onRootFormChanged(it) },
                        label = { Text("Root Word (Optional)") },
                        placeholder = { Text("e.g. apak") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Example Sentence
                    OutlinedTextField(
                        value = uiState.exampleSentence,
                        onValueChange = { viewModel.onExampleSentenceChanged(it) },
                        label = { Text("Example Sentence (Optional)") },
                        placeholder = { Text("How is this word used in a sentence?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Contributor Name
                    OutlinedTextField(
                        value = uiState.contributorName,
                        onValueChange = { viewModel.onContributorNameChanged(it) },
                        label = { Text("Your Name / Credit (Optional)") },
                        placeholder = { Text("Leave blank to submit as Anonymous") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = { viewModel.submitWord() },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Submit"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Entry for Review", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
