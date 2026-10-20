package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Architecture
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel
import kotlinx.coroutines.launch

@Composable
fun PlaygroundScreen(
    viewModel: AssemblyViewModel,
    modifier: Modifier = Modifier
) {
    val currentArch by viewModel.selectedArchitecture.collectAsState()
    val playgroundCode by viewModel.playgroundCode.collectAsState()
    val cpuState by viewModel.cpuState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val breakpoints by viewModel.breakpoints.collectAsState()
    
    val aiLoading by viewModel.aiTutorLoading.collectAsState()
    val aiResponse by viewModel.aiTutorResponse.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var programTitle by remember { mutableStateOf("") }
    
    var editingRegisterName by remember { mutableStateOf<String?>(null) }
    var editingRegisterValue by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("registers") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- Header Section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .drawBehind {
                    drawLine(
                        color = CyberCyan,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = currentArch.displayName,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VM COMPILER DEBUGGER",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // File operations
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            programTitle = "Prog_${currentArch.codeName}_" + (System.currentTimeMillis() % 1000)
                            showSaveDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Source",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetCpu(currentArch) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset State",
                            tint = BrightPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // --- Split Screen Layout ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            
            // PANEL 1: Interactive Code Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(TerminalBlack)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line number Gutter on the left
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(44.dp)
                            .background(TerminalDarkGray)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val totalLinesCount = playgroundCode.lines().size.coerceAtLeast(1)
                        for (i in 0 until totalLinesCount) {
                            val isActiveLine = i == cpuState.ip
                            val hasBreakpoint = breakpoints.contains(i)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .clickable { viewModel.toggleBreakpoint(i) },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasBreakpoint) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(BrightPink)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = (i + 1).toString(),
                                    color = if (isActiveLine) NeonGreen else if (hasBreakpoint) BrightPink else MutedText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActiveLine || hasBreakpoint) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Actual code content textfield with live pointer highlighting
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                    ) {
                        // Drawing line pointer background highlights in real time
                        Column(modifier = Modifier.fillMaxSize()) {
                            playgroundCode.lines().forEachIndexed { idx, _ ->
                                val isActiveLine = idx == cpuState.ip
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .background(
                                            if (isActiveLine) NeonGreen.copy(alpha = 0.15f) else Color.Transparent
                                        )
                                )
                            }
                        }

                        // Code Editor Input
                        BasicTextField(
                            value = playgroundCode,
                            onValueChange = { viewModel.updatePlaygroundCode(it) },
                            textStyle = TextStyle(
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent),
                            cursorBrush = SolidColor(NeonGreen)
                        )
                    }
                }
            }

            // PANEL 2: Action Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray)
                    .drawBehind {
                        drawLine(
                            color = TerminalLightGray,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2f
                        )
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⏮ Reset
                TextButton(onClick = { viewModel.resetCpu() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Reset", tint = BrightPink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESET", color = BrightPink, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // ◀ Backward
                TextButton(onClick = { viewModel.stepBackward() }) {
                    Icon(Icons.Default.Undo, contentDescription = "Back", tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UNDO", color = CyberCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // ▶ Auto Loop Run
                Button(
                    onClick = { viewModel.togglePlay() },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) BrightPink else NeonGreen),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = TerminalBlack,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPlaying) "PAUSE" else "RUN",
                        color = TerminalBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // ⏭ Step Forward
                TextButton(onClick = { viewModel.stepForward() }, enabled = !isPlaying) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Step", tint = NeonGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("STEP", color = if (isPlaying) MutedText else NeonGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                // 🤖 Ask AI
                Button(
                    onClick = { viewModel.askAiToExplain() },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalLightGray),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.SmartToy, contentDescription = "AI", tint = CyberCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI TUTOR", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // PANEL 3: Running Step Explanation Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Console Log",
                        tint = CyberCyan,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CPU BUS CONTROLLER LOG:",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cpuState.explanation,
                            color = PureWhite,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // PANEL 4: State Inspectors Tabs Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray)
            ) {
                val tabs = mutableListOf("registers", "memory", "stack")
                if (currentArch == Architecture.GPU_DEV) tabs.add("canvas")
                if (currentArch == Architecture.OS_DEV) tabs.add("terminal")
                
                tabs.forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = tab }
                            .drawBehind {
                                if (isSelected) {
                                    drawLine(
                                        color = if (tab == "canvas") BrightPink else if (tab == "terminal") AlertOrange else if (tab == "memory") NeonGreen else CyberCyan,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 6f
                                    )
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.uppercase(),
                            color = if (isSelected) PureWhite else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // State tabs rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp)
                    .background(DarkBackground)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    "registers" -> {
                        // Registers Grid View
                        val regsList = cpuState.registers.toList()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            items(regsList) { (regName, regVal) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(TerminalDarkGray)
                                        .border(1.dp, TerminalLightGray, RoundedCornerShape(6.dp))
                                        .clickable {
                                            editingRegisterName = regName
                                            editingRegisterValue = regVal.toString()
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = regName,
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = regVal.toString(),
                                            color = PureWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        // Hex & Binary
                                        val hexStr = "0x" + regVal.toString(16).uppercase()
                                        val binStr = regVal.toString(2).padStart(8, '0')
                                        Text(
                                            text = "HEX: $hexStr  BIN: $binStr",
                                            color = MutedText,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    "memory" -> {
                        var memAddressToSet by remember { mutableStateOf("") }
                        var memValueToSet by remember { mutableStateOf("") }
                        
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SILICON RAM PHYSICAL STORAGE",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Form to write address
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = memAddressToSet,
                                    onValueChange = { memAddressToSet = it },
                                    label = { Text("Addr (e.g., 100)", fontSize = 10.sp, color = MutedText) },
                                    textStyle = TextStyle(color = PureWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = TerminalLightGray
                                    )
                                )
                                OutlinedTextField(
                                    value = memValueToSet,
                                    onValueChange = { memValueToSet = it },
                                    label = { Text("Value (e.g., 42)", fontSize = 10.sp, color = MutedText) },
                                    textStyle = TextStyle(color = PureWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = TerminalLightGray
                                    )
                                )
                                Button(
                                    onClick = {
                                        val addr = memAddressToSet.toIntOrNull() ?: 0
                                        val value = memValueToSet.toIntOrNull() ?: 0
                                        viewModel.updateMemoryValue(addr, value)
                                        memAddressToSet = ""
                                        memValueToSet = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("WRITE", color = TerminalBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Memory dump
                            if (cpuState.memory.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("RAM is currently unallocated.", color = MutedText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                ) {
                                    items(cpuState.memory.toList().sortedBy { it.first }) { (addr, valInMem) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(TerminalDarkGray)
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "ADDR: $addr (0x${addr.toString(16).uppercase()})",
                                                color = CyberCyan,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "VAL: $valInMem (0x${valInMem.toString(16).uppercase()})  BIN: ${valInMem.toString(2).padStart(8, '0')}",
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    "stack" -> {
                        // Call stack visualization
                        if (cpuState.stack.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.LayersClear, "Stack Empty", tint = MutedText, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Memory Call Stack Empty", color = MutedText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    Text("Push registers onto the stack to inspect frames", color = MutedText, fontSize = 10.sp)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                reverseLayout = true
                            ) {
                                items(cpuState.stack.asReversed()) { valOnStack ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(TerminalDarkGray)
                                            .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "[SP + OFFSET]",
                                                color = CyberCyan,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "VAL: $valOnStack (0x${valOnStack.toString(16).uppercase()})",
                                                color = PureWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "canvas" -> {
                        // GPU RENDER CANVAS
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "SIMD SHADER FRAMEBUFFER (8x8)",
                                color = BrightPink,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // 8x8 pixels grid representation
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(8),
                                modifier = Modifier
                                    .size(160.dp)
                                    .background(TerminalBlack)
                                    .border(2.dp, TerminalLightGray)
                            ) {
                                items(cpuState.gpuPixels) { pixelColor ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .background(Color(pixelColor))
                                            .border(0.5.dp, Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                            
                            if (cpuState.gpuActiveOutput) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "✔ GPU SIMD Lanes rendering gradient loop.",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    "terminal" -> {
                        // OS MOCK BIOS TERMINAL
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "BIOS SERIAL MONITOR TEXT BUFFER (INT 0x10)",
                                color = AlertOrange,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black)
                                    .border(2.dp, TerminalLightGray)
                                    .padding(12.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(cpuState.biosTerminal) { line ->
                                        Text(
                                            text = "> $line",
                                            color = Color.Green,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                            
                            if (cpuState.cr0ProtectedBit) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AlertOrange.copy(alpha = 0.15f))
                                        .border(1.dp, AlertOrange, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "CPU ALERT: Entered 32-bit Protected Mode. Segment paging registers enabled.",
                                        color = AlertOrange,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
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

    // --- AI TUTOR CONSOLE SHEET overlay ---
    if (aiLoading || aiResponse != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable { if (!aiLoading) viewModel.clearAiResponse() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .clickable(enabled = false) {}, // Prevent dismiss click inside card
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = BorderStroke(2.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, "AI Tutor", tint = CyberCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PROF. GEMINI: ASSEMBLY INSIGHTS",
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        if (!aiLoading) {
                            IconButton(onClick = { viewModel.clearAiResponse() }) {
                                Icon(Icons.Default.Close, "Close", tint = PureWhite)
                            }
                        }
                    }

                    HorizontalDivider(color = TerminalLightGray, modifier = Modifier.padding(vertical = 12.dp))

                    if (aiLoading) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "ANALYZING REGISTERS & BUS CYCLES...",
                                    color = CyberCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Calling server-side Gemini tutor",
                                    color = MutedText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else if (aiResponse != null) {
                        // Scrollable response text
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = aiResponse!!,
                                color = PureWhite,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Save File Dialog ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    "SAVE SOURCE PROGRAM",
                    color = CyberCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        "Store this program to your local custom library. Access and compile it anytime in your Profile tab.",
                        color = MutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = programTitle,
                        onValueChange = { programTitle = it },
                        label = { Text("Program Title", color = MutedText) },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalLightGray,
                            cursorColor = NeonGreen
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveProgram(programTitle)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("SAVE TO LIBRARY", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = BrightPink)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    // --- Modify Register Dialog ---
    if (editingRegisterName != null) {
        AlertDialog(
            onDismissRequest = { editingRegisterName = null },
            title = {
                Text(
                    "MODIFY REGISTER: ${editingRegisterName}",
                    color = CyberCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        "Input a new integer value (supports decimal or hex prefix '0x'):",
                        color = MutedText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = editingRegisterValue,
                        onValueChange = { editingRegisterValue = it },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = TerminalLightGray,
                            cursorColor = CyberCyan
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedVal = if (editingRegisterValue.lowercase().startsWith("0x")) {
                            editingRegisterValue.substring(2).toIntOrNull(16) ?: 0
                        } else {
                            editingRegisterValue.toIntOrNull() ?: 0
                        }
                        viewModel.updateRegisterValue(editingRegisterName!!, parsedVal)
                        editingRegisterName = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("WRITE REGISTER", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRegisterName = null }) {
                    Text("CANCEL", color = BrightPink)
                }
            },
            containerColor = TerminalDarkGray
        )
    }
}
