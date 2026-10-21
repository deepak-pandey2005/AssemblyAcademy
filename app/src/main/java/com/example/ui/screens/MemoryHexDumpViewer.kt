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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Input
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Architecture
import com.example.simulator.CpuState
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel
import kotlinx.coroutines.launch

/**
 * Display modes for the Memory Hex Dump Viewer.
 */
enum class MemoryViewMode(val title: String) {
    HEX_DUMP("HEX DUMP"),
    ALONGSIDE_REGISTERS("ALONGSIDE REGISTERS"),
    POINTER_RADAR("POINTER RADAR")
}

/**
 * Byte alignment grouping per row.
 */
enum class BytesPerRow(val count: Int, val label: String) {
    BYTES_8(8, "8 BYTES"),
    BYTES_16(16, "16 BYTES")
}

/**
 * Word display grouping.
 */
enum class HexGrouping(val bytes: Int, val label: String) {
    BYTE_1(1, "1 Byte (00)"),
    WORD_2(2, "2 Bytes (0000)"),
    DWORD_4(4, "4 Bytes (00000000)")
}

/**
 * Quick jump presets for standard memory regions.
 */
data class MemoryPreset(
    val title: String,
    val subtitle: String,
    val baseAddress: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

/**
 * Production-grade Memory Viewer Component displaying simulated RAM contents in a classic
 * Hex Dump format, allowing users to inspect memory addresses alongside CPU register values,
 * trace pointer references (ESP, EBP, ESI, EDI), and modify memory values in real time.
 */
@Composable
fun MemoryHexDumpViewer(
    viewModel: AssemblyViewModel,
    modifier: Modifier = Modifier,
    initialAddress: Int = 96
) {
    val cpuState by viewModel.cpuState.collectAsState()
    val modifiedMemoryAddresses by viewModel.modifiedMemoryAddresses.collectAsState()
    val currentArch by viewModel.selectedArchitecture.collectAsState()

    MemoryHexDumpViewerContent(
        cpuState = cpuState,
        modifiedMemoryAddresses = modifiedMemoryAddresses,
        currentArch = currentArch,
        onUpdateByte = { addr, value -> viewModel.updateMemoryByte(addr, value) },
        onUpdateDword = { addr, value -> viewModel.updateMemoryValue(addr, value) },
        onUpdateString = { addr, text -> viewModel.updateMemoryString(addr, text) },
        onClearMemory = { viewModel.clearMemory() },
        onResetDefaults = { viewModel.resetMemoryDefaults(currentArch) },
        modifier = modifier,
        initialAddress = initialAddress
    )
}

@Composable
fun MemoryHexDumpViewerContent(
    cpuState: CpuState,
    modifiedMemoryAddresses: Set<Int>,
    currentArch: Architecture,
    onUpdateByte: (Int, Int) -> Unit,
    onUpdateDword: (Int, Int) -> Unit,
    onUpdateString: (Int, String) -> Unit,
    onClearMemory: () -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
    initialAddress: Int = 96
) {
    var viewMode by remember { mutableStateOf(MemoryViewMode.ALONGSIDE_REGISTERS) }
    var bytesPerRow by remember { mutableStateOf(BytesPerRow.BYTES_8) }
    var hexGrouping by remember { mutableStateOf(HexGrouping.BYTE_1) }
    var isLittleEndian by remember { mutableStateOf(true) }
    var baseAddress by remember { mutableIntStateOf(initialAddress) }
    var selectedByteAddress by remember { mutableStateOf<Int?>(100) }

    // Dialog & Edit states
    var showEditByteDialog by remember { mutableStateOf(false) }
    var showEditDwordDialog by remember { mutableStateOf(false) }
    var showInjectStringDialog by remember { mutableStateOf(false) }
    var showMemoryActionsDialog by remember { mutableStateOf(false) }

    var inputJumpAddress by remember { mutableStateOf("") }
    var isJumpError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Architecture-specific presets
    val presets = remember(cpuState.registers, currentArch) {
        val list = mutableListOf<MemoryPreset>()
        list.add(
            MemoryPreset("DATA SEG", "0x0060 (100)", 96, Icons.Default.Storage, CyberCyan)
        )
        val espVal = cpuState.registers["ESP"] ?: cpuState.registers["SP"] ?: 1000
        val ebpVal = cpuState.registers["EBP"] ?: cpuState.registers["BP"] ?: 1000
        list.add(
            MemoryPreset("STACK [ESP]", "0x${espVal.toString(16).uppercase()} ($espVal)", (espVal / 16) * 16 - 16, Icons.Default.Layers, BrightPink)
        )
        if (ebpVal != espVal) {
            list.add(
                MemoryPreset("FRAME [EBP]", "0x${ebpVal.toString(16).uppercase()}", (ebpVal / 16) * 16, Icons.Default.FilterFrames, AlertOrange)
            )
        }
        val esiVal = cpuState.registers["ESI"] ?: cpuState.registers["SI"] ?: 0
        if (esiVal in 1..0xFFFF) {
            list.add(
                MemoryPreset("SRC [ESI]", "0x${esiVal.toString(16).uppercase()}", (esiVal / 16) * 16, Icons.AutoMirrored.Filled.Input, NeonGreen)
            )
        }
        val ediVal = cpuState.registers["EDI"] ?: cpuState.registers["DI"] ?: 0
        if (ediVal in 1..0xFFFF) {
            list.add(
                MemoryPreset("DST [EDI]", "0x${ediVal.toString(16).uppercase()}", (ediVal / 16) * 16, Icons.Default.Outbox, AlertOrange)
            )
        }
        if (currentArch == Architecture.OS_DEV) {
            list.add(
                MemoryPreset("BOOT [0x7C00]", "0x7C00", 0x7C00, Icons.Default.Terminal, NeonGreen)
            )
        }
        list.add(
            MemoryPreset("LOW MEM", "0x0000", 0, Icons.Default.Memory, PureWhite)
        )
        list
    }

    // Helper: Map which CPU registers point to a given memory address
    fun getRegistersPointingTo(address: Int): List<String> {
        val matches = mutableListOf<String>()
        cpuState.registers.forEach { (reg, regVal) ->
            if (regVal == address || (address >= regVal && address < regVal + 4)) {
                matches.add(reg)
            }
        }
        return matches
    }

    // Safe byte reader for memory
    fun readByte(address: Int): Int {
        if (cpuState.memory.containsKey(address)) {
            return cpuState.memory[address]!! and 0xFF
        }
        val aligned = (address / 4) * 4
        if (cpuState.memory.containsKey(aligned)) {
            val dword = cpuState.memory[aligned]!!
            val byteOffset = address - aligned
            return (dword ushr (byteOffset * 8)) and 0xFF
        }
        return 0
    }

    // Safe 32-bit dword reader
    fun readDword(address: Int): Long {
        val b0 = readByte(address).toLong() and 0xFF
        val b1 = readByte(address + 1).toLong() and 0xFF
        val b2 = readByte(address + 2).toLong() and 0xFF
        val b3 = readByte(address + 3).toLong() and 0xFF
        return if (isLittleEndian) {
            b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        } else {
            (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
        }
    }

    // Generate rows to display around baseAddress (16 rows = 128 or 256 bytes)
    val rowCount = 14
    val stride = bytesPerRow.count
    val rows = remember(baseAddress, bytesPerRow) {
        List(rowCount) { index ->
            val rowAddr = (baseAddress / stride) * stride + (index * stride)
            rowAddr
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground)
            .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(8.dp))
            .testTag("memory_hex_dump_viewer")
    ) {
        // --- 1. TOP TOOLBAR: Status, View Modes & Settings ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and pulse
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SIMULATED RAM HEX DUMP",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Action Menu button (Clear, Reset, Inject)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showMemoryActionsDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Memory Tools",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Mode Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalBlack)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MemoryViewMode.values().forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) CyberCyan else Color.Transparent
                                ),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { viewMode = mode }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            color = if (isSelected) CyberCyan else MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-Toolbar: Row Width, Grouping, Endianness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Width switch (8 vs 16 bytes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BytesPerRow.values().forEach { option ->
                        val isSel = bytesPerRow == option
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) NeonGreen.copy(alpha = 0.15f) else TerminalBlack)
                                .border(BorderStroke(1.dp, if (isSel) NeonGreen else TerminalLightGray), RoundedCornerShape(4.dp))
                                .clickable { bytesPerRow = option }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSel) NeonGreen else MutedText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Endianness toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalBlack)
                        .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(4.dp))
                        .clickable { isLittleEndian = !isLittleEndian }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isLittleEndian) "LE (Little-Endian)" else "BE (Big-Endian)",
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 2. REGION PRESETS & ADDRESS JUMP BAR ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Horizontal scrollable region presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isCurrent = baseAddress in (preset.baseAddress - 16)..(preset.baseAddress + 64)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isCurrent) preset.color.copy(alpha = 0.2f) else TerminalDarkGray)
                            .border(
                                BorderStroke(1.dp, if (isCurrent) preset.color else TerminalLightGray),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                baseAddress = preset.baseAddress
                                selectedByteAddress = preset.baseAddress
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = preset.icon,
                                contentDescription = null,
                                tint = preset.color,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${preset.title} (${preset.subtitle})",
                                color = if (isCurrent) PureWhite else MutedText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address Stepper & Search Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page backward
                IconButton(
                    onClick = {
                        val newBase = (baseAddress - (bytesPerRow.count * 8)).coerceAtLeast(0)
                        baseAddress = newBase
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(TerminalBlack, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Previous Page",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Page forward
                IconButton(
                    onClick = {
                        baseAddress += (bytesPerRow.count * 8)
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(TerminalBlack, RoundedCornerShape(4.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Next Page",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Jump input
                OutlinedTextField(
                    value = inputJumpAddress,
                    onValueChange = {
                        inputJumpAddress = it
                        isJumpError = false
                    },
                    placeholder = {
                        Text(
                            text = "Jump to: 0x64 or 100",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    isError = isJumpError,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("memory_jump_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = TerminalLightGray,
                        errorBorderColor = AlertOrange
                    )
                )

                Button(
                    onClick = {
                        val raw = inputJumpAddress.trim()
                        val parsed = if (raw.startsWith("0x", ignoreCase = true)) {
                            raw.substring(2).toIntOrNull(16)
                        } else {
                            raw.toIntOrNull() ?: raw.toIntOrNull(16)
                        }
                        if (parsed != null && parsed >= 0) {
                            baseAddress = (parsed / bytesPerRow.count) * bytesPerRow.count
                            selectedByteAddress = parsed
                            inputJumpAddress = ""
                            isJumpError = false
                        } else {
                            isJumpError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("memory_jump_button")
                ) {
                    Text(
                        text = "GO",
                        color = TerminalBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- 3. MAIN BODY ACCORDING TO VIEW MODE ---
        when (viewMode) {
            MemoryViewMode.ALONGSIDE_REGISTERS -> {
                AlongsideRegistersView(
                    cpuState = cpuState,
                    rows = rows,
                    bytesPerRow = bytesPerRow,
                    readByte = ::readByte,
                    getRegistersPointingTo = ::getRegistersPointingTo,
                    modifiedMemoryAddresses = modifiedMemoryAddresses,
                    selectedByteAddress = selectedByteAddress,
                    onSelectByte = { selectedByteAddress = it },
                    onJumpToAddress = { addr ->
                        baseAddress = (addr / bytesPerRow.count) * bytesPerRow.count
                        selectedByteAddress = addr
                    }
                )
            }
            MemoryViewMode.HEX_DUMP -> {
                HexDumpTableView(
                    rows = rows,
                    bytesPerRow = bytesPerRow,
                    readByte = ::readByte,
                    getRegistersPointingTo = ::getRegistersPointingTo,
                    modifiedMemoryAddresses = modifiedMemoryAddresses,
                    selectedByteAddress = selectedByteAddress,
                    onSelectByte = { selectedByteAddress = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 340.dp)
                )
            }
            MemoryViewMode.POINTER_RADAR -> {
                PointerRadarView(
                    cpuState = cpuState,
                    readDword = ::readDword,
                    onJumpToAddress = { addr ->
                        baseAddress = (addr / bytesPerRow.count) * bytesPerRow.count
                        selectedByteAddress = addr
                        viewMode = MemoryViewMode.ALONGSIDE_REGISTERS
                    }
                )
            }
        }

        // --- 4. SELECTED BYTE HARDWARE INSPECTOR FOOTER ---
        if (selectedByteAddress != null) {
            val addr = selectedByteAddress!!
            val byteVal = readByte(addr)
            val dwordVal = readDword(addr)
            val pointingRegs = getRegistersPointingTo(addr)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "INSPECTING ADDR: 0x${addr.toString(16).uppercase().padStart(8, '0')}",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(DEC: $addr)",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Register pointers label
                        if (pointingRegs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BrightPink.copy(alpha = 0.2f))
                                    .border(BorderStroke(1.dp, BrightPink), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "POINTED BY: " + pointingRegs.joinToString(", "),
                                    color = BrightPink,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Multi-format values row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalBlack)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BYTE (8-BIT)", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = "HEX: 0x${byteVal.toString(16).uppercase().padStart(2, '0')}  DEC: $byteVal  BIN: ${byteVal.toString(2).padStart(8, '0')}",
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("ASCII CHAR", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            val charRep = if (byteVal in 32..126) "'${byteVal.toChar()}'" else "'.' (unprintable)"
                            Text(
                                text = charRep,
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 32-bit dword representation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalBlack)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DWORD [Addr..+3]: 0x${dwordVal.toString(16).uppercase().padStart(8, '0')} ($dwordVal)",
                            color = PureWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isLittleEndian) "LITTLE-ENDIAN" else "BIG-ENDIAN",
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Edit and manipulation actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showEditByteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("memory_edit_byte_button")
                        ) {
                            Text("EDIT BYTE", color = TerminalBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showEditDwordDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("memory_edit_dword_button")
                        ) {
                            Text("EDIT DWORD", color = TerminalBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onUpdateByte(addr, 0) },
                            border = BorderStroke(1.dp, TerminalLightGray),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("ZERO", color = PureWhite, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }

                        OutlinedButton(
                            onClick = { onUpdateByte(addr, (byteVal + 1) and 0xFF) },
                            border = BorderStroke(1.dp, TerminalLightGray),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("+1", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS FOR EDITING MEMORY VALUES ---

    if (showEditByteDialog && selectedByteAddress != null) {
        var byteInput by remember { mutableStateOf(readByte(selectedByteAddress!!).toString()) }
        var isHexInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditByteDialog = false },
            title = {
                Text(
                    text = "Edit Byte at 0x${selectedByteAddress!!.toString(16).uppercase()}",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter new 8-bit byte value (0 to 255 or 0x00 to 0xFF):",
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = byteInput,
                        onValueChange = { byteInput = it },
                        label = { Text(if (isHexInput) "Hex (e.g., 2A)" else "Decimal (e.g., 42)", color = MutedText) },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = TerminalLightGray
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isHexInput,
                            onCheckedChange = { isHexInput = it }
                        )
                        Text("Hex format (0x)", color = PureWhite, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = if (isHexInput || byteInput.startsWith("0x", ignoreCase = true)) {
                            byteInput.replace("0x", "").toIntOrNull(16)
                        } else {
                            byteInput.toIntOrNull()
                        }
                        if (parsed != null) {
                            onUpdateByte(selectedByteAddress!!, parsed and 0xFF)
                        }
                        showEditByteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                ) {
                    Text("WRITE BYTE", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditByteDialog = false }) {
                    Text("CANCEL", color = MutedText)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    if (showEditDwordDialog && selectedByteAddress != null) {
        var dwordInput by remember { mutableStateOf(readDword(selectedByteAddress!!).toString()) }

        AlertDialog(
            onDismissRequest = { showEditDwordDialog = false },
            title = {
                Text(
                    text = "Edit 32-bit Dword at 0x${selectedByteAddress!!.toString(16).uppercase()}",
                    color = NeonGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter 32-bit Integer value (Decimal or 0x Hex):",
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dwordInput,
                        onValueChange = { dwordInput = it },
                        label = { Text("Value (e.g. 100 or 0x00000064)", color = MutedText) },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = TerminalLightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = if (dwordInput.startsWith("0x", ignoreCase = true)) {
                            dwordInput.substring(2).toLongOrNull(16)?.toInt()
                        } else {
                            dwordInput.toLongOrNull()?.toInt()
                        }
                        if (parsed != null) {
                            onUpdateDword(selectedByteAddress!!, parsed)
                        }
                        showEditDwordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Text("WRITE DWORD", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDwordDialog = false }) {
                    Text("CANCEL", color = MutedText)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    if (showInjectStringDialog && selectedByteAddress != null) {
        var stringInput by remember { mutableStateOf("HELLO") }

        AlertDialog(
            onDismissRequest = { showInjectStringDialog = false },
            title = {
                Text(
                    text = "Inject ASCII String",
                    color = AlertOrange,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Writes characters into RAM sequentially starting at 0x${selectedByteAddress!!.toString(16).uppercase()}:",
                        color = PureWhite,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = stringInput,
                        onValueChange = { stringInput = it },
                        label = { Text("Text string", color = MutedText) },
                        textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AlertOrange,
                            unfocusedBorderColor = TerminalLightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (stringInput.isNotEmpty()) {
                            onUpdateString(selectedByteAddress!!, stringInput)
                        }
                        showInjectStringDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertOrange)
                ) {
                    Text("INJECT", color = TerminalBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInjectStringDialog = false }) {
                    Text("CANCEL", color = MutedText)
                }
            },
            containerColor = TerminalDarkGray
        )
    }

    if (showMemoryActionsDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryActionsDialog = false },
            title = {
                Text(
                    text = "Memory Operations",
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showMemoryActionsDialog = false
                            showInjectStringDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalDarkGray),
                        border = BorderStroke(1.dp, AlertOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Inject ASCII String at Cursor", color = AlertOrange)
                    }

                    Button(
                        onClick = {
                            onResetDefaults()
                            showMemoryActionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalDarkGray),
                        border = BorderStroke(1.dp, CyberCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset RAM to Architecture Defaults", color = CyberCyan)
                    }

                    Button(
                        onClick = {
                            onClearMemory()
                            showMemoryActionsDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TerminalDarkGray),
                        border = BorderStroke(1.dp, BrightPink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All RAM (0x00)", color = BrightPink)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMemoryActionsDialog = false }) {
                    Text("CLOSE", color = PureWhite)
                }
            },
            containerColor = TerminalDarkGray
        )
    }
}

/**
 * Split/Alongside View showing active CPU registers synchronized side-by-side with the Hex Dump.
 */
@Composable
fun AlongsideRegistersView(
    cpuState: CpuState,
    rows: List<Int>,
    bytesPerRow: BytesPerRow,
    readByte: (Int) -> Int,
    getRegistersPointingTo: (Int) -> List<String>,
    modifiedMemoryAddresses: Set<Int>,
    selectedByteAddress: Int?,
    onSelectByte: (Int) -> Unit,
    onJumpToAddress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // --- Register Bank Strip ("Alongside" Header) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .padding(8.dp)
                .testTag("memory_alongside_registers_panel")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CPU REGISTERS ALONGSIDE RAM POINTERS",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "TAP REGISTER TO JUMP IN DUMP",
                    color = MutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontally scrollable register cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(cpuState.registers.toList()) { (reg, regVal) ->
                    val isStackPointer = reg in listOf("ESP", "SP", "RSP")
                    val isBasePointer = reg in listOf("EBP", "BP", "RBP")
                    val isIndexPointer = reg in listOf("ESI", "EDI", "SI", "DI")
                    val pointsToCurrentRow = rows.any { r -> regVal in r until (r + bytesPerRow.count) }

                    val badgeColor = when {
                        isStackPointer -> BrightPink
                        isBasePointer -> AlertOrange
                        isIndexPointer -> NeonGreen
                        pointsToCurrentRow -> CyberCyan
                        else -> TerminalLightGray
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (pointsToCurrentRow) badgeColor.copy(alpha = 0.2f) else TerminalBlack)
                            .border(BorderStroke(1.dp, badgeColor), RoundedCornerShape(4.dp))
                            .clickable { onJumpToAddress(regVal) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = reg,
                                    color = badgeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (pointsToCurrentRow) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Points Here",
                                        tint = badgeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Text(
                                text = "0x" + regVal.toUInt().toString(16).uppercase().padStart(4, '0'),
                                color = PureWhite,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "($regVal)",
                                color = MutedText,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Hex Dump Table underneath
        HexDumpTableView(
            rows = rows,
            bytesPerRow = bytesPerRow,
            readByte = readByte,
            getRegistersPointingTo = getRegistersPointingTo,
            modifiedMemoryAddresses = modifiedMemoryAddresses,
            selectedByteAddress = selectedByteAddress,
            onSelectByte = onSelectByte,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 320.dp)
        )
    }
}

/**
 * Interactive Hex Dump Table displaying addresses, hex bytes, and ASCII decoded text.
 */
@Composable
fun HexDumpTableView(
    rows: List<Int>,
    bytesPerRow: BytesPerRow,
    readByte: (Int) -> Int,
    getRegistersPointingTo: (Int) -> List<String>,
    modifiedMemoryAddresses: Set<Int>,
    selectedByteAddress: Int?,
    onSelectByte: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = bytesPerRow.count

    Column(
        modifier = modifier
            .background(DarkBackground)
    ) {
        // --- Table Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Address header
                Text(
                    text = "OFFSET",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(72.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Byte columns header
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until count) {
                        Text(
                            text = i.toString(16).uppercase().padStart(2, '0'),
                            color = MutedText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        if (i == (count / 2) - 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // ASCII header
                Text(
                    text = "ASCII",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(if (count == 8) 64.dp else 110.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- Table Rows ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            items(rows) { rowAddr ->
                // Check if any register points to this entire row
                val rowRegs = mutableListOf<String>()
                for (offset in 0 until count) {
                    val regs = getRegistersPointingTo(rowAddr + offset)
                    regs.forEach { if (!rowRegs.contains(it)) rowRegs.add(it) }
                }

                val isRowActive = rowRegs.isNotEmpty()
                val rowBg = if (isRowActive) TerminalDarkGray.copy(alpha = 0.5f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Address Offset (e.g., 0x0060)
                    Text(
                        text = "0x" + rowAddr.toString(16).uppercase().padStart(4, '0'),
                        color = if (isRowActive) CyberCyan else MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isRowActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(52.dp)
                    )

                    // Small pointer badge if any register points to this line
                    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                        if (rowRegs.isNotEmpty()) {
                            Text(
                                text = rowRegs.first(),
                                color = BrightPink,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Hex Bytes block
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (offset in 0 until count) {
                            val byteAddr = rowAddr + offset
                            val byteVal = readByte(byteAddr)
                            val isSelected = selectedByteAddress == byteAddr
                            val isModified = modifiedMemoryAddresses.contains(byteAddr)
                            val hasRegPointer = getRegistersPointingTo(byteAddr).isNotEmpty()

                            // Byte cell styling
                            val cellTextColor = when {
                                isSelected -> TerminalBlack
                                isModified -> AlertOrange
                                hasRegPointer -> BrightPink
                                byteVal != 0 -> PureWhite
                                else -> MutedText.copy(alpha = 0.45f)
                            }

                            val cellBg = when {
                                isSelected -> CyberCyan
                                isModified -> AlertOrange.copy(alpha = 0.25f)
                                hasRegPointer -> BrightPink.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(cellBg)
                                    .border(
                                        BorderStroke(
                                            width = if (isSelected || isModified) 1.dp else 0.dp,
                                            color = if (isSelected) CyberCyan else if (isModified) AlertOrange else Color.Transparent
                                        ),
                                        RoundedCornerShape(2.dp)
                                    )
                                    .clickable { onSelectByte(byteAddr) }
                                    .padding(vertical = 2.dp)
                                    .testTag("memory_byte_cell_$byteAddr"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = byteVal.toString(16).uppercase().padStart(2, '0'),
                                    color = cellTextColor,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected || isModified || byteVal != 0) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Midline gap for 8-byte group readability
                            if (offset == (count / 2) - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // ASCII Decoded Representation
                    Box(
                        modifier = Modifier
                            .width(if (count == 8) 64.dp else 110.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(TerminalBlack)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (offset in 0 until count) {
                                val byteAddr = rowAddr + offset
                                val byteVal = readByte(byteAddr)
                                val isSelected = selectedByteAddress == byteAddr
                                val charRep = if (byteVal in 32..126) byteVal.toChar().toString() else "."

                                Text(
                                    text = charRep,
                                    color = when {
                                        isSelected -> CyberCyan
                                        byteVal in 32..126 -> NeonGreen
                                        else -> MutedText.copy(alpha = 0.4f)
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Radar view listing all CPU registers and their targeted memory contents.
 */
@Composable
fun PointerRadarView(
    cpuState: CpuState,
    readDword: (Int) -> Long,
    onJumpToAddress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Text(
            text = "HARDWARE REGISTER POINTER RADAR",
            color = CyberCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Active CPU registers holding pointers and values at target RAM addresses:",
            color = MutedText,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(cpuState.registers.toList()) { (reg, regVal) ->
                val targetDword = readDword(regVal)
                val isStackPointer = reg in listOf("ESP", "SP", "RSP")
                val isBasePointer = reg in listOf("EBP", "BP", "RBP")
                val isIndexPointer = reg in listOf("ESI", "EDI", "SI", "DI")

                val accentColor = when {
                    isStackPointer -> BrightPink
                    isBasePointer -> AlertOrange
                    isIndexPointer -> NeonGreen
                    else -> CyberCyan
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(TerminalDarkGray)
                        .border(BorderStroke(1.dp, TerminalLightGray), RoundedCornerShape(4.dp))
                        .clickable { onJumpToAddress(regVal) }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .border(BorderStroke(1.dp, accentColor), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = reg,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "ADDR: 0x" + regVal.toUInt().toString(16).uppercase().padStart(4, '0') + " ($regVal)",
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "RAM Value at [$reg]: 0x" + targetDword.toString(16).uppercase().padStart(8, '0'),
                                color = MutedText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Button(
                        onClick = { onJumpToAddress(regVal) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("INSPECT", color = TerminalBlack, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
