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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Display format for memory contents.
 */
enum class MemoryTableFormat(val label: String, val shortDesc: String) {
    HEX_DWORD("HEX DWORD", "32-bit hex (0x00000000)"),
    DECIMAL_SIGNED("DEC (S)", "Signed integer (+/-)"),
    DECIMAL_UNSIGNED("DEC (U)", "Unsigned 32-bit uint"),
    BYTE_SPLIT("BYTES (4)", "Little-Endian bytes: B0 B1 B2 B3"),
    ASCII("ASCII", "Text representation")
}

/**
 * Memory Region Presets for quick navigation.
 */
enum class MemoryRegionPreset(val label: String, val startAddress: Int, val description: String) {
    DATA_SEG("DATA SEG", 96, "General Variables & Arrays (0x0060 - 0x00F0)"),
    STACK_SEG("STACK [ESP/EBP]", 960, "Call Stack & Activation Records (0x03C0 - 0x0400)"),
    BUFFER_SEG("BUFFER", 128, "I/O & String Destination Buffer (0x0080 - 0x00C0)"),
    LOW_MEM("LOW MEM", 0, "Interrupt Vectors & BIOS Data Area (0x0000 - 0x0040)")
}

/**
 * Tag model representing an x86 register pointing into this memory address.
 */
data class PointerTag(
    val regName: String,
    val role: String,
    val color: Color
)

/**
 * Scrollable Memory Table component that displays hexadecimal memory addresses and their contents,
 * updating automatically alongside CPU registers with live reactive highlighting, pointer radar markers,
 * in-place cell editing, and fast navigation.
 */
@Composable
fun ScrollableMemoryTable(
    memory: Map<Int, Int>,
    modifiedAddresses: Set<Int> = emptySet(),
    registers: Map<String, Int> = emptyMap(),
    onUpdateValue: ((address: Int, value: Int) -> Unit)? = null,
    onLoadAddressToRegister: ((regName: String, address: Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialAddress: Int = 96,
    height: Dp = 380.dp,
    cycleCount: Long = 0
) {
    var selectedFormat by remember { mutableStateOf(MemoryTableFormat.HEX_DWORD) }
    var selectedRegion by remember { mutableStateOf(MemoryRegionPreset.DATA_SEG) }
    var searchQuery by remember { mutableStateOf("") }
    var editingAddress by remember { mutableStateOf<Int?>(null) }
    var copiedAddress by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Pulsing animation for modified memory locations
    val infiniteTransition = rememberInfiniteTransition(label = "memPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "memPulseAlpha"
    )

    // Build contiguous 4-byte aligned address sequence covering common RAM regions + allocated addresses
    val allAddresses = remember(memory.keys, selectedRegion) {
        val baseAddresses = when (selectedRegion) {
            MemoryRegionPreset.DATA_SEG -> (64..256 step 4).toList()
            MemoryRegionPreset.STACK_SEG -> (940..1024 step 4).toList()
            MemoryRegionPreset.BUFFER_SEG -> (112..208 step 4).toList()
            MemoryRegionPreset.LOW_MEM -> (0..80 step 4).toList()
        }
        val memoryAllocated = memory.keys.map { (it / 4) * 4 }
        (baseAddresses + memoryAllocated).distinct().sorted()
    }

    // Filter addresses if search query provided
    val displayedAddresses = remember(allAddresses, searchQuery) {
        if (searchQuery.isBlank()) {
            allAddresses
        } else {
            val queryClean = searchQuery.trim().lowercase()
            allAddresses.filter { addr ->
                val hexStr = "0x" + addr.toString(16).lowercase()
                val decStr = addr.toString()
                hexStr.contains(queryClean) || decStr.contains(queryClean)
            }
        }
    }

    // Helper: Determine which registers point to a given aligned 4-byte address
    fun getPointersForAddress(address: Int): List<PointerTag> {
        val tags = mutableListOf<PointerTag>()
        registers.forEach { (name, regVal) ->
            if (regVal in address..(address + 3)) {
                val tagColor = when (name.uppercase()) {
                    "ESP" -> CyberCyan
                    "EBP" -> AlertOrange
                    "ESI" -> NeonGreen
                    "EDI" -> Color(0xFF8E24AA)
                    "EAX" -> Color(0xFF007ACC)
                    else -> MutedText
                }
                val role = when (name.uppercase()) {
                    "ESP" -> "Stack Top"
                    "EBP" -> "Frame Base"
                    "ESI" -> "Src"
                    "EDI" -> "Dest"
                    else -> "Ptr"
                }
                tags.add(PointerTag(name.uppercase(), role, tagColor))
            }
        }
        return tags
    }

    fun jumpToAddress(targetAddr: Int) {
        val aligned = (targetAddr / 4) * 4
        val targetIndex = displayedAddresses.indexOfFirst { it >= aligned }
        if (targetIndex != -1) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIndex.coerceAtLeast(0))
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("scrollable_memory_table"),
        colors = CardDefaults.cardColors(containerColor = TerminalBlack),
        border = BorderStroke(1.dp, TerminalLightGray),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- TOP CONTROL BAR ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = pulseAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAM MEMORY TABLE",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyberCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LIVE BUS",
                                color = CyberCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Format selector pills
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            MemoryTableFormat.HEX_DWORD,
                            MemoryTableFormat.DECIMAL_SIGNED,
                            MemoryTableFormat.BYTE_SPLIT,
                            MemoryTableFormat.ASCII
                        ).forEach { fmt ->
                            val isSel = selectedFormat == fmt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) CyberCyan.copy(alpha = 0.2f) else TerminalBlack)
                                    .border(
                                        BorderStroke(1.dp, if (isSel) CyberCyan else TerminalLightGray),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedFormat = fmt }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = fmt.label,
                                    color = if (isSel) CyberCyan else MutedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- POINTER QUICK RADAR CHIPS ---
                // Shows registers that currently point into memory (ESP, EBP, ESI, EDI, EAX)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "REGISTER POINTERS:",
                        color = MutedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val activePointerRegs = listOf("ESP", "EBP", "ESI", "EDI", "EAX", "EBX")
                        items(activePointerRegs) { regName ->
                            val ptrVal = registers[regName]
                            if (ptrVal != null && ptrVal > 0) {
                                val tagColor = when (regName) {
                                    "ESP" -> CyberCyan
                                    "EBP" -> AlertOrange
                                    "ESI" -> NeonGreen
                                    "EDI" -> Color(0xFF8E24AA)
                                    else -> PureWhite
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { jumpToAddress(ptrVal) }
                                        .testTag("chip_jump_ptr_$regName"),
                                    color = tagColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, tagColor.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$regName: 0x${ptrVal.toString(16).uppercase().padStart(4, '0')}",
                                            color = tagColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = tagColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- REGION TABS & SEARCH INPUT ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(MemoryRegionPreset.values()) { region ->
                            val isSel = selectedRegion == region
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) NeonGreen.copy(alpha = 0.15f) else TerminalDarkGray)
                                    .border(
                                        BorderStroke(1.dp, if (isSel) NeonGreen else TerminalLightGray),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        selectedRegion = region
                                        jumpToAddress(region.startAddress)
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = region.label,
                                    color = if (isSel) NeonGreen else MutedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Quick Search / Jump Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Jump 0x...", fontSize = 9.sp, color = MutedText) },
                        textStyle = TextStyle(
                            color = PureWhite,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .width(110.dp)
                            .height(34.dp)
                            .testTag("input_memory_jump"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = TerminalLightGray,
                            focusedContainerColor = TerminalBlack,
                            unfocusedContainerColor = TerminalBlack
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val clean = searchQuery.trim()
                                val target = if (clean.startsWith("0x", ignoreCase = true)) {
                                    clean.substring(2).toIntOrNull(16)
                                } else {
                                    clean.toIntOrNull()
                                }
                                if (target != null) jumpToAddress(target)
                            }
                        )
                    )
                }
            }

            HorizontalDivider(color = TerminalLightGray, thickness = 1.dp)

            // --- STICKY TABLE COLUMN HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray.copy(alpha = 0.8f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ADDRESS",
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(90.dp)
                )

                Text(
                    text = when (selectedFormat) {
                        MemoryTableFormat.HEX_DWORD -> "HEX CONTENT (32-BIT)"
                        MemoryTableFormat.DECIMAL_SIGNED -> "DECIMAL SIGNED"
                        MemoryTableFormat.DECIMAL_UNSIGNED -> "DECIMAL UNSIGNED"
                        MemoryTableFormat.BYTE_SPLIT -> "BYTES [B0 B1 B2 B3]"
                        MemoryTableFormat.ASCII -> "ASCII [CHAR DUMP]"
                    },
                    color = PureWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "PTR RADAR",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(75.dp),
                    textAlign = TextAlign.End
                )
            }

            HorizontalDivider(color = TerminalLightGray, thickness = 1.dp)

            // --- SCROLLABLE TABLE BODY ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(TerminalBlack)
            ) {
                if (displayedAddresses.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No addresses match search query.",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("memory_table_list")
                    ) {
                        items(displayedAddresses) { address ->
                            val dwordValue = memory[address] ?: 0
                            val isModified = modifiedAddresses.contains(address) ||
                                    modifiedAddresses.contains(address + 1) ||
                                    modifiedAddresses.contains(address + 2) ||
                                    modifiedAddresses.contains(address + 3)

                            val pointers = getPointersForAddress(address)

                            MemoryTableRow(
                                address = address,
                                dwordValue = dwordValue,
                                format = selectedFormat,
                                isModified = isModified,
                                pulseAlpha = pulseAlpha,
                                pointerTags = pointers,
                                onClick = { editingAddress = address },
                                onCopy = {
                                    val hexAddr = "0x" + address.toString(16).uppercase().padStart(8, '0')
                                    clipboardManager.setText(AnnotatedString(hexAddr))
                                    copiedAddress = hexAddr
                                }
                            )
                            HorizontalDivider(
                                color = TerminalLightGray.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            // --- BOTTOM SUMMARY BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayedAddresses.size} memory cells displayed (Little-Endian IA-32)",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (modifiedAddresses.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${modifiedAddresses.size} modified",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // --- IN-PLACE MEMORY CELL INSPECTION & EDIT MODAL ---
    editingAddress?.let { addrToEdit ->
        val currentDword = memory[addrToEdit] ?: 0
        MemoryCellEditDialog(
            address = addrToEdit,
            currentValue = currentDword,
            onDismiss = { editingAddress = null },
            onSave = { newVal ->
                onUpdateValue?.invoke(addrToEdit, newVal)
                editingAddress = null
            },
            onLoadToRegister = { regName ->
                onLoadAddressToRegister?.invoke(regName, addrToEdit)
            }
        )
    }
}

/**
 * Individual Row inside the Scrollable Memory Table.
 */
@Composable
private fun MemoryTableRow(
    address: Int,
    dwordValue: Int,
    format: MemoryTableFormat,
    isModified: Boolean,
    pulseAlpha: Float,
    pointerTags: List<PointerTag>,
    onClick: () -> Unit,
    onCopy: () -> Unit
) {
    val hexAddrStr = "0x" + address.toString(16).uppercase().padStart(8, '0')
    val hexValStr = "0x" + dwordValue.toUInt().toString(16).uppercase().padStart(8, '0')

    val b0 = dwordValue and 0xFF
    val b1 = (dwordValue ushr 8) and 0xFF
    val b2 = (dwordValue ushr 16) and 0xFF
    val b3 = (dwordValue ushr 24) and 0xFF

    val isNonZero = dwordValue != 0

    val rowBg = when {
        isModified -> NeonGreen.copy(alpha = 0.12f * pulseAlpha)
        pointerTags.isNotEmpty() -> pointerTags.first().color.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("row_mem_0x${address.toString(16)}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Hexadecimal Address Column
        Row(
            modifier = Modifier.width(90.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hexAddrStr,
                color = if (isModified) NeonGreen else CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // 2. Memory Contents (based on selected format)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (format) {
                MemoryTableFormat.HEX_DWORD -> {
                    Text(
                        text = hexValStr,
                        color = if (isNonZero) PureWhite else MutedText.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isModified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "UPDATED",
                                color = NeonGreen,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                MemoryTableFormat.DECIMAL_SIGNED -> {
                    Text(
                        text = "$dwordValue",
                        color = if (isNonZero) PureWhite else MutedText.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }

                MemoryTableFormat.DECIMAL_UNSIGNED -> {
                    Text(
                        text = dwordValue.toUInt().toString(),
                        color = if (isNonZero) PureWhite else MutedText.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }

                MemoryTableFormat.BYTE_SPLIT -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(b0, b1, b2, b3).forEachIndexed { idx, byteVal ->
                            val byteHex = byteVal.toString(16).uppercase().padStart(2, '0')
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (byteVal != 0) CyberCyan.copy(alpha = 0.12f) else TerminalDarkGray)
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = byteHex,
                                    color = if (byteVal != 0) PureWhite else MutedText.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (byteVal != 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                MemoryTableFormat.ASCII -> {
                    val asciiChars = listOf(b0, b1, b2, b3).map { b ->
                        if (b in 32..126) b.toChar().toString() else "·"
                    }.joinToString(" ")

                    Text(
                        text = asciiChars,
                        color = if (isNonZero) AlertOrange else MutedText.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Register Pointer Radar Tags (ESP, EBP, ESI, EDI)
        Row(
            modifier = Modifier.width(75.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pointerTags.isNotEmpty()) {
                val tag = pointerTags.first()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tag.color.copy(alpha = 0.2f))
                        .border(1.dp, tag.color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag.regName,
                        color = tag.color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Detailed In-Place Memory Cell Inspection & Editor Modal.
 */
@Composable
private fun MemoryCellEditDialog(
    address: Int,
    currentValue: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onLoadToRegister: ((String) -> Unit)? = null
) {
    var inputStr by remember { mutableStateOf("0x" + currentValue.toUInt().toString(16).uppercase()) }
    var isHexMode by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val hexAddress = "0x" + address.toString(16).uppercase().padStart(8, '0')

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, TerminalLightGray, RoundedCornerShape(12.dp))
                .testTag("dialog_memory_cell_edit"),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Memory Cell Inspector",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = PureWhite)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Address info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                    border = BorderStroke(1.dp, TerminalLightGray)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PHYSICAL ADDRESS:", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text(hexAddress, color = CyberCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DECIMAL ADDR:", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("$address (Bytes 0..3)", color = PureWhite, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-byte breakdown (Little-Endian)
                Text(
                    text = "LITTLE-ENDIAN BYTE SLICES:",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val b0 = currentValue and 0xFF
                    val b1 = (currentValue ushr 8) and 0xFF
                    val b2 = (currentValue ushr 16) and 0xFF
                    val b3 = (currentValue ushr 24) and 0xFF

                    listOf(
                        "+0 (B0)" to b0,
                        "+1 (B1)" to b1,
                        "+2 (B2)" to b2,
                        "+3 (B3)" to b3
                    ).forEach { (label, byteVal) ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp)),
                            color = TerminalDarkGray,
                            border = BorderStroke(1.dp, TerminalLightGray)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(label, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = byteVal.toString(16).uppercase().padStart(2, '0'),
                                    color = if (byteVal != 0) NeonGreen else PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input field
                OutlinedTextField(
                    value = inputStr,
                    onValueChange = {
                        inputStr = it
                        errorText = null
                    },
                    label = { Text(if (isHexMode) "Hex Value (0x... or 2A)" else "Decimal Value", color = MutedText) },
                    textStyle = TextStyle(color = PureWhite, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null,
                    supportingText = errorText?.let { { Text(it, color = BrightPink) } },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = TerminalLightGray
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Mode toggle and shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isHexMode = !isHexMode }
                    ) {
                        Checkbox(checked = isHexMode, onCheckedChange = { isHexMode = it })
                        Text("Hex Input Mode", color = PureWhite, fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { inputStr = "0x00000000" },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalDarkGray),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Zero", color = PureWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                val current = currentValue
                                inputStr = "0x" + (current + 1).toUInt().toString(16).uppercase()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalDarkGray),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("+1", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TerminalLightGray)
                    ) {
                        Text("Cancel", color = PureWhite)
                    }

                    Button(
                        onClick = {
                            val clean = inputStr.trim()
                            val parsed = if (clean.startsWith("0x", ignoreCase = true)) {
                                clean.substring(2).toLongOrNull(16)?.toInt()
                            } else if (clean.endsWith("h", ignoreCase = true)) {
                                clean.dropLast(1).toLongOrNull(16)?.toInt()
                            } else if (isHexMode) {
                                clean.toLongOrNull(16)?.toInt()
                            } else {
                                clean.toIntOrNull()
                            }

                            if (parsed == null) {
                                errorText = "Invalid number format. Use e.g. 0x2A or 42."
                            } else {
                                onSave(parsed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Write to RAM", color = TerminalBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
