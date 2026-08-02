package com.kasiguru.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kasiguru.data.local.entity.UserProgressEntity
import com.kasiguru.ui.theme.*

val profileIcons = listOf(
    Icons.Default.Person,
    Icons.Default.Face,
    Icons.Default.SentimentSatisfied,
    Icons.Default.Pets,
    Icons.Default.ChildCare,
    Icons.Default.EmojiEmotions
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val progress = uiState.userProgress ?: return
    val selectedIcon = profileIcons.getOrElse(progress.profileIconId) { Icons.Default.Person }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextWhite)
                    }
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(PrimaryDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = "Profile Avatar",
                    tint = PrimaryLight,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name
            Text(
                text = progress.fullName.ifBlank { progress.userName },
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "@${progress.userName}",
                style = MaterialTheme.typography.bodyLarge,
                color = PrimaryLight
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Info Cards
            ProfileInfoCard(
                icon = Icons.Default.Email,
                label = "Email",
                value = progress.email.ifBlank { "Not provided" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                icon = Icons.Default.Cake,
                label = "Age",
                value = progress.age?.toString() ?: "Not provided"
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileInfoCard(
                icon = Icons.Default.LocationOn,
                label = "Address",
                value = progress.address.ifBlank { "Not provided" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Stats
            Text(
                text = "Learning Stats",
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(modifier = Modifier.weight(1f), label = "Level", value = progress.level.toString(), color = Secondary)
                StatCard(modifier = Modifier.weight(1f), label = "XP", value = progress.totalXp.toString(), color = Primary)
            }
        }
    }
}

@Composable
fun ProfileInfoCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = TextMuted)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextGray)
                Text(text = value, style = MaterialTheme.typography.bodyLarge, color = TextWhite, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextGray)
        }
    }
}
