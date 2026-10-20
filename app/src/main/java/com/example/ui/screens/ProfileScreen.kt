package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserProgram
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

data class Achievement(
    val title: String,
    val criteria: String,
    val pointsRequired: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ProfileScreen(
    viewModel: AssemblyViewModel,
    onLoadProgram: (UserProgram) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.userStats.collectAsState()
    val savedPrograms by viewModel.userPrograms.collectAsState()

    val achievements = remember {
        listOf(
            Achievement("Silicon Initiate", "Earn 100 XP globally", 100, Icons.Default.Computer),
            Achievement("Kernel Commander", "Earn 500 XP globally", 500, Icons.Default.Terminal),
            Achievement("SIMD Graphics Overlord", "Earn 1,200 XP globally", 1200, Icons.Default.ViewInAr),
            Achievement("Assembly Grandmaster", "Earn 2,500 XP globally", 2500, Icons.Default.WorkspacePremium)
        )
    }

    val totalXp = stats?.totalPoints ?: 0
    val rank = when {
        totalXp >= 2500 -> "Assembly Grandmaster"
        totalXp >= 1200 -> "SIMD Shader Architect"
        totalXp >= 500 -> "Kernel Pioneer"
        totalXp >= 100 -> "Silicon Initiate"
        else -> "Hardware Apprentice"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .drawBehind {
                    drawLine(
                        color = BrightPink,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column {
                Text(
                    text = "ACADEMY STATUS & ARCHIVES",
                    color = PureWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Manage your custom source codes and verify engineering ranks.",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, bottom = 80.dp)
        ) {
            
            // --- Rank Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, BrightPink.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrightPink.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Rank Icon",
                                tint = BrightPink,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "CURRENT CADET RANK:",
                                color = BrightPink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = rank,
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Score: $totalXp XP",
                                color = MutedText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // --- Achievements Section ---
            item {
                Text(
                    text = "SILICON HARDWARE ACCOMPLISHMENTS",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                // Show achievement indicators
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    achievements.forEach { achievement ->
                        val isUnlocked = totalXp >= achievement.pointsRequired
                        val borderCol = if (isUnlocked) NeonGreen else TerminalLightGray
                        val bgAlpha = if (isUnlocked) 0.12f else 0.05f
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(TerminalDarkGray)
                                .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isUnlocked) NeonGreen.copy(alpha = bgAlpha) else TerminalBlack),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = achievement.icon,
                                        contentDescription = "Achieve Icon",
                                        tint = if (isUnlocked) NeonGreen else MutedText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = achievement.title,
                                        color = if (isUnlocked) PureWhite else MutedText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = achievement.criteria,
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }
                                
                                if (isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Unlocked",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = MutedText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Custom saved programs library ---
            item {
                Text(
                    text = "MY CUSTOM ASSEMBLY LIBRARY",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            if (savedPrograms.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalDarkGray)
                            .border(1.dp, TerminalLightGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Empty Library",
                                tint = MutedText,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Source Code Library Empty",
                                color = MutedText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Click Save inside the Playground to archive files.",
                                color = MutedText,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            } else {
                items(savedPrograms) { program ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                        border = BorderStroke(1.dp, TerminalLightGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = program.architecture.uppercase(),
                                        color = NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = program.title,
                                        color = PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Compiled with step-by-step debugger",
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            
                            Row {
                                IconButton(onClick = { onLoadProgram(program) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Run",
                                        tint = CyberCyan
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteProgram(program.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = BrightPink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
