package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.X86CurriculumRepository
import com.example.model.X86CurriculumTopic
import com.example.model.X86CurriculumUnit
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

/**
 * Structured Curriculum UI Component for teaching basic x86 Assembly instructions
 * including general-purpose registers and memory operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun X86CurriculumComponent(
    viewModel: AssemblyViewModel,
    onBack: () -> Unit,
    onLaunchSandboxWithCode: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBack()
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var viewingTopic by remember { mutableStateOf<X86CurriculumTopic?>(null) }
    
    val completedLessons by viewModel.completedLessons.collectAsState()
    val allTopics = remember { X86CurriculumRepository.getAllTopics() }
    
    val completedCount = remember(completedLessons) {
        allTopics.count { topic ->
            completedLessons.any { it.topicId == "x86_curriculum_${topic.id}" || it.topicId == topic.id }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "x86 CORE CURRICULUM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = PureWhite
                        )
                        Text(
                            text = "Registers & Memory Operations",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("curriculum_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonGreen
                        )
                    }
                },
                actions = {
                    // Mastery Badge
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
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
                                text = "$completedCount / ${allTopics.size}",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
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
        ) {
            // --- Navigation Tabs ---
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = TerminalDarkGray,
                contentColor = PureWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("LESSONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.School, contentDescription = "Lessons", modifier = Modifier.size(18.dp)) },
                    selectedContentColor = NeonGreen,
                    unselectedContentColor = MutedText,
                    modifier = Modifier.testTag("tab_curriculum_lessons")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("REGISTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.Memory, contentDescription = "Registers", modifier = Modifier.size(18.dp)) },
                    selectedContentColor = CyberCyan,
                    unselectedContentColor = MutedText,
                    modifier = Modifier.testTag("tab_curriculum_registers")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("MEMORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "Memory", modifier = Modifier.size(18.dp)) },
                    selectedContentColor = AlertOrange,
                    unselectedContentColor = MutedText,
                    modifier = Modifier.testTag("tab_curriculum_memory")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("DRILLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Drills", modifier = Modifier.size(18.dp)) },
                    selectedContentColor = BrightPink,
                    unselectedContentColor = MutedText,
                    modifier = Modifier.testTag("tab_curriculum_drills")
                )
            }

            // --- Tab Contents ---
            when (selectedTabIndex) {
                0 -> {
                    CurriculumLessonsView(
                        completedLessons = completedLessons.map { it.topicId },
                        onTopicSelected = { topic -> viewingTopic = topic }
                    )
                }
                1 -> {
                    X86RegisterBitExplorer()
                }
                2 -> {
                    X86MemoryAddressingLab()
                }
                3 -> {
                    X86InstructionWorkbench()
                }
            }
        }
    }

    // --- Topic Detail Modal ---
    viewingTopic?.let { topic ->
        val topicKey = "x86_curriculum_${topic.id}"
        val isCompleted = completedLessons.any { it.topicId == topicKey || it.topicId == topic.id }

        X86TopicDetailModal(
            topic = topic,
            isCompleted = isCompleted,
            onDismiss = { viewingTopic = null },
            onQuizCompleted = {
                viewModel.recordCurriculumCompletion(topicKey, 100)
            },
            onRunInDebugger = { code ->
                viewingTopic = null
                if (onLaunchSandboxWithCode != null) {
                    onLaunchSandboxWithCode(code)
                } else {
                    viewModel.updatePlaygroundCode(code)
                    viewModel.resetCpu(com.example.model.Architecture.X86)
                }
            }
        )
    }
}

@Composable
private fun CurriculumLessonsView(
    completedLessons: List<String>,
    onTopicSelected: (X86CurriculumTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    val units = remember { X86CurriculumRepository.CURRICULUM_UNITS }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Curriculum Banner
        item {
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
                        Surface(
                            color = NeonGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "STRUCTURED SYLLABUS",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "4 Modules • 12 Topics",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Foundation of x86-64 Machine Code",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Step through the CPU register file, memory bus dereferencing, the SIB formula [Base + Index * Scale + Disp], and in-place ALU operations.",
                        color = MutedText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Units and Topics
        items(units) { unit ->
            UnitCard(
                unit = unit,
                completedLessons = completedLessons,
                onTopicSelected = onTopicSelected
            )
        }
    }
}

@Composable
private fun UnitCard(
    unit: X86CurriculumUnit,
    completedLessons: List<String>,
    onTopicSelected: (X86CurriculumTopic) -> Unit
) {
    var isExpanded by remember { mutableStateOf(unit.id == 1) }

    val completedInUnit = remember(completedLessons, unit) {
        unit.topics.count { topic ->
            completedLessons.contains("x86_curriculum_${topic.id}") || completedLessons.contains(topic.id)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, if (isExpanded) CyberCyan.copy(alpha = 0.5f) else TerminalLightGray)
    ) {
        Column {
            // Unit Header
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (completedInUnit == unit.topics.size) NeonGreen.copy(alpha = 0.2f) else TerminalBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unit.id.toString(),
                            color = if (completedInUnit == unit.topics.size) NeonGreen else CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = unit.title,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$completedInUnit / ${unit.topics.size} Topics Mastered",
                            color = if (completedInUnit == unit.topics.size) NeonGreen else MutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle",
                        tint = MutedText
                    )
                }
            }

            // Topics list
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalBlack.copy(alpha = 0.6f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = TerminalLightGray, modifier = Modifier.padding(bottom = 6.dp))

                    unit.topics.forEach { topic ->
                        val isCompleted = completedLessons.contains("x86_curriculum_${topic.id}") || completedLessons.contains(topic.id)

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
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Status",
                                    tint = if (isCompleted) NeonGreen else MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${unit.id}.${topic.topicNumber}: ${topic.title}",
                                        color = if (isCompleted) NeonGreen else PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = topic.subtitle,
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Surface(
                                color = TerminalDarkGray,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, TerminalLightGray)
                            ) {
                                Text(
                                    text = topic.opcodeTag,
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun X86TopicDetailModal(
    topic: X86CurriculumTopic,
    isCompleted: Boolean,
    onDismiss: () -> Unit,
    onQuizCompleted: () -> Unit,
    onRunInDebugger: (String) -> Unit
) {
    var selectedQuizOption by remember { mutableStateOf<Int?>(null) }
    var quizSubmitted by remember { mutableStateOf(isCompleted) }
    var quizCorrect by remember { mutableStateOf(isCompleted) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Top Dismiss Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = topic.opcodeTag,
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unit ${topic.unitId} • Topic ${topic.topicNumber}",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_topic_modal")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = topic.title,
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = topic.subtitle,
                    color = CyberCyan,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Key Concept Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Concept",
                            tint = NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = topic.keyConcept,
                            color = PureWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lesson Content Body
                Text(
                    text = "ARCHITECTURAL DETAILS",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, TerminalLightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        topic.overviewMarkdown.lines().forEach { line ->
                            when {
                                line.startsWith("#") -> {
                                    Text(
                                        text = line.replace("#", "").trim(),
                                        color = NeonGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }
                                line.startsWith("###") -> {
                                    Text(
                                        text = line.replace("###", "").trim(),
                                        color = CyberCyan,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                line.startsWith("-") -> {
                                    Row(modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 2.dp)) {
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
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Syntax Rule Card if provided
                topic.syntaxRule?.let { rule ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SYNTAX & OPERAND CONSTRAINTS",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TerminalBlack),
                        border = BorderStroke(1.dp, CyberCyan)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Format: ${rule.syntax}",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Destination: ${rule.destination}", color = PureWhite, fontSize = 12.sp)
                            Text("Source: ${rule.source}", color = PureWhite, fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Valid Operands:", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            rule.allowedOperands.forEach {
                                Text("  ✓ $it", color = PureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Forbidden Operands:", color = BrightPink, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            rule.forbiddenOperands.forEach {
                                Text("  ✗ $it", color = BrightPink, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Code Example
                Text(
                    text = "ASSEMBLY CODE TEMPLATE",
                    color = MutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalBlack),
                    border = BorderStroke(1.dp, TerminalLightGray)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = topic.codeExample,
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onRunInDebugger(topic.codeExample) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = TerminalBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_run_in_debugger")
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "Debug", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run In Hardware Debugger", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Common Pitfalls Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, AlertOrange.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = AlertOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("COMMON PITFALL", color = AlertOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(topic.commonMistakes, color = PureWhite, fontSize = 12.sp, lineHeight = 16.sp)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Best Practice: ${topic.bestPractices}", color = MutedText, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Knowledge Check Quiz
                Text(
                    text = "KNOWLEDGE CHECK QUIZ (+100 XP)",
                    color = NeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, TerminalLightGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = topic.quizQuestion,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        topic.quizOptions.forEachIndexed { index, option ->
                            val isSelected = selectedQuizOption == index
                            val isCorrectAnswer = index == topic.quizCorrectAnswerIndex

                            val borderCol = when {
                                quizSubmitted && isCorrectAnswer -> NeonGreen
                                quizSubmitted && isSelected && !isCorrectAnswer -> BrightPink
                                isSelected -> CyberCyan
                                else -> TerminalLightGray
                            }

                            val bgCol = when {
                                quizSubmitted && isCorrectAnswer -> NeonGreen.copy(alpha = 0.15f)
                                quizSubmitted && isSelected && !isCorrectAnswer -> BrightPink.copy(alpha = 0.15f)
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
                                        val correct = selectedQuizOption == topic.quizCorrectAnswerIndex
                                        quizCorrect = correct
                                        if (correct) {
                                            onQuizCompleted()
                                        }
                                    }
                                },
                                enabled = selectedQuizOption != null,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TerminalBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_submit_quiz")
                            ) {
                                Text("Submit Answer", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            // Feedback
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (quizCorrect) NeonGreen.copy(alpha = 0.15f) else BrightPink.copy(alpha = 0.15f))
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
                                            text = if (quizCorrect) "CORRECT! +100 XP AWARDED" else "INCORRECT",
                                            color = if (quizCorrect) NeonGreen else BrightPink,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = topic.quizExplanation,
                                        color = PureWhite,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
