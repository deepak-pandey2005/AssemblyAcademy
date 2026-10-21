package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Architecture
import com.example.model.Lesson
import com.example.model.X86TutorialLessons
import com.example.simulator.AssemblySimulator
import com.example.simulator.CpuState
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

/**
 * Interactive Compose UI Layout to render x86 assembly language tutorials.
 * Features:
 * - Rich Conceptual Explanations with hardware architecture breakdowns
 * - Syntax & Memory Addressing deep dives
 * - Interactive Code Snippet Viewer with line highlighting
 * - Live Step-by-Step Hardware Simulator (Registers + Memory Bus + Execution Log)
 * - Knowledge Check Quiz with instant verification and XP reward
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveLessonTutorialLayout(
    lesson: Lesson,
    viewModel: AssemblyViewModel,
    onBack: () -> Unit,
    onNextLesson: ((Lesson) -> Unit)? = null,
    onPreviousLesson: ((Lesson) -> Unit)? = null,
    onOpenInSandbox: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    val completedLessons by viewModel.completedLessons.collectAsState()
    val isCompleted = remember(completedLessons, lesson) {
        completedLessons.any { it.topicId == lesson.id || it.topicId == "x86_curriculum_${lesson.id}" }
    }

    // --- Interactive Simulator State for this Lesson ---
    val codeLines = remember(lesson) {
        lesson.codeSnippet.trimIndent().lines().filter { it.isNotBlank() }
    }

    fun makeInitialCpuState(): CpuState {
        val base = AssemblySimulator.createInitialState(Architecture.X86)
        val regMap = base.registers.toMutableMap()
        lesson.initialRegisters.forEach { (k, v) -> regMap[k] = v }
        val memMap = base.memory.toMutableMap()
        lesson.initialMemory.forEach { (k, v) -> memMap[k] = v }
        return base.copy(
            registers = regMap,
            memory = memMap,
            ip = 0,
            explanation = "Ready to step"
        )
    }

    var cpuState by remember(lesson) { mutableStateOf(makeInitialCpuState()) }
    val stepHistory = remember(lesson) { mutableStateListOf<CpuState>() }
    var highlightedRegs by remember(lesson) { mutableStateOf<Set<String>>(emptySet()) }
    var highlightedMemAddr by remember(lesson) { mutableStateOf<Int?>(null) }
    val executionLogs = remember(lesson) {
        mutableStateListOf("Initialized hardware state. Ready to step.")
    }

    // Quiz State
    var selectedQuizOption by remember(lesson) { mutableStateOf<Int?>(null) }
    var quizSubmitted by remember(lesson) { mutableStateOf(false) }
    var quizCorrect by remember(lesson) { mutableStateOf(false) }

    fun executeNextStep() {
        if (cpuState.ip >= codeLines.size) return
        val currentLine = codeLines[cpuState.ip].trim()
        val oldState = cpuState
        stepHistory.add(oldState)

        val newState = AssemblySimulator.executeStep(oldState, codeLines)
        
        // Find changed registers
        val changed = mutableSetOf<String>()
        newState.registers.forEach { (k, v) ->
            if (oldState.registers[k] != v) changed.add(k)
        }
        highlightedRegs = changed

        // Find changed memory
        var changedMem: Int? = null
        newState.memory.forEach { (k, v) ->
            if (oldState.memory[k] != v) changedMem = k
        }
        highlightedMemAddr = changedMem

        // Generate log entry
        val logDesc = when {
            changedMem != null -> "Cycle ${oldState.ip + 1}: Executed '$currentLine' -> Updated RAM [$changedMem] = ${newState.memory[changedMem]}"
            changed.isNotEmpty() -> "Cycle ${oldState.ip + 1}: Executed '$currentLine' -> ${changed.joinToString { "$it = ${newState.registers[it]}" }}"
            else -> "Cycle ${oldState.ip + 1}: Executed '$currentLine' (${newState.explanation})"
        }
        executionLogs.add(0, logDesc)
        cpuState = newState
    }

    fun stepBack() {
        if (stepHistory.isNotEmpty()) {
            val previous = stepHistory.removeAt(stepHistory.size - 1)
            cpuState = previous
            highlightedRegs = emptySet()
            highlightedMemAddr = null
            executionLogs.add(0, "Reverted back to Cycle ${previous.ip + 1}")
        }
    }

    fun resetSimulation() {
        cpuState = makeInitialCpuState()
        stepHistory.clear()
        highlightedRegs = emptySet()
        highlightedMemAddr = null
        executionLogs.clear()
        executionLogs.add("Simulation reset to initial state.")
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "x86 INTERACTIVE TUTORIAL",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = lesson.difficulty.uppercase(),
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = lesson.title,
                            fontSize = 12.sp,
                            color = PureWhite,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_tutorial_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonGreen
                        )
                    }
                },
                actions = {
                    if (isCompleted) {
                        Surface(
                            color = NeonGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonGreen),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "COMPLETED",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TerminalDarkGray)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Header Banner ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Lesson",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LESSON MODULE",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time",
                                tint = MutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${lesson.estimatedMinutes} min",
                                color = MutedText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = lesson.title,
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (lesson.subtitle.isNotBlank()) {
                        Text(
                            text = lesson.subtitle,
                            color = MutedText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Hardware Context Chips
                    if (lesson.registersInvolved.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Registers:",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            lesson.registersInvolved.forEach { reg ->
                                Surface(
                                    color = TerminalBlack,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = reg,
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Conceptual Explanation Section ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Theory",
                            tint = AlertOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CONCEPTUAL EXPLANATION & ARCHITECTURE",
                            color = AlertOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Parse formatted lines for Markdown style headings, lists, and callouts
                    val lines = lesson.conceptualExplanation.lines()
                    lines.forEach { line ->
                        when {
                            line.startsWith("# ") -> {
                                Text(
                                    text = line.removePrefix("# ").trim(),
                                    color = PureWhite,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("### ") -> {
                                Text(
                                    text = line.removePrefix("### ").trim(),
                                    color = CyberCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            line.startsWith("- ") -> {
                                Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                                    Text("▸ ", color = NeonGreen, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = line.removePrefix("- ").trim(),
                                        color = PureWhite,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            line.startsWith("> ") -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AlertOrange.copy(alpha = 0.12f))
                                        .border(BorderStroke(1.dp, AlertOrange.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = line.removePrefix("> ").trim(),
                                        color = AlertOrange,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            line.isNotBlank() -> {
                                Text(
                                    text = line.trim(),
                                    color = MutedText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Key Takeaways
                    if (lesson.keyTakeaways.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "KEY ARCHITECTURAL TAKEAWAYS",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        lesson.keyTakeaways.forEach { takeaway ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Check",
                                    tint = NeonGreen,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = takeaway,
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Memory Operations Explanation
                    if (lesson.memoryOperationsExplained.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = TerminalBlack,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Memory Bus",
                                    tint = CyberCyan,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "MEMORY BUS TRANSACTION",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = lesson.memoryOperationsExplained,
                                        color = MutedText,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Interactive Code Snippet & Live Hardware Simulation Lab ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Code",
                                tint = NeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INTERACTIVE CODE LAB & STEPPER",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (onOpenInSandbox != null) {
                            TextButton(
                                onClick = { onOpenInSandbox(lesson.codeSnippet) },
                                modifier = Modifier.testTag("btn_open_sandbox")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Sandbox",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sandbox",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Code viewer box with live active IP highlight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TerminalBlack)
                            .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            codeLines.forEachIndexed { index, line ->
                                val isCurrentInstruction = (index == cpuState.ip)
                                val isPastInstruction = (index < cpuState.ip)
                                val bg = when {
                                    isCurrentInstruction -> NeonGreen.copy(alpha = 0.18f)
                                    isPastInstruction -> Color.White.copy(alpha = 0.03f)
                                    else -> Color.Transparent
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bg)
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pointer indicator
                                    if (isCurrentInstruction) {
                                        Text(
                                            text = "▶",
                                            color = NeonGreen,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(16.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }

                                    // Line Number
                                    Text(
                                        text = "${index + 1}".padStart(2, ' '),
                                        color = if (isCurrentInstruction) NeonGreen else MutedText,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrentInstruction) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.width(26.dp)
                                    )

                                    // Instruction Content with styling
                                    val isComment = line.trim().startsWith(";")
                                    Text(
                                        text = line,
                                        color = when {
                                            isComment -> MutedText
                                            isCurrentInstruction -> PureWhite
                                            isPastInstruction -> PureWhite.copy(alpha = 0.8f)
                                            else -> PureWhite.copy(alpha = 0.6f)
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrentInstruction) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stepper Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { executeNextStep() },
                            enabled = cpuState.ip < codeLines.size,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = TerminalBlack,
                                disabledContainerColor = TerminalLightGray,
                                disabledContentColor = MutedText
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_step_next")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Step",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (cpuState.ip < codeLines.size) "Step Forward" else "Finished",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { stepBack() },
                            enabled = stepHistory.isNotEmpty(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("btn_step_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { resetSimulation() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertOrange),
                            border = BorderStroke(1.dp, AlertOrange.copy(alpha = 0.6f)),
                            modifier = Modifier.testTag("btn_reset_simulation")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Register Board
                    Text(
                        text = "LIVE REGISTERS",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val activeRegs = listOf("EAX", "EBX", "ECX", "EDX", "ESI", "ESP")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeRegs.take(3).forEach { reg ->
                            RegisterCell(
                                regName = reg,
                                value = cpuState.registers[reg] ?: 0,
                                isHighlighted = highlightedRegs.contains(reg),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        activeRegs.drop(3).forEach { reg ->
                            RegisterCell(
                                regName = reg,
                                value = cpuState.registers[reg] ?: 0,
                                isHighlighted = highlightedRegs.contains(reg),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Live Memory Table
                    if (cpuState.memory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "LIVE RAM INSPECTION",
                            color = AlertOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val memEntries = cpuState.memory.toList().sortedBy { it.first }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            memEntries.take(4).forEach { (addr, valAtAddr) ->
                                MemoryCell(
                                    address = addr,
                                    value = valAtAddr,
                                    isHighlighted = (highlightedMemAddr == addr),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Execution Console Log
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "EXECUTION LOG",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalBlack)
                            .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            executionLogs.forEachIndexed { i, log ->
                                Text(
                                    text = log,
                                    color = if (i == 0) NeonGreen else MutedText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Interactive Quiz Section ---
            lesson.quiz?.let { quiz ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, if (quizCorrect) NeonGreen else CyberCyan)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Quiz,
                                contentDescription = "Quiz",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KNOWLEDGE CHECKPOINT",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = quiz.question,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        quiz.options.forEachIndexed { index, option ->
                            val isSelected = (selectedQuizOption == index)
                            val isCorrectAnswer = (index == quiz.correctAnswerIndex)

                            val borderCol = when {
                                quizSubmitted && isCorrectAnswer -> NeonGreen
                                quizSubmitted && isSelected && !quizCorrect -> BrightPink
                                isSelected -> CyberCyan
                                else -> TerminalLightGray
                            }

                            val bgCol = when {
                                quizSubmitted && isCorrectAnswer -> NeonGreen.copy(alpha = 0.15f)
                                quizSubmitted && isSelected && !quizCorrect -> BrightPink.copy(alpha = 0.15f)
                                isSelected -> CyberCyan.copy(alpha = 0.1f)
                                else -> TerminalBlack
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bgCol)
                                    .border(BorderStroke(1.dp, borderCol), RoundedCornerShape(6.dp))
                                    .clickable(enabled = !quizSubmitted) {
                                        selectedQuizOption = index
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${('A' + index)}. ",
                                        color = if (isSelected) CyberCyan else MutedText,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = option,
                                        color = PureWhite,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!quizSubmitted) {
                            Button(
                                onClick = {
                                    if (selectedQuizOption != null) {
                                        quizSubmitted = true
                                        val correct = (selectedQuizOption == quiz.correctAnswerIndex)
                                        quizCorrect = correct
                                        if (correct) {
                                            viewModel.recordCurriculumCompletion(lesson.id, 100)
                                        }
                                    }
                                },
                                enabled = selectedQuizOption != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = TerminalBlack
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_submit_lesson_quiz")
                            ) {
                                Text(
                                    text = "Verify Answer",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            // Feedback card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (quizCorrect) NeonGreen.copy(alpha = 0.15f) else BrightPink.copy(alpha = 0.15f))
                                    .border(
                                        BorderStroke(1.dp, if (quizCorrect) NeonGreen else BrightPink),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (quizCorrect) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline,
                                            contentDescription = "Result",
                                            tint = if (quizCorrect) NeonGreen else BrightPink,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (quizCorrect) "EXCELLENT! +100 XP EARNED" else "INCORRECT",
                                            color = if (quizCorrect) NeonGreen else BrightPink,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = quiz.explanation,
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Bottom Navigation to Other Lessons ---
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onPreviousLesson != null) {
                    OutlinedButton(
                        onClick = {
                            val all = X86TutorialLessons.LESSONS
                            val currIdx = all.indexOfFirst { it.id == lesson.id }
                            if (currIdx > 0) onPreviousLesson(all[currIdx - 1])
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                        border = BorderStroke(1.dp, TerminalLightGray)
                    ) {
                        Text("← Prev Lesson", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (onNextLesson != null) {
                    Button(
                        onClick = {
                            val all = X86TutorialLessons.LESSONS
                            val currIdx = all.indexOfFirst { it.id == lesson.id }
                            if (currIdx >= 0 && currIdx < all.size - 1) onNextLesson(all[currIdx + 1])
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = TerminalBlack)
                    ) {
                        Text("Next Lesson →", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RegisterCell(
    regName: String,
    value: Int,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderCol = if (isHighlighted) NeonGreen else TerminalLightGray
    val bgCol = if (isHighlighted) NeonGreen.copy(alpha = 0.15f) else TerminalBlack

    Surface(
        color = bgCol,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = regName,
                color = if (isHighlighted) NeonGreen else MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$value",
                color = if (isHighlighted) NeonGreen else PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "0x${value.toString(16).uppercase()}",
                color = MutedText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MemoryCell(
    address: Int,
    value: Int,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val borderCol = if (isHighlighted) AlertOrange else TerminalLightGray
    val bgCol = if (isHighlighted) AlertOrange.copy(alpha = 0.18f) else TerminalBlack

    Surface(
        color = bgCol,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "[$address]",
                color = if (isHighlighted) AlertOrange else CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "$value",
                color = if (isHighlighted) AlertOrange else PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "0x${value.toString(16).uppercase()}",
                color = MutedText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
