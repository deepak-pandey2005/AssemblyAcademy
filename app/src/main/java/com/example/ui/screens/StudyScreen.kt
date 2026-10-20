package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import com.example.simulator.AssemblySimulator
import com.example.simulator.CpuState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Topic
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    viewModel: AssemblyViewModel,
    onBack: () -> Unit,
    onLaunchSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topic by viewModel.selectedTopic.collectAsState()
    val completedLessons by viewModel.completedLessons.collectAsState()
    val currentArch by viewModel.selectedArchitecture.collectAsState()
    
    val selectedOption by viewModel.selectedQuizOption.collectAsState()
    val quizSubmitted by viewModel.quizSubmitted.collectAsState()
    val quizCorrect by viewModel.quizIsCorrect.collectAsState()

    if (topic == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No Topic Selected", color = MutedText)
        }
        return
    }

    val currentTopic = topic!!
    val isAlreadyCompleted = remember(completedLessons, currentTopic) {
        completedLessons.any { it.topicId == currentTopic.id }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- Toolbar ---
        TopAppBar(
            title = {
                Text(
                    text = currentTopic.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = PureWhite
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonGreen
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TerminalDarkGray),
            modifier = Modifier.background(TerminalDarkGray)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Lesson Content Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "Study",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPILER THEORY & SCHEMATICS",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Body Content
                    Text(
                        text = "Hardware Architectural Model",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple custom formatter for mock markdown
                    val lines = currentTopic.detailedContent.split("\n")
                    lines.forEach { line ->
                        when {
                            line.startsWith("#") -> {
                                Text(
                                    text = line.replace("#", "").trim(),
                                    color = NeonGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("###") -> {
                                Text(
                                    text = line.replace("###", "").trim(),
                                    color = CyberCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("-") -> {
                                Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp)) {
                                    Text("• ", color = NeonGreen, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = line.replace("-", "").trim(),
                                        color = PureWhite,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            line.isNotBlank() -> {
                                Text(
                                    text = line.trim(),
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // --- Code block preview & Inline Debugger ---
                    if (currentTopic.codeTemplate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "INLINE HARDWARE EMULATOR",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        var inlineCpuState by remember(currentTopic) {
                            mutableStateOf(AssemblySimulator.createInitialState(currentArch))
                        }
                        val inlineHistory = remember(currentTopic) { mutableStateListOf<CpuState>() }
                        val codeLines = currentTopic.codeTemplate.lines()
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(TerminalBlack)
                                .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(6.dp))
                        ) {
                            Column {
                                // Gutter + Code Lines block
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // Gutter
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(36.dp)
                                            .background(TerminalDarkGray)
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        for (i in codeLines.indices) {
                                            val isActive = i == inlineCpuState.ip
                                            Text(
                                                text = (i + 1).toString(),
                                                color = if (isActive) NeonGreen else MutedText,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.height(20.dp)
                                            )
                                        }
                                    }
                                    
                                    // Code lines with highlight
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(vertical = 8.dp, horizontal = 12.dp)
                                    ) {
                                        Column {
                                            codeLines.forEachIndexed { idx, line ->
                                                val isActive = idx == inlineCpuState.ip
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(20.dp)
                                                        .background(
                                                            if (isActive) NeonGreen.copy(alpha = 0.15f) else Color.Transparent
                                                        ),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = line,
                                                        color = if (isActive) PureWhite else NeonGreen.copy(alpha = 0.8f),
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                // Explanation / Log console
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TerminalDarkGray)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "STATUS BUS EXPLANATION:",
                                            color = MutedText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = inlineCpuState.explanation,
                                            color = PureWhite,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                // Control buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TerminalBlack)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Reset
                                    TextButton(
                                        onClick = {
                                            inlineCpuState = AssemblySimulator.createInitialState(currentArch)
                                            inlineHistory.clear()
                                        },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, "Reset", tint = BrightPink, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("RESET", color = BrightPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    // Step Back (Undo)
                                    TextButton(
                                        onClick = {
                                            if (inlineHistory.isNotEmpty()) {
                                                inlineCpuState = inlineHistory.removeAt(inlineHistory.lastIndex)
                                            }
                                        },
                                        enabled = inlineHistory.isNotEmpty(),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Undo, "Undo", tint = if (inlineHistory.isNotEmpty()) CyberCyan else MutedText, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("UNDO", color = if (inlineHistory.isNotEmpty()) CyberCyan else MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    
                                    Spacer(modifier = Modifier.weight(1f))
                                    
                                    // Step Forward
                                    Button(
                                        onClick = {
                                            inlineHistory.add(inlineCpuState)
                                            inlineCpuState = AssemblySimulator.executeStep(inlineCpuState, codeLines)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.SkipNext, "Step", tint = TerminalBlack, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("STEP", color = TerminalBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                
                                // Compact registers display
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(TerminalDarkGray)
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "REGISTERS STATE:",
                                            color = MutedText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        val registersToShow = inlineCpuState.registers.toList()
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            registersToShow.forEach { (regName, regVal) ->
                                                Column(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(TerminalBlack)
                                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = regName,
                                                        color = NeonGreen,
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = regVal.toString(),
                                                        color = PureWhite,
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Button to Open Sandbox ---
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.selectTopic(currentTopic)
                            onLaunchSandbox()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debugger",
                            tint = TerminalBlack
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN IN INTERACTIVE DEBUGGER",
                            color = TerminalBlack,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Interactive Quiz Card ---
            if (currentTopic.quiz != null) {
                val quiz = currentTopic.quiz!!
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(
                        1.dp, 
                        if (quizSubmitted && quizCorrect) NeonGreen else if (quizSubmitted) BrightPink else TerminalLightGray
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Quiz",
                                tint = BrightPink,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SILICON REGISTER CHALLENGE",
                                color = BrightPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = quiz.question,
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Render options
                        quiz.options.forEachIndexed { idx, option ->
                            val isSelected = selectedOption == idx
                            val optionBorderColor = when {
                                quizSubmitted && idx == quiz.correctAnswerIndex -> NeonGreen
                                quizSubmitted && isSelected && !quizCorrect -> BrightPink
                                isSelected -> CyberCyan
                                else -> TerminalLightGray
                            }
                            
                            val optionBg = when {
                                quizSubmitted && idx == quiz.correctAnswerIndex -> NeonGreen.copy(alpha = 0.12f)
                                quizSubmitted && isSelected && !quizCorrect -> BrightPink.copy(alpha = 0.12f)
                                isSelected -> CyberCyan.copy(alpha = 0.1f)
                                else -> TerminalBlack
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(optionBg)
                                    .clickable(enabled = !quizSubmitted) {
                                        viewModel.selectQuizOption(idx)
                                    }
                                    .drawBehind {
                                        drawLine(
                                            color = optionBorderColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 10f
                                        )
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${(65 + idx).toChar()}: ",
                                        color = if (isSelected) CyberCyan else MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = option,
                                        color = PureWhite,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        if (!quizSubmitted) {
                            Button(
                                onClick = { viewModel.submitQuiz() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(8.dp),
                                enabled = selectedOption != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "COMPILE & TEST RESPONSE",
                                    color = TerminalBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Quiz Explanation Panel
                        AnimatedVisibility(
                            visible = quizSubmitted,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TerminalBlack)
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (quizCorrect) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                        contentDescription = "Result",
                                        tint = if (quizCorrect) NeonGreen else BrightPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (quizCorrect) "COMPILATION SUCCESSFUL (+100 XP)" else "COMPILATION ERROR",
                                        color = if (quizCorrect) NeonGreen else BrightPink,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = quiz.explanation,
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                
                                if (isAlreadyCompleted) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "✔ Previously completed. XP already claimed.",
                                        color = CyberCyan,
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
}
