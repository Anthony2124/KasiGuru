package com.kasiguru.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kasiguru.R
import com.kasiguru.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isDarkMode by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var dailyGoal by remember { mutableIntStateOf(10) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Data",
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
            // Preferences Section
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = TextHeadingBlack,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_setting_outline),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Dark Theme", color = TextHeadingBlack, fontWeight = FontWeight.Medium)
                        }
                        Switch(checked = isDarkMode, onCheckedChange = { isDarkMode = it })
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_volume_high),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Sound Effects & Audio", color = TextHeadingBlack, fontWeight = FontWeight.Medium)
                        }
                        Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                    }
                }
            }

            // Daily Goal Section
            Text(
                text = "Daily Goal",
                style = MaterialTheme.typography.titleMedium,
                color = TextHeadingBlack,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Words to learn per day:", style = MaterialTheme.typography.bodyMedium, color = TextSubtleGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { goal ->
                            FilterChip(
                                selected = dailyGoal == goal,
                                onClick = { dailyGoal = goal },
                                label = { Text("$goal words", fontWeight = FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HeroCardStart,
                                    selectedLabelColor = TextHeadingBlack
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }
            }

            // Storage & Sync Management
            Text(
                text = "Offline Storage & Sync",
                style = MaterialTheme.typography.titleMedium,
                color = TextHeadingBlack,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Local Database Cache", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                            Text("487 vocabulary entries cached offline", style = MaterialTheme.typography.bodySmall, color = TextSubtleGray)
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_document_outline),
                            contentDescription = null,
                            tint = TextHeadingBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Button(
                        onClick = {
                            isSyncing = true
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                isSyncing = false
                                syncMessage = "Local SQLite database verified (487 vocabulary entries)."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HeroCardStart),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextHeadingBlack, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing...", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_repeat_outline),
                                contentDescription = null,
                                tint = TextHeadingBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Database Now", color = TextHeadingBlack, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (syncMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(syncMessage, style = MaterialTheme.typography.bodySmall, color = Success, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
