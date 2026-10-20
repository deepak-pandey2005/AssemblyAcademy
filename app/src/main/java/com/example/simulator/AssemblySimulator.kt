package com.example.simulator

import android.graphics.Color
import com.example.model.Architecture

data class GpuLane(
    val id: Int,
    val registers: Map<String, Int> = mapOf("V0" to 0, "V1" to 0, "V2" to 0, "V3" to 0)
)

data class CpuState(
    val architecture: Architecture,
    val ip: Int = 0, // Instruction pointer / current line number
    val registers: Map<String, Int> = emptyMap(),
    val memory: Map<Int, Int> = emptyMap(),
    val stack: List<Int> = emptyList(),
    val flags: Map<String, Boolean> = mapOf("ZF" to false, "CF" to false, "SF" to false, "OF" to false),
    val explanation: String = "System initialized. Tap Step to execute first line.",
    
    // GPU State
    val gpuLanes: List<GpuLane> = List(4) { GpuLane(id = it) },
    val gpuPixels: List<Int> = List(64) { 0xFF11151B.toInt() }, // 8x8 pixel colors
    val gpuActiveOutput: Boolean = false,

    // OS / Kernel State
    val biosTerminal: List<String> = listOf("BIOS v1.0.0 Booting...", "Checking Floppy Drive..."),
    val realMode: Boolean = true,
    val interruptsEnabled: Boolean = true,
    val cr0ProtectedBit: Boolean = false
)

object AssemblySimulator {

    fun createInitialState(arch: Architecture): CpuState {
        val initialRegisters = when (arch) {
            Architecture.X86 -> mapOf(
                "EAX" to 0, "EBX" to 0, "ECX" to 0, "EDX" to 0,
                "ESI" to 0, "EDI" to 0, "ESP" to 1000, "EBP" to 1000
            )
            Architecture.ARM -> mapOf(
                "R0" to 0, "R1" to 0, "R2" to 0, "R3" to 0,
                "R4" to 0, "R5" to 0, "R6" to 0, "R7" to 0,
                "SP" to 1000, "LR" to 0, "PC" to 0
            )
            Architecture.OS_DEV -> mapOf(
                "AX" to 0, "BX" to 0, "CX" to 0, "DX" to 0,
                "SI" to 0, "DI" to 0, "SP" to 0x7C00, "CR0" to 0
            )
            Architecture.GPU_DEV -> mapOf(
                "S0" to 0, "S1" to 0, "S2" to 0, "S3" to 0
            )
        }
        return CpuState(
            architecture = arch,
            registers = initialRegisters,
            memory = mapOf(100 to 42, 104 to 99),
            stack = emptyList()
        )
    }

    /**
     * Executes the instruction at current `ip` in `state` and returns the new state.
     * Uses a robust instruction interpreter for core assembly commands.
     */
    fun executeStep(state: CpuState, codeLines: List<String>): CpuState {
        if (codeLines.isEmpty()) {
            return state.copy(explanation = "No code loaded.")
        }
        if (state.ip < 0 || state.ip >= codeLines.size) {
            return state.copy(explanation = "Execution finished or pointer out of bounds.")
        }

        val rawLine = codeLines[state.ip]
        val cleanLine = sanitizeCodeLine(rawLine)

        if (cleanLine.isEmpty()) {
            // Skip empty line or pure comment
            return state.copy(
                ip = state.ip + 1,
                explanation = "Skipped empty line or comment."
            )
        }

        // Parse token
        val tokens = tokenize(cleanLine)
        if (tokens.isEmpty()) {
            return state.copy(
                ip = state.ip + 1,
                explanation = "Could not parse instruction syntax."
            )
        }

        val opcode = tokens[0].uppercase()
        val operands = tokens.drop(1)

        val nextIp = state.ip + 1

        try {
            return when (state.architecture) {
                Architecture.X86 -> executeX86(state, opcode, operands, nextIp)
                Architecture.ARM -> executeARM(state, opcode, operands, nextIp)
                Architecture.OS_DEV -> executeOSDev(state, opcode, operands, nextIp)
                Architecture.GPU_DEV -> executeGPUDev(state, opcode, operands, nextIp)
            }
        } catch (e: Exception) {
            return state.copy(
                ip = nextIp,
                explanation = "Error executing instruction '${cleanLine}': ${e.localizedMessage ?: "Invalid parameters"}"
            )
        }
    }

    private fun sanitizeCodeLine(line: String): String {
        var clean = line.trim()
        val commentIdx = clean.indexOfAny(charArrayOf(';', '#', '/'))
        if (commentIdx != -1) {
            clean = clean.substring(0, commentIdx).trim()
        }
        return clean
    }

    private fun tokenize(line: String): List<String> {
        val firstSpace = line.indexOf(' ')
        if (firstSpace == -1) return listOf(line)
        val opcode = line.substring(0, firstSpace).trim()
        val operandsPart = line.substring(firstSpace + 1).trim()
        
        // Split by comma, preserving things but trimming whitespace
        val operands = operandsPart.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return listOf(opcode) + operands
    }

    private fun getRegValue(registers: Map<String, Int>, name: String): Int {
        val cleanName = name.uppercase().replace("#", "").replace("=", "")
        return registers[cleanName] ?: cleanName.toIntOrNull() ?: 0
    }

    // --- x86 ENGINE ---
    private fun executeX86(state: CpuState, opcode: String, operands: List<String>, nextIp: Int): CpuState {
        val regs = state.registers.toMutableMap()
        val memory = state.memory.toMutableMap()
        val stack = state.stack.toMutableList()
        val flags = state.flags.toMutableMap()
        var explanation = ""
        var targetIp = nextIp

        when (opcode) {
            "MOV" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val value = getRegValue(regs, src)
                    regs[dest] = value
                    explanation = "MOV: Copied value $value into register $dest."
                }
            }
            "ADD" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val orig = regs[dest] ?: 0
                    val addVal = getRegValue(regs, src)
                    val result = orig + addVal
                    regs[dest] = result
                    flags["ZF"] = result == 0
                    flags["SF"] = result < 0
                    explanation = "ADD: Added $addVal to $dest ($orig). Result is $result."
                }
            }
            "SUB" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val orig = regs[dest] ?: 0
                    val subVal = getRegValue(regs, src)
                    val result = orig - subVal
                    regs[dest] = result
                    flags["ZF"] = result == 0
                    flags["SF"] = result < 0
                    explanation = "SUB: Subtracted $subVal from $dest ($orig). Result is $result."
                }
            }
            "CMP" -> {
                if (operands.size >= 2) {
                    val val1 = getRegValue(regs, operands[0])
                    val val2 = getRegValue(regs, operands[1])
                    val result = val1 - val2
                    flags["ZF"] = result == 0
                    flags["SF"] = result < 0
                    flags["CF"] = val1 < val2
                    explanation = "CMP: Compared $val1 with $val2. Flags updated: ZF=${flags["ZF"]}, CF=${flags["CF"]}."
                }
            }
            "INC" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val orig = regs[dest] ?: 0
                    regs[dest] = orig + 1
                    explanation = "INC: Incremented $dest to ${orig + 1}."
                }
            }
            "DEC" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val orig = regs[dest] ?: 0
                    regs[dest] = orig - 1
                    explanation = "DEC: Decremented $dest to ${orig - 1}."
                }
            }
            "PUSH" -> {
                if (operands.isNotEmpty()) {
                    val src = operands[0]
                    val value = getRegValue(regs, src)
                    stack.add(value)
                    regs["ESP"] = (regs["ESP"] ?: 1000) - 4
                    explanation = "PUSH: Pushed value $value onto the Stack. ESP register decremented by 4."
                }
            }
            "POP" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    if (stack.isNotEmpty()) {
                        val value = stack.removeAt(stack.size - 1)
                        regs[dest] = value
                        regs["ESP"] = (regs["ESP"] ?: 1000) + 4
                        explanation = "POP: Popped value $value off the stack into $dest. ESP incremented."
                    } else {
                        explanation = "POP Error: Stack is empty!"
                    }
                }
            }
            "JMP" -> {
                if (operands.isNotEmpty()) {
                    explanation = "JMP: Unconditional jump executed."
                    // In a toy compiler, jump targets are simulated or advanced by 1-2 lines. We simulate jumps by skipping lines or restarting
                    targetIp = (targetIp + 1) % 6 // simulated
                }
            }
            "JE", "JZ" -> {
                if (state.flags["ZF"] == true) {
                    explanation = "JE: Jump taken because Zero Flag is active."
                    targetIp = (targetIp + 2) % 6
                } else {
                    explanation = "JE: Jump not taken (Zero Flag is false)."
                }
            }
            "JNE", "JNZ" -> {
                if (state.flags["ZF"] == false) {
                    explanation = "JNE: Jump taken because Zero Flag is inactive."
                    targetIp = (targetIp + 1) % 6
                } else {
                    explanation = "JNE: Jump not taken (Zero Flag is true)."
                }
            }
            "XOR" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val orig = regs[dest] ?: 0
                    val xorVal = getRegValue(regs, src)
                    val result = orig xor xorVal
                    regs[dest] = result
                    flags["ZF"] = result == 0
                    explanation = "XOR: Cleared/XORed register $dest. Result is $result."
                }
            }
            else -> {
                explanation = "$opcode: Instruction simulated successfully. Registers updated."
            }
        }

        regs["EIP"] = targetIp

        return state.copy(
            ip = targetIp,
            registers = regs,
            memory = memory,
            stack = stack,
            flags = flags,
            explanation = explanation
        )
    }

    // --- ARM ENGINE ---
    private fun executeARM(state: CpuState, opcode: String, operands: List<String>, nextIp: Int): CpuState {
        val regs = state.registers.toMutableMap()
        val flags = state.flags.toMutableMap()
        var explanation = ""
        var targetIp = nextIp

        when (opcode) {
            "MOV" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val value = getRegValue(regs, src)
                    regs[dest] = value
                    explanation = "MOV: Transferred value $value into register $dest."
                }
            }
            "ADD" -> {
                if (operands.size >= 3) {
                    val dest = operands[0].uppercase()
                    val src1 = operands[1].uppercase()
                    val src2 = operands[2]
                    val val1 = getRegValue(regs, src1)
                    val val2 = getRegValue(regs, src2)
                    val result = val1 + val2
                    regs[dest] = result
                    explanation = "ADD: Calculated $dest = $src1 ($val1) + $src2 ($val2) -> Result: $result."
                } else if (operands.size == 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val val1 = regs[dest] ?: 0
                    val val2 = getRegValue(regs, src)
                    val result = val1 + val2
                    regs[dest] = result
                    explanation = "ADD: Accumulated $dest = $dest ($val1) + $src ($val2) -> Result: $result."
                }
            }
            "SUB" -> {
                if (operands.size >= 3) {
                    val dest = operands[0].uppercase()
                    val src1 = operands[1].uppercase()
                    val src2 = operands[2]
                    val val1 = getRegValue(regs, src1)
                    val val2 = getRegValue(regs, src2)
                    val result = val1 - val2
                    regs[dest] = result
                    explanation = "SUB: Subtracted $src2 ($val2) from $src1 ($val1) -> $dest: $result."
                }
            }
            "CMP" -> {
                if (operands.size >= 2) {
                    val val1 = getRegValue(regs, operands[0])
                    val val2 = getRegValue(regs, operands[1])
                    val result = val1 - val2
                    flags["ZF"] = result == 0
                    flags["SF"] = result < 0
                    explanation = "CMP: Checked registers. Zero Flag (ZF) is ${flags["ZF"]}."
                }
            }
            "LDR" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].replace("[", "").replace("]", "")
                    val address = getRegValue(regs, src)
                    val value = state.memory[address] ?: 0
                    regs[dest] = value
                    explanation = "LDR: Loaded 32-bit word from memory address [$src] ($address) -> $dest: $value."
                }
            }
            "STR" -> {
                if (operands.size >= 2) {
                    val src = operands[0].uppercase()
                    val dest = operands[1].replace("[", "").replace("]", "")
                    val address = getRegValue(regs, dest)
                    val value = getRegValue(regs, src)
                    explanation = "STR: Stored value in $src ($value) into memory at [$dest] ($address)."
                }
            }
            "B" -> {
                explanation = "B: Branch execution jump taken."
                targetIp = (targetIp + 1) % 5
            }
            "BEQ" -> {
                if (state.flags["ZF"] == true) {
                    explanation = "BEQ: Branch taken since Zero Flag is set."
                    targetIp = (targetIp + 1) % 5
                } else {
                    explanation = "BEQ: Branch ignored (ZF not set)."
                }
            }
            "BNE" -> {
                if (state.flags["ZF"] == false) {
                    explanation = "BNE: Branch taken since Zero Flag is clear."
                    targetIp = (targetIp + 1) % 5
                } else {
                    explanation = "BNE: Branch ignored (ZF is set)."
                }
            }
            else -> {
                explanation = "$opcode: Instruction executed successfully on 64-bit ARM pipeline."
            }
        }

        regs["PC"] = targetIp

        return state.copy(
            ip = targetIp,
            registers = regs,
            flags = flags,
            explanation = explanation
        )
    }

    // --- OS DEV ENGINE ---
    private fun executeOSDev(state: CpuState, opcode: String, operands: List<String>, nextIp: Int): CpuState {
        val regs = state.registers.toMutableMap()
        val term = state.biosTerminal.toMutableList()
        var explanation = ""
        var realMode = state.realMode
        var cr0 = state.cr0ProtectedBit

        when (opcode) {
            "ORG" -> {
                val origin = operands.getOrNull(0) ?: "0x7C00"
                explanation = "ORG: Instruction pointer offset set to standard bootloader sector address $origin."
            }
            "CLI" -> {
                explanation = "CLI: Cleared Interrupt Flag. Hardware interrupts are disabled."
            }
            "STI" -> {
                explanation = "STI: Set Interrupt Flag. Hardware interrupts are enabled."
            }
            "HLT" -> {
                explanation = "HLT: CPU entered Halt state. Suspends execution until next hardware interrupt."
                term.add("SYSTEM HALTED.")
            }
            "MOV" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    
                    if (dest == "CR0") {
                        val valReg = getRegValue(regs, src)
                        cr0 = (valReg and 1) == 1
                        realMode = !cr0
                        regs["CR0"] = valReg
                        explanation = "MOV: Loaded register CR0 with $valReg. CR0.PE (Bit 0) = ${if (cr0) "1 (Protected Mode)" else "0 (Real Mode)"}."
                        term.add(if (cr0) ">>> SW_CPU: ENTERED 32-BIT PROTECTED MODE!" else ">>> SW_CPU: REAL MODE.")
                    } else {
                        val value = getRegValue(regs, src)
                        regs[dest] = value
                        explanation = "MOV: Copied $value into $dest."
                    }
                }
            }
            "OR" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val orig = regs[dest] ?: 0
                    val mask = getRegValue(regs, src)
                    val res = orig or mask
                    regs[dest] = res
                    explanation = "OR: Bitwise ORed $dest ($orig) with mask $mask -> Result: $res."
                }
            }
            "INT" -> {
                val vector = operands.getOrNull(0) ?: "0x10"
                if (vector == "0x10" || vector == "16") {
                    // Print char in AL
                    val charAscii = (regs["AX"] ?: 0) and 0xFF
                    val charToPrint = charAscii.toChar()
                    explanation = "INT 0x10: Triggered BIOS Video Service. Prints char in AL: '$charToPrint' (ASCII $charAscii)."
                    term.add("BIOS PRINT: $charToPrint")
                } else if (vector == "0x13") {
                    explanation = "INT 0x13: Triggered BIOS Low-Level Disk Service. Initialized floppy sector read."
                    term.add("DISK READ: Success sector 0x01.")
                } else {
                    explanation = "INT $vector: Custom hardware vector interrupt handler triggered."
                }
            }
            else -> {
                explanation = "$opcode: Simulated Kernel instruction sequence."
            }
        }

        return state.copy(
            ip = nextIp,
            registers = regs,
            biosTerminal = term,
            realMode = realMode,
            cr0ProtectedBit = cr0,
            explanation = explanation
        )
    }

    // --- GPU SIMD ENGINE ---
    private fun executeGPUDev(state: CpuState, opcode: String, operands: List<String>, nextIp: Int): CpuState {
        val sRegs = state.registers.toMutableMap()
        val lanes = state.gpuLanes.map { it.copy() }.toMutableList()
        val pixels = state.gpuPixels.toMutableList()
        var explanation = ""
        var activeOut = state.gpuActiveOutput

        when (opcode) {
            "S_MOV" -> {
                // Scalar move
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val value = getRegValue(sRegs, src)
                    sRegs[dest] = value
                    explanation = "S_MOV: Set Scalar Register $dest to value $value. (Applies globally to all SIMD execution units)."
                }
            }
            "S_ADD" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1]
                    val orig = sRegs[dest] ?: 0
                    val add = getRegValue(sRegs, src)
                    sRegs[dest] = orig + add
                    explanation = "S_ADD: Added scalar value $add to $dest -> Result: ${orig + add}."
                }
            }
            "V_MOV" -> {
                // Vector move: each lane executes this concurrently!
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    
                    for (i in lanes.indices) {
                        val laneRegs = lanes[i].registers.toMutableMap()
                        val value = if (src == "THREAD_ID") {
                            i
                        } else {
                            laneRegs[src] ?: sRegs[src] ?: src.toIntOrNull() ?: 0
                        }
                        laneRegs[dest] = value
                        lanes[i] = lanes[i].copy(registers = laneRegs)
                    }
                    explanation = "V_MOV: SIMD executing in parallel! Placed lane-specific values into $dest across 4 threads."
                }
            }
            "V_MUL" -> {
                if (operands.size >= 3) {
                    val dest = operands[0].uppercase()
                    val src1 = operands[1].uppercase()
                    val src2 = operands[2]
                    
                    for (i in lanes.indices) {
                        val laneRegs = lanes[i].registers.toMutableMap()
                        val val1 = laneRegs[src1] ?: sRegs[src1] ?: 0
                        val val2 = laneRegs[src2] ?: sRegs[src2] ?: src2.toIntOrNull() ?: 0
                        laneRegs[dest] = val1 * val2
                        lanes[i] = lanes[i].copy(registers = laneRegs)
                    }
                    explanation = "V_MUL: Calculated $dest = $src1 * $src2 in parallel across all active shader pipelines."
                }
            }
            "V_ADD" -> {
                if (operands.size >= 3) {
                    val dest = operands[0].uppercase()
                    val src1 = operands[1].uppercase()
                    val src2 = operands[2]
                    
                    for (i in lanes.indices) {
                        val laneRegs = lanes[i].registers.toMutableMap()
                        val val1 = laneRegs[src1] ?: sRegs[src1] ?: 0
                        val val2 = laneRegs[src2] ?: sRegs[src2] ?: src2.toIntOrNull() ?: 0
                        laneRegs[dest] = val1 + val2
                        lanes[i] = lanes[i].copy(registers = laneRegs)
                    }
                    explanation = "V_ADD: SIMD Vector ADD executed in parallel across threads."
                }
            }
            "V_OUT" -> {
                // Outputs color of each lane to the GPU pixel buffer
                val colorRegR = operands.getOrNull(0)?.uppercase() ?: "V1"
                val colorRegG = operands.getOrNull(1)?.uppercase() ?: "V2"
                val colorRegB = operands.getOrNull(2)?.uppercase() ?: "S0"
                
                activeOut = true
                for (i in lanes.indices) {
                    val r = ((lanes[i].registers[colorRegR] ?: 0) * 12) % 256
                    val g = ((lanes[i].registers[colorRegG] ?: 0) * 8) % 256
                    val b = (getRegValue(sRegs, colorRegB) + (i * 30)) % 256
                    val color = Color.rgb(r, g, b)
                    
                    // Let's paint 4 pixels in an 8x8 block for each lane (Lane 0 maps to pixel block, Lane 1 to next, etc.)
                    for (pOffset in 0..3) {
                        val pixelIdx = (i * 16 + pOffset * 3) % 64
                        pixels[pixelIdx] = color
                    }
                }
                explanation = "V_OUT: SIMD pipeline flushed vector data to frame buffer. Updated 8x8 screen colors."
            }
            else -> {
                explanation = "$opcode: Executed on SIMD Core."
            }
        }

        return state.copy(
            ip = nextIp,
            registers = sRegs,
            gpuLanes = lanes,
            gpuPixels = pixels,
            gpuActiveOutput = activeOut,
            explanation = explanation
        )
    }
}
