package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SIBAddressExpression
import com.example.ui.theme.*

/**
 * Interactive Memory Addressing & SIB Formula Lab.
 * Teaches direct memory, indirect addressing, scaled-indexed modes,
 * and the crucial difference between MOV and LEA.
 */
@Composable
fun X86MemoryAddressingLab(
    modifier: Modifier = Modifier
) {
    // Memory state (Address -> Value)
    var memoryCells by remember {
        mutableStateOf(
            mapOf(
                100 to 42,
                104 to 99,
                108 to 15,
                112 to 256,
                116 to 88,
                120 to 777
            )
        )
    }

    // SIB Expression parameters
    var selectedBaseName by remember { mutableStateOf("RBX") }
    var baseAddress by remember { mutableStateOf(100) }

    var selectedIndexName by remember { mutableStateOf("RCX") }
    var indexValue by remember { mutableStateOf(2) }

    var selectedScale by remember { mutableStateOf(4) }
    var selectedDisplacement by remember { mutableStateOf(0) }

    val sib = remember(selectedBaseName, baseAddress, selectedIndexName, indexValue, selectedScale, selectedDisplacement) {
        SIBAddressExpression(
            baseName = selectedBaseName,
            baseValue = baseAddress,
            indexName = selectedIndexName,
            indexValue = indexValue,
            scale = selectedScale,
            displacement = selectedDisplacement
        )
    }

    val targetAddress = sib.effectiveAddress
    val memoryValue = memoryCells[targetAddress]

    // Selected comparison instruction: MOV vs LEA
    var activeOpcode by remember { mutableStateOf("MOV") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Header Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Memory",
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MEMORY OPERATIONS & SIB LAB",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "x86 CPUs calculate complex effective addresses in hardware using the formula: [Base + Index * Scale + Disp].",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Live SIB Formula Display ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalBlack),
            border = BorderStroke(1.dp, CyberCyan)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE EFFECTIVE ADDRESS EXPRESSION",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = sib.expressionString,
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Math Breakdown
                Text(
                    text = "Calculation: $baseAddress + ($indexValue * $selectedScale) + $selectedDisplacement",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "= $targetAddress decimal (0x${targetAddress.toString(16).uppercase()} hex)",
                    color = NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Interactive SIB Parameter Controls ---
        Text(
            text = "CONFIGURE ADDRESS OPERANDS",
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
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Base Register
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Base Register:", color = PureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Triple("RBX", 100, "Base Array (100)"),
                            Triple("RBP", 104, "Stack Frame (104)"),
                            Triple("RDI", 108, "Dest Buffer (108)")
                        ).forEach { (name, addr, _) ->
                            val isSel = selectedBaseName == name
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedBaseName = name
                                    baseAddress = addr
                                },
                                label = { Text("$name ($addr)", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan,
                                    selectedLabelColor = TerminalBlack,
                                    containerColor = TerminalBlack,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }

                // 2. Index Register & Value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Index ($selectedIndexName):", color = PureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 1, 2, 3).forEach { idx ->
                            val isSel = indexValue == idx
                            FilterChip(
                                selected = isSel,
                                onClick = { indexValue = idx },
                                label = { Text("i = $idx", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreen,
                                    selectedLabelColor = TerminalBlack,
                                    containerColor = TerminalBlack,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }

                // 3. Scale Factor (1, 2, 4, 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scale Multiplier:", color = PureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Pair(1, "1 (Byte)"),
                            Pair(2, "2 (Word)"),
                            Pair(4, "4 (DWord)"),
                            Pair(8, "8 (QWord)")
                        ).forEach { (sc, label) ->
                            val isSel = selectedScale == sc
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedScale = sc },
                                label = { Text("x$sc", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertOrange,
                                    selectedLabelColor = TerminalBlack,
                                    containerColor = TerminalBlack,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }

                // 4. Displacement Offset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Displacement:", color = PureWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 4, 8, -4).forEach { disp ->
                            val isSel = selectedDisplacement == disp
                            val label = if (disp >= 0) "+$disp" else "$disp"
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedDisplacement = disp },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PureWhite,
                                    selectedLabelColor = TerminalBlack,
                                    containerColor = TerminalBlack,
                                    labelColor = PureWhite
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Visual RAM Memory Grid ---
        Text(
            text = "PHYSICAL RAM GRID (32-BIT CELLS)",
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
            Column(modifier = Modifier.padding(14.dp)) {
                memoryCells.toSortedMap().forEach { (addr, value) ->
                    val isTarget = addr == targetAddress
                    val borderColor = if (isTarget) NeonGreen else TerminalLightGray
                    val bgColor = if (isTarget) NeonGreen.copy(alpha = 0.15f) else TerminalBlack

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .border(BorderStroke(if (isTarget) 2.dp else 1.dp, borderColor), RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isTarget) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = "Target",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "RAM[0x${addr.toString(16).uppercase()}] ($addr)",
                                color = if (isTarget) NeonGreen else CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$value (0x${value.toString(16).uppercase()})",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            if (isTarget) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = NeonGreen,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "REFERENCED",
                                        color = TerminalBlack,
                                        fontSize = 9.sp,
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MOV vs LEA Contrast Interactive Experiment ---
        Text(
            text = "MOV VS LEA: THE CRITICAL DIFFERENCE",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeOpcode = "MOV" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeOpcode == "MOV") CyberCyan else TerminalBlack,
                            contentColor = if (activeOpcode == "MOV") TerminalBlack else PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_select_mov")
                    ) {
                        Text("MOV EAX, ${sib.expressionString}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { activeOpcode = "LEA" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeOpcode == "LEA") NeonGreen else TerminalBlack,
                            contentColor = if (activeOpcode == "LEA") TerminalBlack else PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_select_lea")
                    ) {
                        Text("LEA EAX, ${sib.expressionString}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (activeOpcode == "MOV") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.1f))
                            .border(BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "RESULT: EAX = ${memoryValue ?: "Undefined (RAM unmapped)"}",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• MOV dereferenced address $targetAddress and loaded the data stored inside that memory cell into EAX.",
                                color = PureWhite,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• In C equivalent: int eax = *(int*)($targetAddress);",
                                color = MutedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonGreen.copy(alpha = 0.1f))
                            .border(BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "RESULT: EAX = $targetAddress (Address Math Only!)",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• LEA (Load Effective Address) computed the address $baseAddress + ($indexValue * $selectedScale) + $selectedDisplacement = $targetAddress and stored the number $targetAddress into EAX directly.",
                                color = PureWhite,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• RAM WAS NEVER TOUCHED! (No bus read, no memory cycle).",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "• In C equivalent: int* eax = &array[i];",
                                color = MutedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
