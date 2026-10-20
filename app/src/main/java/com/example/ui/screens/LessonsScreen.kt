package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Architecture
import com.example.model.Chapter
import com.example.model.Syllabus
import com.example.model.Topic
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

@Composable
fun LessonsScreen(
    viewModel: AssemblyViewModel,
    onTopicSelected: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedArch by viewModel.selectedArchitecture.collectAsState()
    val completedLessons by viewModel.completedLessons.collectAsState()
    val stats by viewModel.userStats.collectAsState()

    val chapters = remember(selectedArch) {
        Syllabus.getChaptersFor(selectedArch)
    }

    // Calculate metrics for current architecture
    val completedCountForArch = remember(completedLessons, selectedArch) {
        completedLessons.count { it.topicId.startsWith(selectedArch.codeName) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- High-tech Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .drawBehind {
                    drawLine(
                        color = NeonGreen,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal",
                            tint = NeonGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ASSEMBLY ACADEMY",
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                    
                    // Streak indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BrightPink.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = BrightPink,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats?.streakDays ?: 0} DAYS",
                            color = BrightPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Compilers, Kernels & Parallel GPU Shaders",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- Architecture Selection Tabs ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Architecture.values().forEach { arch ->
                        val isSelected = arch == selectedArch
                        val borderColor = if (isSelected) NeonGreen else TerminalLightGray
                        val bgBrush = if (isSelected) {
                            Brush.verticalGradient(listOf(TerminalLightGray, TerminalBlack))
                        } else {
                            Brush.verticalGradient(listOf(TerminalDarkGray, TerminalDarkGray))
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgBrush)
                                .clickable { viewModel.selectArchitecture(arch) }
                                .drawBehind {
                                    if (isSelected) {
                                        drawLine(
                                            color = NeonGreen,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 6f
                                        )
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = when (arch) {
                                        Architecture.X86 -> Icons.Default.Memory
                                        Architecture.ARM -> Icons.Default.DeveloperBoard
                                        Architecture.OS_DEV -> Icons.Default.SettingsInputHdmi
                                        Architecture.GPU_DEV -> Icons.Default.ViewInAr
                                    },
                                    contentDescription = arch.displayName,
                                    tint = if (isSelected) NeonGreen else MutedText,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = arch.displayName,
                                    color = if (isSelected) PureWhite else MutedText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // --- Syllabus Progress Dashboard Card ---
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, TerminalLightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = selectedArch.tag.uppercase(),
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${selectedArch.displayName} Curriculum Progress",
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Progress Text
                            Text(
                                text = "$completedCountForArch / 50 TOPICS",
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Custom terminal-looking progress bar
                        val progress = completedCountForArch / 50f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(TerminalBlack)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(CyberCyan, NeonGreen)
                                        )
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "XP",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stats?.totalPoints ?: 0} Total XP",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hardware,
                                    contentDescription = "Opcodes",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stats?.instructionsExecuted ?: 0} Ops Compiled",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // --- Curated Course Chapters (200 Lessons Total) ---
            items(chapters) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    completedLessons = completedLessons.map { it.topicId },
                    onTopicSelected = onTopicSelected
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ChapterCard(
    chapter: Chapter,
    completedLessons: List<String>,
    onTopicSelected: (Topic) -> Unit
) {
    var isExpanded by remember { mutableStateOf(chapter.id == 1) }
    
    // Calculate progress inside chapter
    val completedInChapter = remember(completedLessons, chapter) {
        chapter.topics.count { completedLessons.contains(it.id) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, if (isExpanded) NeonGreen.copy(alpha = 0.5f) else TerminalLightGray)
    ) {
        Column {
            // Chapter Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (completedInChapter == 10) NeonGreen.copy(alpha = 0.15f) else TerminalBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chapter.id.toString(),
                            color = if (completedInChapter == 10) NeonGreen else CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = chapter.title,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$completedInChapter / 10 Topics Completed",
                            color = if (completedInChapter == 10) NeonGreen else MutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MutedText
                    )
                }
            }
            
            // Expandable List of 10 Lessons
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalBlack.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Divider
                    HorizontalDivider(color = TerminalLightGray, modifier = Modifier.padding(bottom = 8.dp))
                    
                    chapter.topics.forEachIndexed { index, topic ->
                        val isCompleted = completedLessons.contains(topic.id)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onTopicSelected(topic) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Status Circle
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Status",
                                    tint = if (isCompleted) NeonGreen else MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${chapter.id}.${index + 1}: ${topic.title}",
                                        color = if (isCompleted) NeonGreen else PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = topic.overview,
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Time",
                                    tint = MutedText,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${topic.estimatedMinutes}m",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
