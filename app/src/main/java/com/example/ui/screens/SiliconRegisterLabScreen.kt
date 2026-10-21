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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AssemblyViewModel

/**
 * SiliconRegisterLabScreen: A ground-level interactive laboratory where users can
 * directly manipulate register values, flip bits, and follow guided experiments
 * to see how changing values fundamentally shifts CPU execution pathways, ALU gates,
 * and computer behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiliconRegisterLabScreen(
    viewModel: AssemblyViewModel,
    onNavigateToDebugger: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedLabTab by remember { mutableIntStateOf(0) } // 0: Guided Missions, 1: Hardware Sandbox

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- Lab Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalDarkGray)
                .drawBehind {
                    drawLine(
                        color = NeonGreen,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonGreen.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeveloperBoard,
                                    contentDescription = "Silicon Lab",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SILICON REGISTER LAB",
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Transistor Bits, ALU Logic & Cause-Effect",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Live Circuit Indicator
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, TerminalLightGray)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIVE ALU",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedLabTab,
                    containerColor = TerminalDarkGray,
                    contentColor = PureWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedLabTab]),
                            color = NeonGreen
                        )
                    }
                ) {
                    Tab(
                        selected = selectedLabTab == 0,
                        onClick = { selectedLabTab = 0 },
                        text = {
                            Text(
                                "GUIDED MISSIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        icon = {
                            Icon(Icons.Default.School, contentDescription = "Missions", modifier = Modifier.size(16.dp))
                        },
                        selectedContentColor = NeonGreen,
                        unselectedContentColor = MutedText,
                        modifier = Modifier.testTag("tab_guided_missions")
                    )
                    Tab(
                        selected = selectedLabTab == 1,
                        onClick = { selectedLabTab = 1 },
                        text = {
                            Text(
                                "GROUND-LEVEL SANDBOX",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        icon = {
                            Icon(Icons.Default.Tune, contentDescription = "Sandbox", modifier = Modifier.size(16.dp))
                        },
                        selectedContentColor = CyberCyan,
                        unselectedContentColor = MutedText,
                        modifier = Modifier.testTag("tab_silicon_sandbox")
                    )
                }
            }
        }

        // --- Main Content Area ---
        when (selectedLabTab) {
            0 -> GuidedMissionsView(onNavigateToDebugger = onNavigateToDebugger)
            1 -> GroundLevelSandboxView(onNavigateToDebugger = onNavigateToDebugger)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 0: GUIDED MISSIONS
// -------------------------------------------------------------------------------------------------

data class GuidedExperiment(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val primaryRegister: String,
    val secondaryRegister: String? = null,
    val instructionSnippet: String,
    val initialValA: Long,
    val initialValB: Long = 0L,
    val suggestedChanges: List<SuggestedValueOption>,
    val targetExplanation: String,
    val siliconHardwareNotes: String
)

data class SuggestedValueOption(
    val label: String,
    val valueA: Long,
    val valueB: Long = 0L,
    val description: String,
    val expectedOutcome: String
)

val EXPERIMENTS_LIST = listOf(
    GuidedExperiment(
        id = "exp_zero_flag",
        title = "Zero Flag & Conditional Branch Divergence",
        category = "Control Flow",
        summary = "Change EAX to match EBX and watch the Zero Flag (ZF) silicon gate flip from 0 to 1, causing the computer to take a completely different execution route!",
        primaryRegister = "EAX",
        secondaryRegister = "EBX",
        instructionSnippet = """
            CMP EAX, EBX
            JE  ACCESS_GRANTED
            ; Fallthrough (ZF == 0):
            CALL log_denial_event
            JMP exit
            ACCESS_GRANTED: ; (ZF == 1):
            CALL unlock_root_shell
        """.trimIndent(),
        initialValA = 5L,
        initialValB = 10L,
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "EAX = 5 (Default)",
                valueA = 5L,
                valueB = 10L,
                description = "Values mismatch. 5 - 10 = -5. ALU difference != 0.",
                expectedOutcome = "ZF = 0. Computer takes Access Denied branch."
            ),
            SuggestedValueOption(
                label = "EAX = 10 (MATCH)",
                valueA = 10L,
                valueB = 10L,
                description = "Values match perfectly! 10 - 10 = 0. 32-input NOR gate activates.",
                expectedOutcome = "ZF = 1! JE branches to ACCESS_GRANTED (Root Shell unlocked)!"
            ),
            SuggestedValueOption(
                label = "EAX = 100 (Greater)",
                valueA = 100L,
                valueB = 10L,
                description = "100 - 10 = +90. Difference != 0, but positive.",
                expectedOutcome = "ZF = 0, SF = 0. JG would branch, but JE does not!"
            )
        ),
        targetExplanation = "At the silicon level, the CMP instruction does an internal ALU subtraction (EAX - EBX) without saving the result to a register. The 32 output bits pass through a 32-input NOR gate. Only when ALL 32 bits are 0 does the NOR gate output HIGH (1), setting the Zero Flag (ZF). The JE (Jump if Equal) hardware unit directly monitors the ZF wire to decide whether to overwrite the Instruction Pointer (EIP).",
        siliconHardwareNotes = "Transistor Gate: 32-bit NOR Tree | Wire: ZF line to Instruction Decoder | Branch Unit: Multiplexer on EIP bus"
    ),
    GuidedExperiment(
        id = "exp_overflow_sign",
        title = "Integer 32-Bit Overflow & Sign Flip",
        category = "Arithmetic",
        summary = "Set EAX to maximum 32-bit positive integer (0x7FFFFFFF) and add 1. Watch the sign bit flip to negative and trigger the Overflow Flag (OF)!",
        primaryRegister = "EAX",
        instructionSnippet = """
            ; EAX starts at 0x7FFFFFFF (2,147,483,647)
            ADD EAX, 1
            ; Result: 0x80000000 (-2,147,483,648)
            JO  handle_arithmetic_overflow
            ; Normal execution if OF == 0...
        """.trimIndent(),
        initialValA = 0x7FFFFFFFL,
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "EAX = 0x7FFFFFFF",
                valueA = 0x7FFFFFFFL,
                description = "Max positive 32-bit signed integer. Bit 31 (sign) is 0.",
                expectedOutcome = "Safe range. Signed: +2,147,483,647. OF = 0, SF = 0."
            ),
            SuggestedValueOption(
                label = "ADD 1 -> 0x80000000",
                valueA = 0x80000000L,
                description = "Adding 1 causes carry to ripple into Bit 31, flipping sign to 1!",
                expectedOutcome = "OVERFLOW! Result: -2,147,483,648. OF = 1, SF = 1! Bank balance crashes!"
            ),
            SuggestedValueOption(
                label = "EAX = 0xFFFFFFFF (-1)",
                valueA = 0xFFFFFFFFL,
                description = "All 32 bits set to 1. In two's complement, this equals -1.",
                expectedOutcome = "Adding 1 produces 0x00000000 with CF = 1 (wraparound), ZF = 1."
            )
        ),
        targetExplanation = "x86 CPUs interpret 32-bit values as two's complement. Bit 31 is the Sign bit. When 0x7FFFFFFF (positive) has 1 added to it, the carries ripple all the way up into bit 31, flipping it to 1. The CPU detects that two positive numbers added together produced a negative number, raising the Overflow Flag (OF = 1). In game development and financial software, this bug has caused bank balances to flip from billionaire to deep debt!",
        siliconHardwareNotes = "Transistor Gate: CarryIn(31) XOR CarryOut(31) -> OF | Wire: Bit 31 tap -> SF"
    ),
    GuidedExperiment(
        id = "exp_bitmask_security",
        title = "Bitmasking, Hardware Flags & Permissions",
        category = "Bitwise Security",
        summary = "Inspect how changing individual bits in ECX grants or revokes Read, Write, Execute, and Superuser permissions via the TEST instruction.",
        primaryRegister = "ECX",
        instructionSnippet = """
            ; ECX = Permission Byte:
            ; Bit 0: READ, Bit 1: WRITE, Bit 2: EXECUTE, Bit 7: ADMIN
            TEST ECX, 0x80   ; Test Bit 7 (0b10000000)
            JZ   ACCESS_DENIED ; Branch if Bit 7 was 0 (ZF == 1)
            ; If Bit 7 was 1, ZF == 0 (Proceed to privileged kernel mode)
        """.trimIndent(),
        initialValA = 0x05L, // Read (1) + Execute (4) = 5
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "ECX = 0x05 (Standard User)",
                valueA = 0x05L,
                description = "Bits set: 0 (Read) and 2 (Exec). Bit 7 (Admin) is 0.",
                expectedOutcome = "TEST ECX, 0x80 produces 0. ZF = 1. JZ branches to ACCESS_DENIED."
            ),
            SuggestedValueOption(
                label = "ECX = 0x85 (Admin Granted)",
                valueA = 0x85L,
                description = "Bit 7 (Admin) is 1 (0x80) + Read (0x01) + Exec (0x04).",
                expectedOutcome = "TEST ECX, 0x80 produces 0x80 (!= 0). ZF = 0. Security check passed!"
            ),
            SuggestedValueOption(
                label = "ECX = 0x00 (No Perms)",
                valueA = 0x00L,
                description = "All permission bits cleared to 0.",
                expectedOutcome = "All TEST checks fail. ZF = 1. Complete lockdown."
            )
        ),
        targetExplanation = "Operating system kernels compress access control lists into bit flags. The TEST instruction performs a bitwise AND between the register and a mask (0x80 = 1000 0000b) without modifying the register, only updating CPU flags. If the tested bit is 0, the result is 0, so the Zero Flag (ZF) becomes 1. Changing a single bit in silicon flips the boolean outcome of the security gate.",
        siliconHardwareNotes = "Transistor Gate: Parallel 32-bit AND array | Output: Fed to Zero Detector NOR tree"
    ),
    GuidedExperiment(
        id = "exp_stack_hijack",
        title = "Return Address Corruption & Stack Hijacking",
        category = "Memory & Stack",
        summary = "Observe what happens at the register level when memory at the Stack Pointer (ESP) is altered. When RET executes, EIP is directly overwritten!",
        primaryRegister = "ESP",
        secondaryRegister = "EIP",
        instructionSnippet = """
            ; Normal Function Epilogue:
            MOV  ESP, EBP
            POP  EBP
            RET  ; Pops [ESP] directly into EIP!
            ; Normal Return Address: 0x00401050
            ; Corrupted Injected Addr: 0x00402000 (Shellcode)
        """.trimIndent(),
        initialValA = 0x00401050L,
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "Normal [ESP] = 0x00401050",
                valueA = 0x00401050L,
                description = "Standard return address pointing to safe caller function in main.",
                expectedOutcome = "RET loads EIP = 0x00401050. Program continues normally."
            ),
            SuggestedValueOption(
                label = "Injected [ESP] = 0x00402000",
                valueA = 0x00402000L,
                description = "Attacker overwrote the return address on the stack via buffer overflow.",
                expectedOutcome = "RET pops 0x00402000 into EIP! CPU jumps straight into secret payload!"
            ),
            SuggestedValueOption(
                label = "Corrupted [ESP] = 0x00000000",
                valueA = 0x00000000L,
                description = "Null pointer corrupted on the stack.",
                expectedOutcome = "RET loads EIP = 0x0. CPU triggers Page Fault / Segmentation Crash!"
            )
        ),
        targetExplanation = "The CPU does not know whether an address on the stack is legitimate or malicious. The RET (Return) instruction has one simple hardware circuit: MemoryBus.Read(ESP) -> DataBus -> EIP, followed by ESP = ESP + 4. By changing the 32-bit value residing in memory at the stack pointer, the computer's entire future code execution is instantly hijacked.",
        siliconHardwareNotes = "Hardware Bus: Memory Data Bus connects directly to Instruction Fetch Pointer (EIP)"
    ),
    GuidedExperiment(
        id = "exp_carry_wrap",
        title = "Carry Flag & 8-Bit AL Register Wraparound",
        category = "Micro-Architecture",
        summary = "Load AL (lower 8 bits of EAX) with 255 (0xFF). Add 1 to see the 9th bit spill out of the 8-bit bus into the Carry Flag (CF) while AL resets to 0!",
        primaryRegister = "AL",
        instructionSnippet = """
            MOV AL, 0xFF  ; 255 in 8-bit register
            ADD AL, 1     ; 255 + 1 = 256 (0x100)
            ; AL only holds 8 bits -> becomes 0x00!
            ; The 9th bit sets Carry Flag (CF = 1)
            JC  handle_wraparound
        """.trimIndent(),
        initialValA = 0xFFL,
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "AL = 0xFF (255)",
                valueA = 0xFFL,
                description = "Highest unsigned integer that fits into an 8-bit byte.",
                expectedOutcome = "AL = 0xFF. CF = 0, ZF = 0."
            ),
            SuggestedValueOption(
                label = "ADD 1 -> AL = 0x00 (Wrapped)",
                valueA = 0x100L,
                description = "256 cannot fit into 8 bits. AL drops to 0, Carry bit emitted.",
                expectedOutcome = "AL = 0x00, CF = 1 (Carry line raised), ZF = 1! JC branches!"
            ),
            SuggestedValueOption(
                label = "AL = 0x7F (127 Signed Max)",
                valueA = 0x7FL,
                description = "Highest signed 8-bit integer. Adding 1 flips bit 7 to negative (-128).",
                expectedOutcome = "AL = 0x80, OF = 1, SF = 1, CF = 0."
            )
        ),
        targetExplanation = "In the ALU adder, each bit position calculates: Sum = A XOR B XOR CarryIn. When adding 1 to 0xFF (1111 1111b), carries ripple through all 8 stages. The 8th stage emits CarryOut = 1, which latches into the CPU's Carry Flag flip-flop. The 8 data bus lines only capture the lower 8 sum bits, which are all 0.",
        siliconHardwareNotes = "Transistor Gate: 8-stage Ripple Carry Adder -> CarryOut latch"
    ),
    GuidedExperiment(
        id = "exp_ecx_loop",
        title = "Loop Counter Trap: ECX = 0 Underflow",
        category = "Loop & Branching",
        summary = "The x86 LOOP instruction decrements ECX and repeats if ECX != 0. Watch what happens if ECX starts at 0: it underflows to 4,294,967,295 iterations!",
        primaryRegister = "ECX",
        instructionSnippet = """
            ; LOOP instruction does: ECX = ECX - 1; JNZ loop_start
            loop_start:
                ; ... do work ...
                LOOP loop_start
            ; What happens if ECX starts at 0?
            ; First decrement: 0 - 1 = 0xFFFFFFFF (4.29 Billion times!)
        """.trimIndent(),
        initialValA = 3L,
        suggestedChanges = listOf(
            SuggestedValueOption(
                label = "ECX = 3 (Normal Loop)",
                valueA = 3L,
                description = "Loop repeats 3 times (ECX: 3 -> 2 -> 1 -> 0).",
                expectedOutcome = "Normal termination when ECX reaches 0. ZF = 1."
            ),
            SuggestedValueOption(
                label = "ECX = 0 (Underflow Trap!)",
                valueA = 0L,
                description = "ECX starts at 0. Before checking for 0, LOOP decrements ECX to -1!",
                expectedOutcome = "ECX becomes 0xFFFFFFFF! Computer enters a 4.29 billion iteration freeze!"
            ),
            SuggestedValueOption(
                label = "ECX = 1 (Single Pass)",
                valueA = 1L,
                description = "Loop executes exactly once, decrements to 0, and exits.",
                expectedOutcome = "ECX = 0, loop exits immediately."
            )
        ),
        targetExplanation = "Because the classic x86 LOOP instruction decrements before testing for zero, passing ECX = 0 does NOT execute 0 times—it wraps around to 0xFFFFFFFF (4,294,967,295) and runs 4.29 billion times! This is why modern compilers emit `TEST ECX, ECX; JZ exit` before entering loops.",
        siliconHardwareNotes = "Hardware Mechanism: Unchecked unsigned subtraction in CPU Micro-Sequencer"
    )
)

@Composable
fun GuidedMissionsView(
    onNavigateToDebugger: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeMissionIndex by remember { mutableIntStateOf(0) }
    val currentMission = EXPERIMENTS_LIST[activeMissionIndex]

    // Local mission state that user can manipulate
    var userValA by remember(currentMission.id) { mutableLongStateOf(currentMission.initialValA) }
    var userValB by remember(currentMission.id) { mutableLongStateOf(currentMission.initialValB) }
    var lastActionExpl by remember(currentMission.id) { mutableStateOf("Initial setup loaded. Choose a value change below to see how the computer responds.") }

    // Derived ground-level silicon calculations
    val (zf, cf, sf, of) = remember(userValA, userValB, currentMission.id) {
        when (currentMission.id) {
            "exp_zero_flag" -> {
                val diff = (userValA - userValB).toInt()
                val isZero = (diff == 0)
                val isSign = (diff < 0)
                val isCarry = (userValA < userValB)
                listOf(isZero, isCarry, isSign, false)
            }
            "exp_overflow_sign" -> {
                val a32 = userValA.toInt()
                val isSign = (a32 and 0x80000000.toInt()) != 0
                val isOver = (userValA == 0x80000000L)
                val isZero = (a32 == 0)
                listOf(isZero, userValA > 0xFFFFFFFFL, isSign, isOver)
            }
            "exp_bitmask_security" -> {
                val tested = (userValA and 0x80L)
                val isZero = (tested == 0L)
                listOf(isZero, false, false, false)
            }
            "exp_stack_hijack" -> {
                val isHijacked = (userValA == 0x00402000L)
                val isCrash = (userValA == 0L)
                listOf(!isHijacked && !isCrash, false, false, isCrash)
            }
            "exp_carry_wrap" -> {
                val al = (userValA and 0xFFL)
                val isCarry = (userValA > 0xFFL)
                val isZero = (al == 0L)
                val isSign = ((al and 0x80L) != 0L)
                listOf(isZero, isCarry, isSign, (userValA == 0x80L))
            }
            "exp_ecx_loop" -> {
                val isZero = (userValA == 0L)
                val isFreeze = (userValA == 0xFFFFFFFFL || userValA == 0L)
                listOf(isZero, false, isFreeze, false)
            }
            else -> listOf(false, false, false, false)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Mission Selector Carousel ---
        item {
            Text(
                text = "SELECT GUIDED INVESTIGATION",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EXPERIMENTS_LIST.size) { index ->
                    val exp = EXPERIMENTS_LIST[index]
                    val isSelected = index == activeMissionIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeMissionIndex = index },
                        label = {
                            Text(
                                text = "Mission ${index + 1}: ${exp.primaryRegister}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = TerminalBlack,
                            selectedLeadingIconColor = TerminalBlack,
                            containerColor = TerminalDarkGray,
                            labelColor = PureWhite
                        ),
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else TerminalLightGray),
                        modifier = Modifier.testTag("mission_chip_$index")
                    )
                }
            }
        }

        // --- Current Mission Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CyberCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentMission.category.uppercase(),
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "MISSION ${activeMissionIndex + 1} OF ${EXPERIMENTS_LIST.size}",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentMission.title,
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentMission.summary,
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // --- Interactive Value Mutation Section ("Change Values Here") ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Change Values",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEP 1: CHANGE VALUES (CHOOSE OR CUSTOMIZE)",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pick a preset below or use the buttons to guide the value change and observe the ground-level ripple:",
                        color = MutedText,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Buttons
                    currentMission.suggestedChanges.forEachIndexed { idx, opt ->
                        val isCurrentMatch = (userValA == opt.valueA && (currentMission.secondaryRegister == null || userValB == opt.valueB))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    userValA = opt.valueA
                                    if (currentMission.secondaryRegister != null) userValB = opt.valueB
                                    lastActionExpl = opt.description + " -> " + opt.expectedOutcome
                                }
                                .testTag("btn_suggested_val_$idx"),
                            color = if (isCurrentMatch) TerminalBlack else DarkBackground,
                            border = BorderStroke(
                                1.5.dp,
                                if (isCurrentMatch) NeonGreen else TerminalLightGray
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCurrentMatch,
                                    onClick = {
                                        userValA = opt.valueA
                                        if (currentMission.secondaryRegister != null) userValB = opt.valueB
                                        lastActionExpl = opt.description + " -> " + opt.expectedOutcome
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NeonGreen,
                                        unselectedColor = MutedText
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = opt.label,
                                        color = if (isCurrentMatch) NeonGreen else PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = opt.description,
                                        color = MutedText,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Outcome: ${opt.expectedOutcome}",
                                        color = if (isCurrentMatch) PureWhite else MutedText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Increment / Decrement & Invert Adjusters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userValA += 1
                                lastActionExpl = "Incremented ${currentMission.primaryRegister} by 1."
                            },
                            modifier = Modifier.weight(1f).testTag("btn_inc_a"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = BorderStroke(1.dp, TerminalLightGray)
                        ) {
                            Text("+1", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                userValA -= 1
                                lastActionExpl = "Decremented ${currentMission.primaryRegister} by 1."
                            },
                            modifier = Modifier.weight(1f).testTag("btn_dec_a"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
                            border = BorderStroke(1.dp, TerminalLightGray)
                        ) {
                            Text("-1", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                userValA = 0L
                                lastActionExpl = "Reset ${currentMission.primaryRegister} to 0."
                            },
                            modifier = Modifier.weight(1f).testTag("btn_zero_a"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrightPink),
                            border = BorderStroke(1.dp, TerminalLightGray)
                        ) {
                            Text("ZERO", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- Ground-Level Silicon Register & Bus Visualizer ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.5.dp, NeonGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Silicon Inspector",
                                tint = NeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GROUND-LEVEL HARDWARE STATE",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            color = TerminalBlack,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, TerminalLightGray)
                        ) {
                            Text(
                                text = "LIVE ALU WIRES",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Register Display
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TerminalLightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentMission.primaryRegister,
                                    color = NeonGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "0x" + userValA.toInt().toUInt().toString(16).uppercase().padStart(8, '0'),
                                    color = PureWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Signed: ${userValA.toInt()}",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Unsigned: ${userValA.toInt().toUInt()}",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            // 32-bit Binary Representation Strip
                            val binStr = userValA.toInt().toUInt().toString(2).padStart(32, '0')
                            Text(
                                text = "SILICON BIT CELLS (Bits 31 .. 0):",
                                color = MutedText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                binStr.forEachIndexed { bIndex, bitChar ->
                                    val bitNum = 31 - bIndex
                                    val isHigh = bitChar == '1'
                                    val isSignBit = bitNum == 31
                                    Box(
                                        modifier = Modifier
                                            .size(width = 16.dp, height = 24.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                when {
                                                    isHigh && isSignBit -> AlertOrange
                                                    isHigh -> NeonGreen
                                                    else -> DarkBackground
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                if (isSignBit) AlertOrange else TerminalLightGray,
                                                RoundedCornerShape(3.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bitChar.toString(),
                                            color = if (isHigh) TerminalBlack else MutedText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Secondary Register if present
                    if (currentMission.secondaryRegister != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = TerminalBlack,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TerminalLightGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentMission.secondaryRegister,
                                        color = CyberCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Signed: ${userValB.toInt()}",
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = "0x" + userValB.toInt().toUInt().toString(16).uppercase().padStart(8, '0'),
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live EFLAGS LED HUD
                    Text(
                        text = "CPU EFLAGS REGISTER FLIP-FLOPS:",
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
                        EflagLedItem(name = "ZF", label = "Zero", isHigh = zf, modifier = Modifier.weight(1f))
                        EflagLedItem(name = "CF", label = "Carry", isHigh = cf, modifier = Modifier.weight(1f))
                        EflagLedItem(name = "SF", label = "Sign", isHigh = sf, modifier = Modifier.weight(1f))
                        EflagLedItem(name = "OF", label = "Overflow", isHigh = of, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentMission.siliconHardwareNotes,
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- Step 2: How This Changes Computer Working (Branch & Execution Impact) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.5.dp, AlertOrange)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AltRoute,
                            contentDescription = "Control Flow Impact",
                            tint = AlertOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEP 2: HOW COMPUTER WORKING CHANGES",
                            color = AlertOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Execution Path Outcome Banner
                    val branchTaken = when (currentMission.id) {
                        "exp_zero_flag" -> zf
                        "exp_overflow_sign" -> of
                        "exp_bitmask_security" -> !zf
                        "exp_stack_hijack" -> (userValA == 0x00402000L)
                        "exp_carry_wrap" -> cf
                        "exp_ecx_loop" -> (userValA != 0L && userValA != 0xFFFFFFFFL)
                        else -> false
                    }

                    Surface(
                        color = if (branchTaken) NeonGreen.copy(alpha = 0.12f) else BrightPink.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (branchTaken) NeonGreen else BrightPink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (branchTaken) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (branchTaken) NeonGreen else BrightPink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (branchTaken) "PATH A: PRIVILEGED / BRANCH TAKEN" else "PATH B: DENIAL / FALLTHROUGH",
                                    color = if (branchTaken) NeonGreen else BrightPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = lastActionExpl,
                                color = PureWhite,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Assembly Code Context
                    Text(
                        text = "ACTIVE CPU INSTRUCTION STREAM:",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, TerminalLightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentMission.instructionSnippet,
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp),
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Deep Conceptual Explanation
                    Text(
                        text = "DEEP TRANSISTOR & SILICON REASONING:",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentMission.targetExplanation,
                        color = MutedText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button to launch in full debugger
                    Button(
                        onClick = {
                            onNavigateToDebugger?.invoke(currentMission.instructionSnippet)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_open_in_debugger"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Debugger",
                            tint = TerminalBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STEP CODE IN SYSTEM DEBUGGER",
                            color = TerminalBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: GROUND-LEVEL HARDWARE SANDBOX
// -------------------------------------------------------------------------------------------------

data class AluComputationResult(
    val resultVal: Long,
    val zf: Boolean,
    val cf: Boolean,
    val sf: Boolean,
    val of: Boolean,
    val pf: Boolean
)

@Composable
fun GroundLevelSandboxView(
    onNavigateToDebugger: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val registerNames = remember { listOf("EAX", "EBX", "ECX", "EDX", "ESI", "EDI", "ESP", "EBP") }
    var activeRegIndex by remember { mutableIntStateOf(0) }
    val activeReg = registerNames[activeRegIndex]

    // Active 32-bit register value
    var regValue by remember { mutableLongStateOf(0x0000002AL) } // 42 in hex
    var operandBValue by remember { mutableLongStateOf(5L) }
    var selectedOp by remember { mutableStateOf("ADD") }
    var liveAutoEval by remember { mutableStateOf(true) }

    // Calculate ALU micro-operation
    val (resultVal, zf, cf, sf, of, pf) = remember(regValue, operandBValue, selectedOp) {
        val a = regValue.toInt()
        val b = operandBValue.toInt()
        var r = 0
        var carry = false
        var overflow = false

        when (selectedOp) {
            "ADD" -> {
                val longRes = regValue + operandBValue
                r = a + b
                carry = (longRes and 0x100000000L) != 0L
                overflow = ((a xor r) and (b xor r) and 0x80000000.toInt()) != 0
            }
            "SUB" -> {
                r = a - b
                carry = (regValue.toULong() < operandBValue.toULong())
                overflow = ((a xor b) and (a xor r) and 0x80000000.toInt()) != 0
            }
            "CMP" -> {
                r = a - b
                carry = (regValue.toULong() < operandBValue.toULong())
                overflow = ((a xor b) and (a xor r) and 0x80000000.toInt()) != 0
            }
            "AND" -> {
                r = a and b
                carry = false
                overflow = false
            }
            "OR" -> {
                r = a or b
                carry = false
                overflow = false
            }
            "XOR" -> {
                r = a xor b
                carry = false
                overflow = false
            }
            "NOT" -> {
                r = a.inv()
                carry = false
                overflow = false
            }
            "SHL" -> {
                val shift = b and 0x1F
                r = a shl shift
                carry = if (shift > 0) ((a ushr (32 - shift)) and 1) != 0 else false
                overflow = false
            }
            "SHR" -> {
                val shift = b and 0x1F
                r = a ushr shift
                carry = if (shift > 0) ((a ushr (shift - 1)) and 1) != 0 else false
                overflow = false
            }
            "INC" -> {
                r = a + 1
                carry = false // x86 INC does not modify CF!
                overflow = (a == 0x7FFFFFFF)
            }
            "DEC" -> {
                r = a - 1
                carry = false
                overflow = (a == 0x80000000.toInt())
            }
            else -> {
                r = a
                carry = false
                overflow = false
            }
        }

        val zero = (r == 0)
        val sign = (r and 0x80000000.toInt()) != 0
        val lowByte = r and 0xFF
        var parityCount = 0
        for (i in 0 until 8) {
            if (((lowByte ushr i) and 1) == 1) parityCount++
        }
        val parity = (parityCount % 2 == 0)

        AluComputationResult(r.toLong() and 0xFFFFFFFFL, zero, carry, sign, overflow, parity)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Register Selector Chips ---
        item {
            Text(
                text = "TARGET REGISTER",
                color = MutedText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(registerNames.size) { index ->
                    val name = registerNames[index]
                    val isSelected = index == activeRegIndex
                    FilterChip(
                        selected = isSelected,
                        onClick = { activeRegIndex = index },
                        label = {
                            Text(
                                text = name,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonGreen,
                            selectedLabelColor = TerminalBlack,
                            containerColor = TerminalDarkGray,
                            labelColor = PureWhite
                        ),
                        border = BorderStroke(1.dp, if (isSelected) NeonGreen else TerminalLightGray),
                        modifier = Modifier.testTag("reg_chip_$name")
                    )
                }
            }
        }

        // --- Ground-Level 32-Bit Transistor Bit Strip (Interactive Flip) ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Bit Strip",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$activeReg TRANSISTOR BIT CELLS",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "TAP BIT TO FLIP (0 ↔ 1)",
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Current Value Monospace Banner
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, TerminalLightGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HEX: 0x" + regValue.toInt().toUInt().toString(16).uppercase().padStart(8, '0'),
                                color = NeonGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "DEC: ${regValue.toInt()}",
                                color = PureWhite,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "UINT: ${regValue.toInt().toUInt()}",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 32-bit Interactive Bit Buttons (Bits 31 to 0)
                    Text(
                        text = "UPPER 16 BITS (Bits 31 .. 16):",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BitCellRow(
                        startBit = 31,
                        endBit = 16,
                        currentValue = regValue,
                        onBitFlipped = { bitIdx ->
                            val mask = 1L shl bitIdx
                            regValue = regValue xor mask
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "LOWER 16 BITS (Bits 15 .. 0) [Sub-Registers AH / AL]:",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BitCellRow(
                        startBit = 15,
                        endBit = 0,
                        currentValue = regValue,
                        onBitFlipped = { bitIdx ->
                            val mask = 1L shl bitIdx
                            regValue = regValue xor mask
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Value Presets
                    Text(
                        text = "QUICK LOAD PRESETS:",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip("ZERO (0)") { regValue = 0L }
                        PresetChip("ONE (1)") { regValue = 1L }
                        PresetChip("MAX INT (0x7FFFFFFF)") { regValue = 0x7FFFFFFFL }
                        PresetChip("MIN INT (0x80000000)") { regValue = 0x80000000L }
                        PresetChip("ALL 1s (0xFFFFFFFF)") { regValue = 0xFFFFFFFFL }
                        PresetChip("CODE (0x00401000)") { regValue = 0x00401000L }
                        PresetChip("STACK (0x0012FF48)") { regValue = 0x0012FF48L }
                    }
                }
            }
        }

        // --- Hardware Micro-Operation Engine (ALU) ---
        item {
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
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "ALU Op",
                                tint = AlertOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ALU MICRO-OPERATION ENGINE",
                                color = AlertOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-EVAL",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = liveAutoEval,
                                onCheckedChange = { liveAutoEval = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGreen,
                                    checkedTrackColor = TerminalBlack
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Operation Picker
                    val ops = listOf("ADD", "SUB", "CMP", "AND", "OR", "XOR", "SHL", "SHR", "INC", "DEC", "NOT")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(ops.size) { idx ->
                            val op = ops[idx]
                            val isSel = op == selectedOp
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedOp = op },
                                label = { Text(op, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AlertOrange,
                                    selectedLabelColor = TerminalBlack,
                                    containerColor = TerminalDarkGray,
                                    labelColor = PureWhite
                                ),
                                border = BorderStroke(1.dp, if (isSel) AlertOrange else TerminalLightGray),
                                modifier = Modifier.testTag("op_chip_$op")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Operand B Controller (for binary ops)
                    if (selectedOp !in listOf("NOT", "INC", "DEC")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPERAND B (Value: ${operandBValue.toInt()} / 0x${operandBValue.toInt().toUInt().toString(16).uppercase()}):",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Slider(
                            value = operandBValue.toFloat(),
                            onValueChange = { operandBValue = it.toLong() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = TerminalLightGray
                            ),
                            modifier = Modifier.testTag("slider_operand_b")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { operandBValue = 0L },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, TerminalLightGray)
                            ) { Text("0", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            OutlinedButton(
                                onClick = { operandBValue = 1L },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, TerminalLightGray)
                            ) { Text("1", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            OutlinedButton(
                                onClick = { operandBValue = 10L },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, TerminalLightGray)
                            ) { Text("10", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                            OutlinedButton(
                                onClick = { operandBValue = 0x80L },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, TerminalLightGray)
                            ) { Text("0x80", fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ALU Operation Result Box
                    Surface(
                        color = TerminalBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, NeonGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ALU COMPUTED OUTPUT ($selectedOp):",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (selectedOp == "CMP") "(Flags only, no write)" else "RESULT -> $activeReg",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "0x" + resultVal.toInt().toUInt().toString(16).uppercase().padStart(8, '0'),
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Signed Decimal: ${resultVal.toInt()} | Unsigned: ${resultVal.toInt().toUInt()}",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            if (selectedOp != "CMP") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { regValue = resultVal },
                                    modifier = Modifier.fillMaxWidth().testTag("btn_commit_alu_result"),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "WRITE RESULT INTO $activeReg",
                                        color = TerminalBlack,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // EFLAGS Status Bar
                    Text(
                        text = "EFLAGS REGISTER WIRE OUTPUTS:",
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
                        EflagLedItem("ZF", "Zero", zf, Modifier.weight(1f))
                        EflagLedItem("CF", "Carry", cf, Modifier.weight(1f))
                        EflagLedItem("SF", "Sign", sf, Modifier.weight(1f))
                        EflagLedItem("OF", "Overflow", of, Modifier.weight(1f))
                        EflagLedItem("PF", "Parity", pf, Modifier.weight(1f))
                    }
                }
            }
        }

        // --- Branch Divergence Table ("How this changes the computer") ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
                border = BorderStroke(1.dp, TerminalLightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.AltRoute,
                            contentDescription = "Branches",
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOW INSTRUCTION BRANCHES REACT IN SILICON",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Depending on the flags above, here is how conditional jump instructions decide whether to alter the Instruction Pointer (EIP):",
                        color = MutedText,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BranchDecisionRow("JE / JZ", "Jump if Equal / Zero", zf, "Requires ZF == 1")
                    BranchDecisionRow("JNE / JNZ", "Jump if Not Equal", !zf, "Requires ZF == 0")
                    BranchDecisionRow("JS", "Jump if Sign / Negative", sf, "Requires SF == 1")
                    BranchDecisionRow("JO", "Jump if Overflow", of, "Requires OF == 1")
                    BranchDecisionRow("JC / JB", "Jump if Carry / Below", cf, "Requires CF == 1")
                    BranchDecisionRow("JG / JNLE", "Jump if Greater", !zf && (sf == of), "Requires ZF == 0 and SF == OF")
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPONENT HELPERS
// -------------------------------------------------------------------------------------------------

@Composable
fun EflagLedItem(
    name: String,
    label: String,
    isHigh: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isHigh) TerminalBlack else DarkBackground,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (isHigh) NeonGreen else TerminalLightGray),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isHigh) NeonGreen else TerminalLightGray)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = if (isHigh) NeonGreen else MutedText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = if (isHigh) "1 (HIGH)" else "0 (LOW)",
                color = if (isHigh) PureWhite else MutedText,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BitCellRow(
    startBit: Int,
    endBit: Int,
    currentValue: Long,
    onBitFlipped: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (bitIdx in startBit downTo endBit) {
            val mask = 1L shl bitIdx
            val isHigh = (currentValue and mask) != 0L
            val isSign = bitIdx == 31

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onBitFlipped(bitIdx) }
                    .testTag("bit_cell_$bitIdx")
            ) {
                Text(
                    text = "$bitIdx",
                    fontSize = 8.sp,
                    color = MutedText,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 28.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isHigh && isSign -> AlertOrange
                                isHigh -> NeonGreen
                                else -> DarkBackground
                            }
                        )
                        .border(
                            1.dp,
                            if (isSign) AlertOrange else TerminalLightGray,
                            RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHigh) "1" else "0",
                        color = if (isHigh) TerminalBlack else MutedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PresetChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        color = TerminalBlack,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, TerminalLightGray),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            color = PureWhite,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BranchDecisionRow(
    opcode: String,
    meaning: String,
    isTaken: Boolean,
    condition: String
) {
    Surface(
        color = TerminalBlack,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, TerminalLightGray),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = opcode,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "$meaning ($condition)",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }

            Surface(
                color = if (isTaken) NeonGreen.copy(alpha = 0.15f) else TerminalLightGray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isTaken) NeonGreen else TerminalLightGray)
            ) {
                Text(
                    text = if (isTaken) "BRANCH TAKEN" else "NOT TAKEN",
                    color = if (isTaken) NeonGreen else MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}
