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
import com.example.model.X86CurriculumRepository
import com.example.model.X86RegisterInfo
import com.example.ui.theme.*

/**
 * Interactive Register Bit Visualizer for teaching 64-bit Quadword, 32-bit Doubleword,
 * 16-bit Word, and 8-bit Byte register slicing and the crucial x86-64 zero-extension rule.
 */
@Composable
fun X86RegisterBitExplorer(
    modifier: Modifier = Modifier
) {
    val catalog = X86CurriculumRepository.REGISTER_CATALOG
    var selectedRegister by remember { mutableStateOf(catalog.first()) }
    
    // Store 64-bit register value as Long
    var regValue by remember { mutableStateOf(42L) } // 42 in hex (0x2A)
    var lastActionLog by remember { mutableStateOf("Initial state: Register loaded with 42 (0x2A).") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Header Section ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Registers",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "x86-64 REGISTER SLICING INSPECTOR",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Understand how 64-bit, 32-bit, 16-bit, and 8-bit sub-registers share the same physical silicon transistors.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Register Selector Chips ---
        Text(
            text = "SELECT GENERAL-PURPOSE REGISTER",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            catalog.forEach { reg ->
                val isSelected = reg.name64 == selectedRegister.name64
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedRegister = reg
                        lastActionLog = "Selected ${reg.name64} (${reg.primaryRole})."
                    },
                    label = {
                        Text(
                            text = reg.name64,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = TerminalBlack,
                        containerColor = TerminalDarkGray,
                        labelColor = PureWhite
                    ),
                    border = BorderStroke(1.dp, if (isSelected) NeonGreen else TerminalLightGray),
                    modifier = Modifier.testTag("reg_chip_${reg.name64}")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Register Role & Calling Convention Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedRegister.name64} ARCHITECTURAL ROLE",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        color = CyberCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = selectedRegister.callingConvention,
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Primary Role: ${selectedRegister.primaryRole}",
                    color = MutedText,
                    fontSize = 12.sp
                )
                Text(
                    text = "• Preserved Across Calls: ${selectedRegister.preserveRule}",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Visual Bit Hierarchy Slicing Map ---
        Text(
            text = "BIT SLICING DIAGRAM: ${selectedRegister.name64}",
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
                // Tier 1: 64-bit Quadword
                RegisterSliceBar(
                    label = selectedRegister.name64,
                    bitRange = "Bits [63 .. 0] (Quadword / 8 Bytes)",
                    valueHex = "0x" + "%016X".format(regValue),
                    containerColor = CyberCyan.copy(alpha = 0.12f),
                    borderColor = CyberCyan,
                    textColor = CyberCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 2: 32-bit Doubleword (Lower half of 64-bit)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerminalBlack.copy(alpha = 0.4f))
                            .border(BorderStroke(1.dp, TerminalLightGray.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[63..32] Upper DWord",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        RegisterSliceBar(
                            label = selectedRegister.name32,
                            bitRange = "Bits [31 .. 0] (DWord)",
                            valueHex = "0x" + "%08X".format(regValue and 0xFFFFFFFFL),
                            containerColor = NeonGreen.copy(alpha = 0.15f),
                            borderColor = NeonGreen,
                            textColor = NeonGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 3: 16-bit Word (Lower 16 bits)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(2f)) {
                        RegisterSliceBar(
                            label = selectedRegister.name16,
                            bitRange = "Bits [15 .. 0] (Word)",
                            valueHex = "0x" + "%04X".format(regValue and 0xFFFFL),
                            containerColor = AlertOrange.copy(alpha = 0.15f),
                            borderColor = AlertOrange,
                            textColor = AlertOrange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tier 4: 8-bit Bytes (AH and AL)
                val high8Name = selectedRegister.nameHigh8
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(2f))
                    if (high8Name != null) {
                        Box(modifier = Modifier.weight(1f)) {
                            RegisterSliceBar(
                                label = high8Name,
                                bitRange = "[15..8] High",
                                valueHex = "0x" + "%02X".format((regValue shr 8) and 0xFFL),
                                containerColor = BrightPink.copy(alpha = 0.15f),
                                borderColor = BrightPink,
                                textColor = BrightPink
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RegisterSliceBar(
                            label = selectedRegister.nameLow8,
                            bitRange = "[7..0] Low",
                            valueHex = "0x" + "%02X".format(regValue and 0xFFL),
                            containerColor = PureWhite.copy(alpha = 0.15f),
                            borderColor = PureWhite,
                            textColor = PureWhite
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Action Log Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalBlack),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Log",
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lastActionLog,
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Interactive Modification Playground ---
        Text(
            text = "TEST INSTRUCTION MUTATIONS ON SILICON",
            color = MutedText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Mutation 1: Write to AL
            OutlinedButton(
                onClick = {
                    val newVal = (regValue and -256L) or 0xFFL
                    regValue = newVal
                    lastActionLog = "Executed 'MOV ${selectedRegister.nameLow8}, 0xFF': Modified bits [7..0]. Upper 56 bits remained strictly unchanged!"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_write_al"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Write to ${selectedRegister.nameLow8} (8-bit: 0xFF)", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Preserves [63..8]", color = MutedText, fontSize = 11.sp)
                }
            }

            // Mutation 2: Write to EAX (Demonstrate Zero Extension!)
            Button(
                onClick = {
                    val newVal = 0x12345678L
                    regValue = newVal
                    lastActionLog = "ZERO-EXTENSION RULE TRIGGERED: 'MOV ${selectedRegister.name32}, 0x12345678' wrote to 32-bit register and AUTOMATICALLY ZEROED bits [63..32] of ${selectedRegister.name64}!"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_zero_extend_eax"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TerminalBlack)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Write to ${selectedRegister.name32} (32-bit: 0x12345678)", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Zero-Extends!", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // Mutation 3: Set full 64-bit value with upper bits
            OutlinedButton(
                onClick = {
                    regValue = 0x7EADBEEFCAFEF00DL
                    lastActionLog = "Executed 'MOV ${selectedRegister.name64}, 0x7EADBEEFCAFEF00D': All 64 bits loaded with test hex pattern."
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_write_rax"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Load 64-bit Hex Pattern", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Sets all 64 bits", color = MutedText, fontSize = 11.sp)
                }
            }

            // Mutation 4: Clear with XOR
            OutlinedButton(
                onClick = {
                    regValue = 0L
                    lastActionLog = "Executed 'XOR ${selectedRegister.name32}, ${selectedRegister.name32}': Zeroed register using 2-byte compiler idiom. Upper 32 bits also zeroed!"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_xor_clear"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertOrange),
                border = BorderStroke(1.dp, AlertOrange.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("XOR ${selectedRegister.name32}, ${selectedRegister.name32} (Zero)", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Text("Fast Clear", color = MutedText, fontSize = 11.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterSliceBar(
    label: String,
    bitRange: String,
    valueHex: String,
    containerColor: Color,
    borderColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = bitRange,
                    color = MutedText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = valueHex,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
