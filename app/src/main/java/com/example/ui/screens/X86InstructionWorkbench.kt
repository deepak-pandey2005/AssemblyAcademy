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
import androidx.compose.material.icons.automirrored.filled.Undo
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
import com.example.model.Architecture
import com.example.simulator.AssemblySimulator
import com.example.simulator.CpuState
import com.example.ui.editor.X86SyntaxHighlighter
import com.example.ui.theme.*

data class InstructionScenario(
    val id: String,
    val title: String,
    val tag: String,
    val description: String,
    val code: String
)

val X86_SCENARIOS = listOf(
    InstructionScenario(
        id = "drill_mov_basic",
        title = "1. Basic MOV & Register Transfer",
        tag = "MOV",
        description = "Immediate loading into registers and register-to-register copies.",
        code = """
            MOV EAX, 42         ; Immediate to register
            MOV EBX, 100        ; Load EBX
            MOV ECX, EAX        ; Reg to reg copy (ECX = 42)
            ADD ECX, EBX        ; ECX = 42 + 100 = 142
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_mem_deref",
        title = "2. Reading & Writing Memory [addr]",
        tag = "MEMORY",
        description = "Using square brackets to read from and write to physical RAM.",
        code = """
            MOV EAX, [100]      ; Read 42 from RAM address 100
            ADD EAX, 10         ; EAX becomes 52
            MOV [104], EAX      ; Store 52 into RAM at address 104
            MOV EDX, [104]      ; Verify read from address 104
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_mem_to_mem",
        title = "3. Illegal Mem-to-Mem & 2-Step Pattern",
        tag = "RULE",
        description = "Why MOV [mem], [mem] fails and how to copy via a scratch register.",
        code = """
            ; ILLEGAL: MOV [108], [100] -> Fails in x86 hardware!
            MOV EAX, [100]      ; Step 1: Read source memory into scratch EAX
            MOV [108], EAX      ; Step 2: Write scratch EAX to target address 108
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_lea_vs_mov",
        title = "4. LEA vs MOV Comparison",
        tag = "LEA",
        description = "Address arithmetic (LEA) vs dereferencing memory data (MOV).",
        code = """
            MOV EBX, 100        ; Base pointer to RAM address 100
            MOV EAX, [EBX]      ; MOV: Dereferences RAM -> loads 42
            LEA EDX, [EBX + 16] ; LEA: Calculates 100 + 16 = 116 (No RAM access!)
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_inplace_arith",
        title = "5. In-Place Memory Arithmetic",
        tag = "ALU",
        description = "Performing ADD and INC directly on RAM memory cells.",
        code = """
            MOV EAX, 5          ; Value to add
            ADD [100], EAX      ; Memory at 100 becomes 42 + 5 = 47!
            INC [100]           ; Memory at 100 increments to 48!
            MOV EBX, [100]      ; Load final memory value into EBX
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_stack_ops",
        title = "6. Stack Operations: PUSH & POP",
        tag = "STACK",
        description = "LIFO stack mechanics and ESP stack pointer decrement/increment.",
        code = """
            MOV EAX, 500
            MOV EBX, 900
            PUSH EAX            ; ESP drops by 4, 500 pushed
            PUSH EBX            ; ESP drops by 4, 900 pushed
            POP ECX             ; ECX = 900 (last pushed, LIFO!)
            POP EDX             ; EDX = 500 (first pushed)
        """.trimIndent()
    ),
    InstructionScenario(
        id = "drill_xor_zero",
        title = "7. XOR EAX, EAX Zeroing Idiom",
        tag = "XOR",
        description = "2-byte zeroing idiom recognized directly by the CPU rename stage.",
        code = """
            MOV EAX, 0x12345678 ; Load test value
            XOR EAX, EAX        ; Cleared to 0, sets Zero Flag ZF=1!
        """.trimIndent()
    )
)

@Composable
fun X86InstructionWorkbench(
    modifier: Modifier = Modifier
) {
    var selectedScenario by remember { mutableStateOf(X86_SCENARIOS.first()) }
    var codeLines by remember(selectedScenario) { mutableStateOf(selectedScenario.code.trimIndent().lines()) }
    
    var cpuState by remember(selectedScenario) {
        mutableStateOf(AssemblySimulator.createInitialState(Architecture.X86))
    }
    val history = remember(selectedScenario) { mutableStateListOf<CpuState>() }
    var modifiedRegs by remember(selectedScenario) { mutableStateOf<Set<String>>(emptySet()) }
    var previousRegs by remember(selectedScenario) { mutableStateOf<Map<String, Int>>(emptyMap()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Header ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Workbench",
                        tint = CyberCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "x86 INSTRUCTION STEP-THROUGH LAB",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Step through curated instruction patterns line-by-line and inspect register, memory, and flag state mutations in real time.",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Scenario Selector Horizontal Scroll ---
        Text(
            text = "SELECT INSTRUCTION DRILL",
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
            X86_SCENARIOS.forEach { scenario ->
                val isSelected = scenario.id == selectedScenario.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedScenario = scenario
                        codeLines = scenario.code.trimIndent().lines()
                        cpuState = AssemblySimulator.createInitialState(Architecture.X86)
                        history.clear()
                    },
                    label = {
                        Text(
                            text = scenario.tag,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = TerminalBlack,
                        containerColor = TerminalDarkGray,
                        labelColor = PureWhite
                    ),
                    border = BorderStroke(1.dp, if (isSelected) CyberCyan else TerminalLightGray),
                    modifier = Modifier.testTag("scenario_chip_${scenario.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Scenario Description Card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = selectedScenario.title,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedScenario.description,
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Code Viewer & Active Instruction Pointer Highlight ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalBlack),
            border = BorderStroke(1.dp, TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                codeLines.forEachIndexed { index, line ->
                    val isActive = index == cpuState.ip
                    val bgColor = if (isActive) NeonGreen.copy(alpha = 0.15f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(bgColor)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isActive) "▶ ${index + 1}" else "  ${index + 1}",
                            color = if (isActive) NeonGreen else MutedText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.width(36.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = X86SyntaxHighlighter.highlight(line, X86SyntaxHighlighter.DarkTerminalTheme),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Execution Step Explanation Box ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, CyberCyan)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = cpuState.explanation,
                    color = PureWhite,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Step Controls: Forward, Backward, Reset ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Backward
            OutlinedButton(
                onClick = {
                    if (history.isNotEmpty()) {
                        val prev = history.removeAt(history.size - 1)
                        cpuState = prev
                    }
                },
                enabled = history.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_step_backward"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                border = BorderStroke(1.dp, if (history.isNotEmpty()) CyberCyan else TerminalLightGray)
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            // Reset
            OutlinedButton(
                onClick = {
                    cpuState = AssemblySimulator.createInitialState(Architecture.X86)
                    history.clear()
                    modifiedRegs = emptySet()
                    previousRegs = emptyMap()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_step_reset"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertOrange),
                border = BorderStroke(1.dp, AlertOrange)
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            // Step Forward
            Button(
                onClick = {
                    history.add(cpuState)
                    val nextState = AssemblySimulator.executeStep(cpuState, codeLines)
                    val changed = mutableSetOf<String>()
                    nextState.registers.forEach { (reg, v) ->
                        if ((cpuState.registers[reg] ?: 0) != v) {
                            changed.add(reg)
                        }
                    }
                    previousRegs = cpuState.registers
                    modifiedRegs = changed
                    cpuState = nextState
                },
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("btn_step_forward"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = TerminalBlack)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Step", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Step Line", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Live Register File Display ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIVE REGISTERS",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (modifiedRegs.isNotEmpty()) {
                Surface(
                    color = AlertOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, AlertOrange)
                ) {
                    Text(
                        text = "${modifiedRegs.size} MODIFIED",
                        color = AlertOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
            border = BorderStroke(1.dp, if (modifiedRegs.isNotEmpty()) AlertOrange.copy(alpha = 0.6f) else TerminalLightGray)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                listOf(
                    Pair("EAX", cpuState.registers["EAX"] ?: 0),
                    Pair("EBX", cpuState.registers["EBX"] ?: 0),
                    Pair("ECX", cpuState.registers["ECX"] ?: 0),
                    Pair("EDX", cpuState.registers["EDX"] ?: 0),
                    Pair("ESP", cpuState.registers["ESP"] ?: 1000)
                ).chunked(2).forEach { rowRegs ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowRegs.forEach { (name, value) ->
                            val isMod = modifiedRegs.contains(name)
                            val prev = previousRegs[name]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMod) TerminalDarkGray else TerminalBlack)
                                    .border(
                                        BorderStroke(
                                            if (isMod) 1.5.dp else 1.dp,
                                            if (isMod) AlertOrange else TerminalLightGray
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = name,
                                            color = if (isMod) AlertOrange else CyberCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                        if (isMod && prev != null && prev != value) {
                                            val d = value - prev
                                            val dStr = if (d > 0) "+$d" else "$d"
                                            Text(
                                                text = "Δ $dStr (was $prev)",
                                                color = AlertOrange,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$value",
                                            color = if (isMod) NeonGreen else PureWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "0x" + value.toString(16).uppercase(),
                                            color = MutedText,
                                            fontSize = 9.sp,
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

        // --- Live Memory View ---
        Text(
            text = "LIVE RAM STATE",
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
            Column(modifier = Modifier.padding(12.dp)) {
                listOf(100, 104, 108, 112).forEach { addr ->
                    val memVal = cpuState.memory[addr] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TerminalBlack)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RAM[0x${addr.toString(16).uppercase()}] ($addr)", color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("$memVal", color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
