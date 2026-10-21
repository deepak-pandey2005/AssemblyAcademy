package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simulator.X86RegisterSnapshot
import com.example.simulator.X86RegisterStateManager
import com.example.simulator.X86ScenarioPreset
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

enum class DashboardDisplayFormat(val label: String) {
    HEX("HEX"),
    DEC_SIGNED("DEC (S)"),
    DEC_UNSIGNED("DEC (U)"),
    BINARY("BINARY"),
    ASCII("ASCII")
}

enum class DashboardCategory(val label: String) {
    ALL("ALL (8)"),
    GENERAL("GENERAL (4)"),
    POINTERS("POINTERS (4)"),
    SYSTEM("SYSTEM & FLAGS")
}

/**
 * Real-Time Reactive Dashboard for simulating and observing x86 CPU registers.
 * Features live reactive state streams, sub-register bit-slicing inspection (AX, AH, AL),
 * an interactive 32-bit transistor matrix, live EFLAGS indicators, micro-instruction dispatch,
 * undo/redo time travel, and machine scenario presets.
 */
@Composable
fun X86ReactiveDashboardScreen(
    viewModel: AssemblyViewModel,
    onNavigateBack: () -> Unit,
    onLaunchInDebugger: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stateManager = viewModel.x86StateManager
    val snapshot by stateManager.state.collectAsState()
    val modifiedRegisters by stateManager.modifiedRegisters.collectAsState()
    val history by stateManager.history.collectAsState()
    val canUndo by stateManager.canUndo.collectAsState()
    val canRedo by stateManager.canRedo.collectAsState()
    val watchpointAlert by stateManager.watchpointAlert.collectAsState()

    var selectedFormat by remember { mutableStateOf(DashboardDisplayFormat.HEX) }
    var selectedCategory by remember { mutableStateOf(DashboardCategory.ALL) }
    var selectedRegisterForMatrix by remember { mutableStateOf("EAX") }
    var expandedRegisterName by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var editingRegisterName by remember { mutableStateOf<String?>(null) }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    // Custom instruction input
    var customInstructionText by remember { mutableStateOf("") }

    // Pulsing animation for modified registers
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("x86_reactive_dashboard_screen")
    ) {
        // --- 1. TOP HEADER APP BAR ---
        Surface(
            color = TerminalDarkGray,
            border = BorderStroke(1.dp, TerminalLightGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PureWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "x86 CPU REGISTER DASHBOARD",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = "CYCLE #${snapshot.cycleCount} • CENTRAL REACTIVE STATE",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top Action Icons (Undo, Redo, Presets, History, Reset)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { stateManager.undo() },
                            enabled = canUndo,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) PureWhite else MutedText.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { stateManager.redo() },
                            enabled = canRedo,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) PureWhite else MutedText.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showPresetDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Scenarios",
                                tint = AlertOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { showHistoryDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Audit Log",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { stateManager.reset() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset CPU",
                                tint = BrightPink,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Watchpoint Alert Banner (if any condition triggered)
                watchpointAlert?.let { alertMsg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(AlertOrange.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, AlertOrange), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AlertOrange, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(alertMsg, color = PureWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { stateManager.dismissWatchpointAlert() }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = PureWhite, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- SCROLLABLE DASHBOARD BODY ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // --- 2. EFLAGS REACTIVE STATUS RIBBON ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EFLAGS STATUS REGISTER",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "0x" + snapshot.eflags.toUInt().toString(16).uppercase().padStart(8, '0'),
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // EFLAGS interactive bits row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FlagIndicatorChip("ZF", snapshot.zf, "Zero", pulseAlpha) { stateManager.toggleFlag("ZF") }
                        FlagIndicatorChip("CF", snapshot.cf, "Carry", pulseAlpha) { stateManager.toggleFlag("CF") }
                        FlagIndicatorChip("SF", snapshot.sf, "Sign", pulseAlpha) { stateManager.toggleFlag("SF") }
                        FlagIndicatorChip("OF", snapshot.of, "Overflow", pulseAlpha) { stateManager.toggleFlag("OF") }
                        FlagIndicatorChip("PF", snapshot.pf, "Parity", pulseAlpha) { stateManager.toggleFlag("PF") }
                        FlagIndicatorChip("AF", snapshot.af, "Aux", pulseAlpha) { stateManager.toggleFlag("AF") }
                        FlagIndicatorChip("IF", snapshot.ifFlag, "Interrupt", pulseAlpha) { stateManager.toggleFlag("IF") }
                        FlagIndicatorChip("DF", snapshot.df, "Direction", pulseAlpha) { stateManager.toggleFlag("DF") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 3. FORMAT SELECTOR & CATEGORY FILTER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(DashboardCategory.values()) { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else TerminalDarkGray)
                                .border(BorderStroke(1.dp, if (isSel) CyberCyan else TerminalLightGray), RoundedCornerShape(4.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = cat.label,
                                color = if (isSel) CyberCyan else MutedText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Format Selector dropdown / switch
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(DashboardDisplayFormat.HEX, DashboardDisplayFormat.DEC_SIGNED, DashboardDisplayFormat.BINARY).forEach { fmt ->
                        val isSel = selectedFormat == fmt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) NeonGreen.copy(alpha = 0.15f) else TerminalBlack)
                                .border(BorderStroke(1.dp, if (isSel) NeonGreen else TerminalLightGray), RoundedCornerShape(4.dp))
                                .clickable { selectedFormat = fmt }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = fmt.label,
                                color = if (isSel) NeonGreen else MutedText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- 4. CORE REGISTER CARDS (Reactive Grid) ---
            val registerList = remember(selectedCategory, snapshot) {
                when (selectedCategory) {
                    DashboardCategory.ALL -> listOf(
                        Triple("EAX", snapshot.eax, "Accumulator / Arithmetic / Return Value"),
                        Triple("EBX", snapshot.ebx, "Base Register / Pointer Offset"),
                        Triple("ECX", snapshot.ecx, "Counter / Loop Index / Shift Count"),
                        Triple("EDX", snapshot.edx, "Data Register / High Multiplier / I/O"),
                        Triple("ESI", snapshot.esi, "Source Index / String Pointer"),
                        Triple("EDI", snapshot.edi, "Destination Index / Buffer Pointer"),
                        Triple("ESP", snapshot.esp, "Stack Pointer [Top of Call Stack]"),
                        Triple("EBP", snapshot.ebp, "Base Pointer [Stack Frame Base]")
                    )
                    DashboardCategory.GENERAL -> listOf(
                        Triple("EAX", snapshot.eax, "Accumulator / Arithmetic / Return Value"),
                        Triple("EBX", snapshot.ebx, "Base Register / Pointer Offset"),
                        Triple("ECX", snapshot.ecx, "Counter / Loop Index / Shift Count"),
                        Triple("EDX", snapshot.edx, "Data Register / High Multiplier / I/O")
                    )
                    DashboardCategory.POINTERS -> listOf(
                        Triple("ESI", snapshot.esi, "Source Index / String Pointer"),
                        Triple("EDI", snapshot.edi, "Destination Index / Buffer Pointer"),
                        Triple("ESP", snapshot.esp, "Stack Pointer [Top of Call Stack]"),
                        Triple("EBP", snapshot.ebp, "Base Pointer [Stack Frame Base]")
                    )
                    DashboardCategory.SYSTEM -> listOf(
                        Triple("EIP", snapshot.eip, "Instruction Pointer [Program Counter]"),
                        Triple("CS", snapshot.cs, "Code Segment Selector (0x0010)"),
                        Triple("DS", snapshot.ds, "Data Segment Selector (0x0018)"),
                        Triple("SS", snapshot.ss, "Stack Segment Selector (0x0018)"),
                        Triple("CR0", snapshot.cr0, "Control Register 0 (Protected Mode/Paging)")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                registerList.forEach { (regName, regValue, roleDesc) ->
                    val isModified = modifiedRegisters.contains(regName)
                    val isFocusedForMatrix = selectedRegisterForMatrix == regName
                    val isExpanded = expandedRegisterName == regName

                    ReactiveRegisterCard(
                        regName = regName,
                        regValue = regValue,
                        roleDescription = roleDesc,
                        displayFormat = selectedFormat,
                        isModified = isModified,
                        isSelectedForMatrix = isFocusedForMatrix,
                        isExpanded = isExpanded,
                        pulseAlpha = pulseAlpha,
                        snapshot = snapshot,
                        onSelectForMatrix = { selectedRegisterForMatrix = regName },
                        onToggleExpand = {
                            expandedRegisterName = if (isExpanded) null else regName
                        },
                        onQuickSet = { delta ->
                            stateManager.setRegister(regName, regValue + delta, "Quick increment $delta")
                            viewModel.syncFromX86StateManager()
                        },
                        onZero = {
                            stateManager.setRegister(regName, 0, "Zeroed $regName")
                            viewModel.syncFromX86StateManager()
                        },
                        onNot = {
                            stateManager.setRegister(regName, regValue.inv(), "Bitwise NOT $regName")
                            viewModel.syncFromX86StateManager()
                        },
                        onEdit = { editingRegisterName = regName },
                        onSubRegisterEdit = { subName, subVal ->
                            stateManager.setRegister(subName, subVal, "Sub-register $subName update")
                            viewModel.syncFromX86StateManager()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 5. 32-BIT TRANSISTOR MATRIX (Interactive Bit Flipper) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "32-BIT TRANSISTOR MATRIX ($selectedRegisterForMatrix)",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "TAP BIT TO FLIP (0 <-> 1)",
                            color = MutedText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val targetValue = snapshot.getRegisterValue(selectedRegisterForMatrix)

                    // 32 bits arranged in 4 bytes (Byte 3, Byte 2, Byte 1, Byte 0)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (byteIdx in 3 downTo 0) {
                            val byteStartBit = byteIdx * 8 + 7
                            val byteEndBit = byteIdx * 8
                            val byteVal = (targetValue ushr (byteIdx * 8)) and 0xFF
                            val byteLabel = when (selectedRegisterForMatrix) {
                                "EAX" -> if (byteIdx == 1) "AH" else if (byteIdx == 0) "AL" else "Byte $byteIdx"
                                "EBX" -> if (byteIdx == 1) "BH" else if (byteIdx == 0) "BL" else "Byte $byteIdx"
                                "ECX" -> if (byteIdx == 1) "CH" else if (byteIdx == 0) "CL" else "Byte $byteIdx"
                                "EDX" -> if (byteIdx == 1) "DH" else if (byteIdx == 0) "DL" else "Byte $byteIdx"
                                else -> "Byte $byteIdx"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalBlack)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$byteLabel [Bits $byteStartBit..$byteEndBit] = 0x${byteVal.toString(16).uppercase().padStart(2, '0')}",
                                    color = if (byteIdx == 0) NeonGreen else if (byteIdx == 1) CyberCyan else PureWhite,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(150.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    for (bit in byteStartBit downTo byteEndBit) {
                                        val isBitSet = (targetValue and (1 shl bit)) != 0
                                        val isSignBit = bit == 31
                                        val bitColor = when {
                                            isSignBit && isBitSet -> BrightPink
                                            isBitSet -> NeonGreen
                                            else -> MutedText.copy(alpha = 0.35f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isBitSet) bitColor.copy(alpha = 0.25f) else TerminalDarkGray)
                                                .border(BorderStroke(1.dp, bitColor), RoundedCornerShape(3.dp))
                                                .clickable {
                                                    stateManager.toggleRegisterBit(selectedRegisterForMatrix, bit)
                                                    viewModel.syncFromX86StateManager()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isBitSet) "1" else "0",
                                                color = if (isBitSet) bitColor else MutedText,
                                                fontSize = 9.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // --- 6. MICRO-INSTRUCTION EXECUTION CONSOLE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = AlertOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MICRO-INSTRUCTION TEST BENCH",
                            color = AlertOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1-tap quick operations for selected register
                    val reg = selectedRegisterForMatrix
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item { MicroOpChip("INC $reg") { stateManager.executeMicroInstruction("INC $reg"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("DEC $reg") { stateManager.executeMicroInstruction("DEC $reg"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("ADD $reg, 10") { stateManager.executeMicroInstruction("ADD $reg, 10"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("SUB $reg, 5") { stateManager.executeMicroInstruction("SUB $reg, 5"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("XOR $reg, $reg") { stateManager.executeMicroInstruction("XOR $reg, $reg"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("SHL $reg, 1") { stateManager.executeMicroInstruction("SHL $reg, 1"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("SHR $reg, 1") { stateManager.executeMicroInstruction("SHR $reg, 1"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("NOT $reg") { stateManager.executeMicroInstruction("NOT $reg"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("NEG $reg") { stateManager.executeMicroInstruction("NEG $reg"); viewModel.syncFromX86StateManager() } }
                        item { MicroOpChip("XCHG EAX, EBX") { stateManager.executeMicroInstruction("XCHG EAX, EBX"); viewModel.syncFromX86StateManager() } }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Instruction input line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInstructionText,
                            onValueChange = { customInstructionText = it },
                            placeholder = { Text("e.g. MOV EAX, 0x1234 or ADD ECX, 5", color = MutedText, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            textStyle = TextStyle(color = PureWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (customInstructionText.isNotBlank()) {
                                    stateManager.executeMicroInstruction(customInstructionText)
                                    viewModel.syncFromX86StateManager()
                                    customInstructionText = ""
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AlertOrange,
                                unfocusedBorderColor = TerminalLightGray
                            )
                        )

                        Button(
                            onClick = {
                                if (customInstructionText.isNotBlank()) {
                                    stateManager.executeMicroInstruction(customInstructionText)
                                    viewModel.syncFromX86StateManager()
                                    customInstructionText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertOrange),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("EXECUTE", color = TerminalBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LAST ACTION: " + snapshot.lastAction,
                        color = MutedText,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 7. LINK TO DEBUGGER & EXTERNAL SIMULATOR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val code = "; Loaded from Central Register State\n" +
                                "MOV EAX, 0x${snapshot.eax.toString(16).uppercase()}\n" +
                                "MOV EBX, 0x${snapshot.ebx.toString(16).uppercase()}\n" +
                                "MOV ECX, 0x${snapshot.ecx.toString(16).uppercase()}\n" +
                                "MOV EDX, 0x${snapshot.edx.toString(16).uppercase()}\n" +
                                "ADD EAX, EBX\n" +
                                "INC ECX\n"
                        if (onLaunchInDebugger != null) {
                            onLaunchInDebugger(code)
                        } else {
                            viewModel.updatePlaygroundCode(code)
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TerminalBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LAUNCH IN DEBUGGER", color = TerminalBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // --- EDIT REGISTER MODAL ---
    editingRegisterName?.let { regToEdit ->
        val currentVal = snapshot.getRegisterValue(regToEdit)
        var inputStr by remember { mutableStateOf(currentVal.toString()) }
        var isHexMode by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { editingRegisterName = null },
            title = {
                Text(
                    text = "Edit Register $regToEdit",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter new 32-bit value in Decimal or Hex (0x...):",
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inputStr,
                        onValueChange = { inputStr = it },
                        label = { Text(if (isHexMode) "Hex (e.g. 0x2A or 100)" else "Decimal", color = MutedText) },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = TerminalLightGray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isHexMode, onCheckedChange = { isHexMode = it })
                        Text("Parse as Hex (0x)", color = PureWhite, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = if (isHexMode || inputStr.startsWith("0x", ignoreCase = true)) {
                            inputStr.replace("0x", "").toLongOrNull(16)?.toInt()
                        } else {
                            inputStr.toLongOrNull()?.toInt()
                        }
                        if (parsed != null) {
                            stateManager.setRegister(regToEdit, parsed, "User edit via dashboard")
                            viewModel.syncFromX86StateManager()
                        }
                        editingRegisterName = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("APPLY", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRegisterName = null }) {
                    Text("CANCEL", color = MutedText)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    // --- SCENARIO PRESETS DIALOG ---
    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AlertOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Machine Scenarios",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    X86ScenarioPreset.values().forEach { preset ->
                        Surface(
                            color = TerminalBlack,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, TerminalLightGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    stateManager.applyPreset(preset)
                                    viewModel.syncFromX86StateManager()
                                    showPresetDialog = false
                                }
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = preset.title,
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.description,
                                    color = MutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetDialog = false }) {
                    Text("CLOSE", color = PureWhite)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    // --- AUDIT HISTORY DIALOG ---
    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = CyberCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Register Transition Audit Log",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No register changes logged yet.", color = MutedText, fontSize = 11.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(history.reversed()) { item ->
                            Surface(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, TerminalLightGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${item.registerName}: 0x${item.oldValue.toString(16).uppercase()} -> 0x${item.newValue.toString(16).uppercase()}",
                                            color = PureWhite,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = item.reason,
                                            color = MutedText,
                                            fontSize = 9.sp
                                        )
                                    }
                                    Text(
                                        text = "#${item.cycleIndex}",
                                        color = CyberCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("CLOSE", color = PureWhite)
                }
            },
            containerColor = TerminalDarkGray
        )
    }
}

/**
 * Individual Register Card component displaying formatted values, sub-register nesting,
 * delta changes, and quick micro-manipulation tools.
 */
@Composable
fun ReactiveRegisterCard(
    regName: String,
    regValue: Int,
    roleDescription: String,
    displayFormat: DashboardDisplayFormat,
    isModified: Boolean,
    isSelectedForMatrix: Boolean,
    isExpanded: Boolean,
    pulseAlpha: Float,
    snapshot: X86RegisterSnapshot,
    onSelectForMatrix: () -> Unit,
    onToggleExpand: () -> Unit,
    onQuickSet: (Int) -> Unit,
    onZero: () -> Unit,
    onNot: () -> Unit,
    onEdit: () -> Unit,
    onSubRegisterEdit: (String, Int) -> Unit
) {
    val borderColor = when {
        isModified -> AlertOrange.copy(alpha = pulseAlpha)
        isSelectedForMatrix -> CyberCyan
        else -> TerminalLightGray
    }

    val containerBg = when {
        isModified -> TerminalDarkGray
        isSelectedForMatrix -> TerminalDarkGray.copy(alpha = 0.8f)
        else -> TerminalBlack
    }

    val formattedMain = when (displayFormat) {
        DashboardDisplayFormat.HEX -> "0x" + regValue.toUInt().toString(16).uppercase().padStart(8, '0')
        DashboardDisplayFormat.DEC_SIGNED -> if (regValue >= 0) "+$regValue" else "$regValue"
        DashboardDisplayFormat.DEC_UNSIGNED -> regValue.toUInt().toString()
        DashboardDisplayFormat.BINARY -> {
            val bin = regValue.toUInt().toString(2).padStart(32, '0')
            bin.chunked(8).joinToString(" ")
        }
        DashboardDisplayFormat.ASCII -> {
            val b0 = (regValue and 0xFF).toChar().let { if (it in ' '..'~') it else '.' }
            val b1 = ((regValue ushr 8) and 0xFF).toChar().let { if (it in ' '..'~') it else '.' }
            val b2 = ((regValue ushr 16) and 0xFF).toChar().let { if (it in ' '..'~') it else '.' }
            val b3 = ((regValue ushr 24) and 0xFF).toChar().let { if (it in ' '..'~') it else '.' }
            "'$b3 $b2 $b1 $b0'"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reactive_card_$regName"),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(if (isModified || isSelectedForMatrix) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = regName,
                        color = if (isModified) AlertOrange else if (isSelectedForMatrix) CyberCyan else NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = roleDescription,
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Bit-matrix selector button
                    IconButton(
                        onClick = onSelectForMatrix,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Matrix Focus",
                            tint = if (isSelectedForMatrix) CyberCyan else MutedText,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Edit button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Value",
                            tint = CyberCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Expand sub-registers
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Sub-registers",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Value Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formattedMain,
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Secondary Hex / Dec Summary
                Text(
                    text = "DEC: $regValue  HEX: 0x${regValue.toUInt().toString(16).uppercase()}",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Quick Micro-actions Row (+1, -1, ZERO, ~NOT)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallActionBtn("+1") { onQuickSet(1) }
                SmallActionBtn("-1") { onQuickSet(-1) }
                SmallActionBtn("ZERO") { onZero() }
                SmallActionBtn("~NOT") { onNot() }
            }

            // Sub-Registers Hierarchy (Expanded View)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TerminalLightGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "HARDWARE SUB-REGISTER SLICES",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                when (regName) {
                    "EAX" -> SubRegisterFamilyRow(
                        name16 = "AX", val16 = snapshot.ax,
                        name8H = "AH", val8H = snapshot.ah,
                        name8L = "AL", val8L = snapshot.al,
                        onSubRegisterEdit = onSubRegisterEdit
                    )
                    "EBX" -> SubRegisterFamilyRow(
                        name16 = "BX", val16 = snapshot.bx,
                        name8H = "BH", val8H = snapshot.bh,
                        name8L = "BL", val8L = snapshot.bl,
                        onSubRegisterEdit = onSubRegisterEdit
                    )
                    "ECX" -> SubRegisterFamilyRow(
                        name16 = "CX", val16 = snapshot.cx,
                        name8H = "CH", val8H = snapshot.ch,
                        name8L = "CL", val8L = snapshot.cl,
                        onSubRegisterEdit = onSubRegisterEdit
                    )
                    "EDX" -> SubRegisterFamilyRow(
                        name16 = "DX", val16 = snapshot.dx,
                        name8H = "DH", val8H = snapshot.dh,
                        name8L = "DL", val8L = snapshot.dl,
                        onSubRegisterEdit = onSubRegisterEdit
                    )
                    "ESI" -> SubRegister16OnlyRow("SI", snapshot.si, onSubRegisterEdit)
                    "EDI" -> SubRegister16OnlyRow("DI", snapshot.di, onSubRegisterEdit)
                    "ESP" -> SubRegister16OnlyRow("SP", snapshot.sp, onSubRegisterEdit)
                    "EBP" -> SubRegister16OnlyRow("BP", snapshot.bp, onSubRegisterEdit)
                }
            }
        }
    }
}

@Composable
private fun FlagIndicatorChip(
    flagName: String,
    isActive: Boolean,
    title: String,
    pulseAlpha: Float,
    onClick: () -> Unit
) {
    val activeColor = if (flagName == "ZF") NeonGreen else if (flagName in listOf("CF", "OF", "SF")) AlertOrange else CyberCyan

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isActive) activeColor.copy(alpha = 0.2f) else TerminalBlack)
                .border(
                    BorderStroke(1.dp, if (isActive) activeColor else TerminalLightGray),
                    RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = flagName,
                color = if (isActive) activeColor else MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isActive) "1" else "0",
            color = if (isActive) activeColor else MutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SubRegisterFamilyRow(
    name16: String,
    val16: Int,
    name8H: String,
    val8H: Int,
    name8L: String,
    val8L: Int,
    onSubRegisterEdit: (String, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SubRegChip(name16, "0x" + val16.toString(16).uppercase().padStart(4, '0'), "16-bit", CyberCyan, Modifier.weight(1.2f)) {
            onSubRegisterEdit(name16, (val16 + 1) and 0xFFFF)
        }
        SubRegChip(name8H, "0x" + val8H.toString(16).uppercase().padStart(2, '0'), "High 8-bit", AlertOrange, Modifier.weight(1f)) {
            onSubRegisterEdit(name8H, (val8H + 1) and 0xFF)
        }
        SubRegChip(name8L, "0x" + val8L.toString(16).uppercase().padStart(2, '0'), "Low 8-bit", NeonGreen, Modifier.weight(1f)) {
            onSubRegisterEdit(name8L, (val8L + 1) and 0xFF)
        }
    }
}

@Composable
private fun SubRegister16OnlyRow(
    name16: String,
    val16: Int,
    onSubRegisterEdit: (String, Int) -> Unit
) {
    SubRegChip(
        name16,
        "0x" + val16.toString(16).uppercase().padStart(4, '0'),
        "16-bit Sub-Register (Bits 0..15)",
        CyberCyan,
        Modifier.fillMaxWidth()
    ) {
        onSubRegisterEdit(name16, (val16 + 1) and 0xFFFF)
    }
}

@Composable
private fun SubRegChip(
    title: String,
    valueStr: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = TerminalBlack,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, accent),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(valueStr, color = PureWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Text(subtitle, color = MutedText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun SmallActionBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(TerminalDarkGray)
            .border(BorderStroke(0.5.dp, TerminalLightGray), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = PureWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun MicroOpChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(TerminalBlack)
            .border(BorderStroke(1.dp, AlertOrange.copy(alpha = 0.7f)), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = AlertOrange,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
