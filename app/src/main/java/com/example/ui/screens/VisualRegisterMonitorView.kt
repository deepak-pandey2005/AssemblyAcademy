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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Architecture
import com.example.simulator.CpuState
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

enum class RegisterDisplayFormat(val label: String) {
    HEX("HEX"),
    DEC_SIGNED("DEC (S)"),
    DEC_UNSIGNED("DEC (U)"),
    BIN("BINARY"),
    ASCII("ASCII")
}

enum class RegisterCategory(val label: String) {
    ALL("ALL"),
    GENERAL("GENERAL"),
    POINTERS("POINTERS"),
    MODIFIED_ONLY("MODIFIED"),
    PINNED("WATCHLIST")
}

/**
 * Real-time Visual Register Monitoring View.
 * Displays interactive live CPU registers with animated change detection,
 * sub-register bit breakdowns, status flags (EFLAGS), and step modification history.
 */
@Composable
fun VisualRegisterMonitorView(
    viewModel: AssemblyViewModel,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onOpenDashboard: (() -> Unit)? = null
) {
    val cpuState by viewModel.cpuState.collectAsState()
    val modifiedRegisters by viewModel.modifiedRegisters.collectAsState()
    val previousRegisters by viewModel.previousRegisters.collectAsState()
    val registerHistory by viewModel.registerHistory.collectAsState()

    VisualRegisterMonitorContent(
        cpuState = cpuState,
        modifiedRegisters = modifiedRegisters,
        previousRegisters = previousRegisters,
        registerHistory = registerHistory,
        onUpdateRegister = { reg, value -> viewModel.updateRegisterValue(reg, value) },
        onClearHistory = { viewModel.clearRegisterHistory() },
        onOpenDashboard = onOpenDashboard,
        modifier = modifier,
        isCompact = isCompact
    )
}

@Composable
fun VisualRegisterMonitorContent(
    cpuState: CpuState,
    modifiedRegisters: Set<String>,
    previousRegisters: Map<String, Int>,
    registerHistory: List<AssemblyViewModel.RegisterChange>,
    onUpdateRegister: (String, Int) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onOpenDashboard: (() -> Unit)? = null
) {
    var displayFormat by remember { mutableStateOf(RegisterDisplayFormat.HEX) }
    var selectedCategory by remember { mutableStateOf(RegisterCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val pinnedRegisters = remember { mutableStateListOf<String>() }

    var editingRegisterName by remember { mutableStateOf<String?>(null) }
    var editingRegisterValue by remember { mutableStateOf("") }
    var expandedRegisterName by remember { mutableStateOf<String?>(null) }
    var showTimelineDialog by remember { mutableStateOf(false) }

    // Pulsing animation for modified registers
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Filter registers
    val allRegisters = cpuState.registers.toList()
    val filteredRegisters = remember(allRegisters, selectedCategory, searchQuery, modifiedRegisters, pinnedRegisters.size) {
        allRegisters.filter { (name, _) ->
            val matchesSearch = searchQuery.isBlank() || name.contains(searchQuery.trim(), ignoreCase = true)
            val matchesCategory = when (selectedCategory) {
                RegisterCategory.ALL -> true
                RegisterCategory.GENERAL -> isGeneralPurposeRegister(name, cpuState.architecture)
                RegisterCategory.POINTERS -> isPointerOrIndexRegister(name, cpuState.architecture)
                RegisterCategory.MODIFIED_ONLY -> modifiedRegisters.contains(name)
                RegisterCategory.PINNED -> pinnedRegisters.contains(name)
            }
            matchesSearch && matchesCategory
        }.sortedWith(compareByDescending<Pair<String, Int>> { pinnedRegisters.contains(it.first) }
            .thenByDescending { modifiedRegisters.contains(it.first) }
            .thenBy { it.first }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
    ) {
        // --- Header Bar ---
        Surface(
            color = TerminalDarkGray,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live pulse indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (modifiedRegisters.isNotEmpty()) AlertOrange else NeonGreen
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE REGISTER MONITOR",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        if (modifiedRegisters.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = AlertOrange.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, AlertOrange.copy(alpha = pulseAlpha))
                            ) {
                                Text(
                                    text = "${modifiedRegisters.size} UPDATED",
                                    color = AlertOrange,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Format Toggle Buttons & History
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RegisterDisplayFormat.entries.take(4).forEach { format ->
                            val isSelected = displayFormat == format
                            Surface(
                                color = if (isSelected) CyberCyan.copy(alpha = 0.25f) else TerminalBlack,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else TerminalLightGray
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { displayFormat = format }
                                    .testTag("format_toggle_${format.name.lowercase()}")
                            ) {
                                Text(
                                    text = format.label,
                                    color = if (isSelected) CyberCyan else MutedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showTimelineDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Register History",
                                tint = if (registerHistory.isNotEmpty()) NeonGreen else MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        onOpenDashboard?.let { openDash ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonGreen.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, NeonGreen), RoundedCornerShape(4.dp))
                                    .clickable { openDash() }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Dashboard,
                                        contentDescription = "Reactive Dashboard",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DASHBOARD",
                                        color = NeonGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- EFLAGS / Status Flags Visual HUD ---
                EFlagsBar(flags = cpuState.flags)

                Spacer(modifier = Modifier.height(10.dp))

                // --- Category Filter Chips ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RegisterCategory.entries.forEach { category ->
                        val isCatSelected = selectedCategory == category
                        val count = when (category) {
                            RegisterCategory.ALL -> allRegisters.size
                            RegisterCategory.GENERAL -> allRegisters.count { isGeneralPurposeRegister(it.first, cpuState.architecture) }
                            RegisterCategory.POINTERS -> allRegisters.count { isPointerOrIndexRegister(it.first, cpuState.architecture) }
                            RegisterCategory.MODIFIED_ONLY -> modifiedRegisters.size
                            RegisterCategory.PINNED -> pinnedRegisters.size
                        }

                        Surface(
                            color = if (isCatSelected) NeonGreen.copy(alpha = 0.15f) else TerminalBlack,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCatSelected) NeonGreen else TerminalLightGray.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = category }
                                .testTag("cat_chip_${category.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = category.label,
                                    color = if (isCatSelected) NeonGreen else PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    color = if (isCatSelected) NeonGreen else MutedText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Registers List Content ---
        if (filteredRegisters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedCategory == RegisterCategory.MODIFIED_ONLY) {
                        "No registers modified yet. Step forward (⏭) to see changes!"
                    } else if (selectedCategory == RegisterCategory.PINNED) {
                        "No pinned registers. Tap the star icon on any register to watch it."
                    } else {
                        "No matching registers found."
                    },
                    color = MutedText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isCompact) 280.dp else 450.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredRegisters, key = { it.first }) { (regName, regValue) ->
                    val isModified = modifiedRegisters.contains(regName)
                    val isPinned = pinnedRegisters.contains(regName)
                    val prevValue = previousRegisters[regName]
                    val isExpanded = expandedRegisterName == regName

                    RegisterCard(
                        regName = regName,
                        regValue = regValue,
                        prevValue = prevValue,
                        isModified = isModified,
                        isPinned = isPinned,
                        isExpanded = isExpanded,
                        format = displayFormat,
                        pulseAlpha = pulseAlpha,
                        architecture = cpuState.architecture,
                        onToggleExpand = {
                            expandedRegisterName = if (isExpanded) null else regName
                        },
                        onTogglePin = {
                            if (isPinned) pinnedRegisters.remove(regName)
                            else pinnedRegisters.add(regName)
                        },
                        onEdit = {
                            editingRegisterName = regName
                            editingRegisterValue = regValue.toString()
                        },
                        onQuickSet = { newVal -> onUpdateRegister(regName, newVal) },
                        onToggleBit = { bitIndex ->
                            val flipped = regValue xor (1 shl bitIndex)
                            onUpdateRegister(regName, flipped)
                        }
                    )
                }
            }
        }
    }

    // --- Edit Register Modal Dialog ---
    if (editingRegisterName != null) {
        val reg = editingRegisterName!!
        AlertDialog(
            onDismissRequest = { editingRegisterName = null },
            containerColor = TerminalDarkGray,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EDIT REGISTER: $reg",
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter a scalar value in Decimal (e.g., 42 or -1) or Hex (e.g., 0x2A):",
                        color = MutedText,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editingRegisterValue,
                        onValueChange = { editingRegisterValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_register_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PureWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = TerminalLightGray
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // Quick Presets
                    Text(
                        text = "QUICK INJECTIONS:",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 42, 100, 0xFF, -1).forEach { preset ->
                            Surface(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, TerminalLightGray),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { editingRegisterValue = preset.toString() }
                            ) {
                                Text(
                                    text = if (preset < 0) "$preset" else if (preset > 100) "0x${preset.toString(16).uppercase()}" else "$preset",
                                    color = NeonGreen,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = parseRegisterValue(editingRegisterValue)
                        if (parsed != null) {
                            onUpdateRegister(reg, parsed)
                        }
                        editingRegisterName = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("confirm_register_edit")
                ) {
                    Text("APPLY", color = TerminalBlack, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRegisterName = null }) {
                    Text("CANCEL", color = MutedText, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // --- Timeline History Dialog ---
    if (showTimelineDialog) {
        AlertDialog(
            onDismissRequest = { showTimelineDialog = false },
            containerColor = TerminalDarkGray,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "REGISTER MODIFICATION HISTORY",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (registerHistory.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = BrightPink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            text = {
                if (registerHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No register modifications recorded yet.\nStep through instructions to record transitions.",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(registerHistory.reversed()) { change ->
                            Surface(
                                color = TerminalBlack,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, TerminalLightGray.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = NeonGreen.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = change.regName,
                                                    color = NeonGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Step #${change.stepIndex}",
                                                color = MutedText,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        if (change.instruction.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = change.instruction,
                                                color = PureWhite,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "0x${change.oldValue.toString(16).uppercase()} -> 0x${change.newValue.toString(16).uppercase()}",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        val delta = change.newValue - change.oldValue
                                        val deltaStr = if (delta >= 0) "+$delta" else "$delta"
                                        Text(
                                            text = "Δ $deltaStr (${change.newValue})",
                                            color = if (delta >= 0) NeonGreen else BrightPink,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimelineDialog = false }) {
                    Text("CLOSE", color = CyberCyan, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

/**
 * Individual Register Card with real-time change pulse, multi-format view,
 * sub-register slicing, bit breakdown grid, and quick actions.
 */
@Composable
private fun RegisterCard(
    regName: String,
    regValue: Int,
    prevValue: Int?,
    isModified: Boolean,
    isPinned: Boolean,
    isExpanded: Boolean,
    format: RegisterDisplayFormat,
    pulseAlpha: Float,
    architecture: Architecture,
    onToggleExpand: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onQuickSet: (Int) -> Unit,
    onToggleBit: (Int) -> Unit
) {
    val borderColor = when {
        isModified -> AlertOrange.copy(alpha = pulseAlpha)
        isPinned -> CyberCyan
        else -> TerminalLightGray
    }

    val containerBg = when {
        isModified -> TerminalDarkGray
        else -> TerminalBlack
    }

    val roleDescription = getRegisterRole(regName, architecture)

    Surface(
        color = containerBg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (isModified || isPinned) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("register_card_$regName")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Card Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Register Name
                    Text(
                        text = regName,
                        color = if (isModified) AlertOrange else NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Role Tag
                    Text(
                        text = roleDescription,
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isPointerOrIndexRegister(regName, architecture) && regValue in 0..0xFFFF) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, CyberCyan)
                        ) {
                            Text(
                                text = "-> RAM 0x" + regValue.toString(16).uppercase().padStart(4, '0'),
                                color = CyberCyan,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                // Status Badges & Pin/Expand Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isModified && prevValue != null && prevValue != regValue) {
                        val delta = regValue - prevValue
                        val deltaSign = if (delta > 0) "+$delta" else "$delta"
                        Surface(
                            color = AlertOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, AlertOrange)
                        ) {
                            Text(
                                text = "Δ $deltaSign",
                                color = AlertOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Pin Register",
                            tint = if (isPinned) CyberCyan else MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Register Value",
                            tint = CyberCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Register Details",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Value Display
            val formattedValue = formatRegisterValue(regValue, format)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formattedValue,
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isModified && prevValue != null) {
                        Text(
                            text = "PREV: 0x${prevValue.toString(16).uppercase()} ($prevValue)",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Quick Format Equivalents
                val hexSummary = "0x" + regValue.toString(16).uppercase().padStart(8, '0')
                val decSummary = regValue.toString()
                Text(
                    text = if (format == RegisterDisplayFormat.HEX) "DEC: $decSummary" else hexSummary,
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Sub-Registers Slicing for x86 (e.g. EAX -> AX, AH, AL)
            val subRegs = getSubRegisters(regName, regValue, architecture)
            if (subRegs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subRegs.forEach { sub ->
                        Surface(
                            color = TerminalDarkGray,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(0.5.dp, TerminalLightGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                Text(
                                    text = sub.first,
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "0x" + sub.second.toString(16).uppercase(),
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // --- Expandable Bit Slicing & Direct Manipulation ---
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = TerminalLightGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "32-BIT BINARY CELL INSPECTION (CLICK BITS TO TOGGLE)",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // 4 Byte Blocks: [31..24] [23..16] [15..8] [7..0]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (byteIdx in 3 downTo 0) {
                            val byteStartBit = (byteIdx + 1) * 8 - 1
                            val byteEndBit = byteIdx * 8
                            val byteVal = (regValue ushr byteEndBit) and 0xFF

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TerminalDarkGray)
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "B$byteIdx [$byteStartBit:$byteEndBit]",
                                    color = MutedText,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "0x" + byteVal.toString(16).uppercase().padStart(2, '0'),
                                    color = NeonGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                // Individual 8 bits
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    for (bit in byteStartBit downTo byteEndBit) {
                                        val isHigh = ((regValue ushr bit) and 1) == 1
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (isHigh) NeonGreen else TerminalBlack)
                                                .border(0.5.dp, if (isHigh) NeonGreen else TerminalLightGray, RoundedCornerShape(2.dp))
                                                .clickable { onToggleBit(bit) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isHigh) "1" else "0",
                                                color = if (isHigh) TerminalBlack else MutedText,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Manipulation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickActionButton(label = "ZERO", onClick = { onQuickSet(0) }, color = BrightPink)
                        QuickActionButton(label = "+1", onClick = { onQuickSet(regValue + 1) }, color = NeonGreen)
                        QuickActionButton(label = "-1", onClick = { onQuickSet(regValue - 1) }, color = NeonGreen)
                        QuickActionButton(label = "NOT (~)", onClick = { onQuickSet(regValue.inv()) }, color = CyberCyan)
                        QuickActionButton(label = "0xFF", onClick = { onQuickSet(0xFF) }, color = AlertOrange)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
    color: Color
) {
    Surface(
        color = TerminalDarkGray,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Real-time EFLAGS / CPU status register display showing ZF, CF, SF, OF, PF
 * and live branch prediction outcomes.
 */
@Composable
private fun EFlagsBar(flags: Map<String, Boolean>) {
    Surface(
        color = TerminalBlack,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, TerminalLightGray.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EFLAGS STATUS REGISTER",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Branch condition evaluator preview
                val zf = flags["ZF"] ?: false
                val sf = flags["SF"] ?: false
                val of = flags["OF"] ?: false
                val jeTaken = zf
                val jgTaken = !zf && (sf == of)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "JZ: ${if (jeTaken) "TAKEN" else "FALLTHROUGH"}",
                        color = if (jeTaken) NeonGreen else MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "JG: ${if (jgTaken) "TAKEN" else "FALLTHROUGH"}",
                        color = if (jgTaken) NeonGreen else MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Flags row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ZF", "CF", "SF", "OF").forEach { flagName ->
                    val isActive = flags[flagName] ?: false
                    Surface(
                        color = if (isActive) NeonGreen.copy(alpha = 0.2f) else TerminalDarkGray,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (isActive) NeonGreen else TerminalLightGray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = flagName,
                                color = if (isActive) NeonGreen else PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) NeonGreen else TerminalLightGray)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Helper Functions ---

private fun formatRegisterValue(value: Int, format: RegisterDisplayFormat): String {
    return when (format) {
        RegisterDisplayFormat.HEX -> "0x" + value.toString(16).uppercase().padStart(8, '0')
        RegisterDisplayFormat.DEC_SIGNED -> value.toString()
        RegisterDisplayFormat.DEC_UNSIGNED -> (value.toLong() and 0xFFFFFFFFL).toString()
        RegisterDisplayFormat.BIN -> {
            val unsigned = value.toLong() and 0xFFFFFFFFL
            val raw = unsigned.toString(2).padStart(32, '0')
            // Format as 4 bytes: 00000000 00000000 00000000 00000000
            "${raw.substring(0, 8)} ${raw.substring(8, 16)} ${raw.substring(16, 24)} ${raw.substring(24, 32)}"
        }
        RegisterDisplayFormat.ASCII -> {
            val charVal = (value and 0xFF).toChar()
            if (charVal.isLetterOrDigit() || charVal.isWhitespace()) "'$charVal' (ASCII ${value and 0xFF})"
            else "'\\x${(value and 0xFF).toString(16).uppercase()}' (Non-printable)"
        }
    }
}

private fun getRegisterRole(regName: String, arch: Architecture): String {
    return when (arch) {
        Architecture.X86 -> when (regName.uppercase()) {
            "EAX" -> "Accumulator / Arithmetic & Ret"
            "EBX" -> "Base Register / Pointer"
            "ECX" -> "Counter Register / Loops"
            "EDX" -> "Data Register / I/O & Mul"
            "ESP" -> "Stack Pointer (Top of Stack)"
            "EBP" -> "Base Pointer (Stack Frame)"
            "ESI" -> "Source Index (Memory Stream)"
            "EDI" -> "Destination Index (Memory Stream)"
            "EIP" -> "Instruction Pointer"
            else -> "General Register"
        }
        Architecture.ARM -> when (regName.uppercase()) {
            "R0" -> "Arg 0 / Return Value"
            "R1" -> "Arg 1"
            "R2" -> "Arg 2"
            "R3" -> "Arg 3"
            "SP" -> "Stack Pointer"
            "LR" -> "Link Register (Return Addr)"
            "PC" -> "Program Counter"
            else -> "General Register"
        }
        Architecture.GPU_DEV -> when (regName.uppercase()) {
            "S0" -> "Scalar Accumulator"
            "S1" -> "Scalar Base"
            "S2" -> "Scalar Stride"
            "S3" -> "Scalar Bound"
            else -> "Scalar Unit"
        }
        Architecture.OS_DEV -> when (regName.uppercase()) {
            "AX" -> "Real-Mode Accumulator"
            "BX" -> "Real-Mode Base"
            "CX" -> "Real-Mode Counter"
            "DX" -> "Real-Mode Data"
            "SP" -> "Stack Pointer (0x7C00)"
            "CR0" -> "Control Register 0 (PE Mode)"
            else -> "OS Register"
        }
    }
}

private fun isGeneralPurposeRegister(regName: String, arch: Architecture): Boolean {
    val u = regName.uppercase()
    return when (arch) {
        Architecture.X86 -> u in listOf("EAX", "EBX", "ECX", "EDX", "RAX", "RBX", "RCX", "RDX")
        Architecture.ARM -> u in listOf("R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7")
        Architecture.GPU_DEV -> u.startsWith("S")
        Architecture.OS_DEV -> u in listOf("AX", "BX", "CX", "DX")
    }
}

private fun isPointerOrIndexRegister(regName: String, arch: Architecture): Boolean {
    val u = regName.uppercase()
    return when (arch) {
        Architecture.X86 -> u in listOf("ESP", "EBP", "ESI", "EDI", "RSP", "RBP", "RSI", "RDI", "EIP")
        Architecture.ARM -> u in listOf("SP", "LR", "PC")
        Architecture.GPU_DEV -> false
        Architecture.OS_DEV -> u in listOf("SP", "SI", "DI", "CR0")
    }
}

private fun getSubRegisters(regName: String, regValue: Int, arch: Architecture): List<Pair<String, Int>> {
    if (arch != Architecture.X86) return emptyList()
    val u = regName.uppercase()
    return when (u) {
        "EAX" -> listOf(
            "AX [15:0]" to (regValue and 0xFFFF),
            "AH [15:8]" to ((regValue ushr 8) and 0xFF),
            "AL [7:0]" to (regValue and 0xFF)
        )
        "EBX" -> listOf(
            "BX [15:0]" to (regValue and 0xFFFF),
            "BH [15:8]" to ((regValue ushr 8) and 0xFF),
            "BL [7:0]" to (regValue and 0xFF)
        )
        "ECX" -> listOf(
            "CX [15:0]" to (regValue and 0xFFFF),
            "CH [15:8]" to ((regValue ushr 8) and 0xFF),
            "CL [7:0]" to (regValue and 0xFF)
        )
        "EDX" -> listOf(
            "DX [15:0]" to (regValue and 0xFFFF),
            "DH [15:8]" to ((regValue ushr 8) and 0xFF),
            "DL [7:0]" to (regValue and 0xFF)
        )
        "ESP" -> listOf("SP [15:0]" to (regValue and 0xFFFF))
        "EBP" -> listOf("BP [15:0]" to (regValue and 0xFFFF))
        "ESI" -> listOf("SI [15:0]" to (regValue and 0xFFFF))
        "EDI" -> listOf("DI [15:0]" to (regValue and 0xFFFF))
        else -> emptyList()
    }
}

private fun parseRegisterValue(input: String): Int? {
    val trimmed = input.trim()
    return try {
        if (trimmed.startsWith("0x", ignoreCase = true)) {
            val hex = trimmed.substring(2)
            hex.toLong(16).toInt()
        } else {
            trimmed.toInt()
        }
    } catch (_: Exception) {
        null
    }
}
