package com.example.simulator

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Complete snapshot of simulated x86 CPU registers.
 * Models 32-bit IA-32 registers (EAX, EBX, etc.) with hardware-accurate
 * sub-register aliasing (AX, AH, AL), EFLAGS condition codes, segment
 * registers, and control registers.
 */
data class X86RegisterSnapshot(
    val eax: Int = 0,
    val ebx: Int = 0,
    val ecx: Int = 0,
    val edx: Int = 0,
    val esi: Int = 0,
    val edi: Int = 0,
    val ebp: Int = 1000,
    val esp: Int = 1000,
    val eip: Int = 0,
    val eflags: Int = 0x0002, // In x86, bit 1 is permanently 1 (reserved)
    val cs: Int = 0x0010,
    val ds: Int = 0x0018,
    val ss: Int = 0x0018,
    val es: Int = 0x0018,
    val fs: Int = 0x0000,
    val gs: Int = 0x0000,
    val cr0: Int = 0x00000010, // ET flag default set
    val cr3: Int = 0x00000000,
    val memory: Map<Int, Int> = mapOf(100 to 42, 104 to 99, 108 to 15, 112 to 256, 116 to 88, 120 to 777),
    val modifiedMemoryAddresses: Set<Int> = emptySet(),
    val cycleCount: Long = 0,
    val lastModifiedRegister: String = "",
    val lastAction: String = "CPU Power-On Initialized"
) {
    // --- 16-bit Word Sub-Registers (Bits 0..15) ---
    val ax: Int get() = eax and 0xFFFF
    val bx: Int get() = ebx and 0xFFFF
    val cx: Int get() = ecx and 0xFFFF
    val dx: Int get() = edx and 0xFFFF
    val si: Int get() = esi and 0xFFFF
    val di: Int get() = edi and 0xFFFF
    val bp: Int get() = ebp and 0xFFFF
    val sp: Int get() = esp and 0xFFFF
    val ip: Int get() = eip and 0xFFFF

    // --- 8-bit Low Byte Sub-Registers (Bits 0..7) ---
    val al: Int get() = eax and 0xFF
    val bl: Int get() = ebx and 0xFF
    val cl: Int get() = ecx and 0xFF
    val dl: Int get() = edx and 0xFF

    // --- 8-bit High Byte Sub-Registers (Bits 8..15) ---
    val ah: Int get() = (eax ushr 8) and 0xFF
    val bh: Int get() = (ebx ushr 8) and 0xFF
    val ch: Int get() = (ecx ushr 8) and 0xFF
    val dh: Int get() = (edx ushr 8) and 0xFF

    // --- EFLAGS Individual Status Bits ---
    val cf: Boolean get() = (eflags and (1 shl 0)) != 0   // Carry Flag (Bit 0)
    val pf: Boolean get() = (eflags and (1 shl 2)) != 0   // Parity Flag (Bit 2)
    val af: Boolean get() = (eflags and (1 shl 4)) != 0   // Auxiliary Carry Flag (Bit 4)
    val zf: Boolean get() = (eflags and (1 shl 6)) != 0   // Zero Flag (Bit 6)
    val sf: Boolean get() = (eflags and (1 shl 7)) != 0   // Sign Flag (Bit 7)
    val tf: Boolean get() = (eflags and (1 shl 8)) != 0   // Trap Flag (Bit 8)
    val ifFlag: Boolean get() = (eflags and (1 shl 9)) != 0 // Interrupt Flag (Bit 9)
    val df: Boolean get() = (eflags and (1 shl 10)) != 0  // Direction Flag (Bit 10)
    val of: Boolean get() = (eflags and (1 shl 11)) != 0  // Overflow Flag (Bit 11)

    // Lookup 32-bit register value by canonical name
    fun getRegisterValue(name: String): Int {
        return when (name.uppercase().trim()) {
            "EAX" -> eax
            "EBX" -> ebx
            "ECX" -> ecx
            "EDX" -> edx
            "ESI" -> esi
            "EDI" -> edi
            "EBP" -> ebp
            "ESP" -> esp
            "EIP" -> eip
            "EFLAGS" -> eflags
            "AX" -> ax
            "BX" -> bx
            "CX" -> cx
            "DX" -> dx
            "SI" -> si
            "DI" -> di
            "BP" -> bp
            "SP" -> sp
            "AL" -> al
            "BL" -> bl
            "CL" -> cl
            "DL" -> dl
            "AH" -> ah
            "BH" -> bh
            "CH" -> ch
            "DH" -> dh
            "CS" -> cs
            "DS" -> ds
            "SS" -> ss
            "ES" -> es
            "FS" -> fs
            "GS" -> gs
            "CR0" -> cr0
            "CR3" -> cr3
            else -> 0
        }
    }

    // Convert core 8 general registers to Map for simulator interop
    fun toSimulatorMap(): Map<String, Int> {
        return mapOf(
            "EAX" to eax,
            "EBX" to ebx,
            "ECX" to ecx,
            "EDX" to edx,
            "ESI" to esi,
            "EDI" to edi,
            "ESP" to esp,
            "EBP" to ebp,
            "EIP" to eip,
            "AX" to ax,
            "BX" to bx,
            "CX" to cx,
            "DX" to dx,
            "SI" to si,
            "DI" to di,
            "SP" to sp,
            "BP" to bp,
            "AL" to al,
            "BL" to bl,
            "CL" to cl,
            "DL" to dl,
            "AH" to ah,
            "BH" to bh,
            "CH" to ch,
            "DH" to dh
        )
    }

    // Convert flags to Map for simulator interop
    fun toFlagsMap(): Map<String, Boolean> {
        return mapOf(
            "ZF" to zf,
            "CF" to cf,
            "SF" to sf,
            "OF" to of,
            "PF" to pf,
            "AF" to af,
            "IF" to ifFlag,
            "DF" to df
        )
    }
}

/**
 * Log record of a register modification event.
 */
data class X86RegisterTransition(
    val registerName: String,
    val oldValue: Int,
    val newValue: Int,
    val reason: String,
    val cycleIndex: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Watchpoint condition for reactive alerting.
 */
data class X86Watchpoint(
    val id: String,
    val registerName: String,
    val condition: String, // "EQUALS", "ZERO", "GREATER_THAN", "LESS_THAN", "OVERFLOW"
    val threshold: Int = 0,
    val isActive: Boolean = true
)

/**
 * Preset machine scenarios to demonstrate real-world computer working.
 */
enum class X86ScenarioPreset(
    val title: String,
    val description: String,
    val setupAction: (X86RegisterStateManager) -> Unit
) {
    CLEAN_POWER_ON(
        title = "Clean CPU Reset",
        description = "Initializes registers to standard IA-32 power-on defaults.",
        setupAction = { it.reset() }
    ),
    LOOP_COUNTER(
        title = "ECX Loop Counter",
        description = "Configures ECX = 10, EAX = 0, EBX = 5 ready for a loop accumulation.",
        setupAction = {
            it.reset()
            it.setRegister("ECX", 10, "Preset: Loop Counter")
            it.setRegister("EAX", 0, "Preset: Accumulator Initialized")
            it.setRegister("EBX", 5, "Preset: Step Operand")
        }
    ),
    STACK_FRAME_PROLOGUE(
        title = "Stack Frame (ESP/EBP)",
        description = "Simulates function prologue: PUSH EBP; MOV EBP, ESP; SUB ESP, 16.",
        setupAction = {
            it.reset()
            it.setRegister("ESP", 984, "Preset: Stack Frame Alloc")
            it.setRegister("EBP", 1000, "Preset: Frame Base Pointer")
            it.setRegister("EAX", 42, "Preset: Function Return Arg")
        }
    ),
    INTEGER_OVERFLOW_TEST(
        title = "Signed Overflow Test",
        description = "Loads EAX = 0x7FFFFFFF (2147483647). Adding 1 will flip SF and trigger OF.",
        setupAction = {
            it.reset()
            it.setRegister("EAX", 0x7FFFFFFF, "Preset: Max Signed 32-bit")
            it.setRegister("EBX", 1, "Preset: Addend")
        }
    ),
    ZERO_FLAG_COMPARISON(
        title = "Equality Comparison (ZF)",
        description = "Loads EAX = 100, EBX = 100. CMP will clear result and set Zero Flag (ZF = 1).",
        setupAction = {
            it.reset()
            it.setRegister("EAX", 100, "Preset: Operand A")
            it.setRegister("EBX", 100, "Preset: Operand B")
        }
    ),
    STRING_COPY_SETUP(
        title = "String Index (ESI -> EDI)",
        description = "Prepares string source index ESI = 0x0064, destination EDI = 0x0080, ECX = 32.",
        setupAction = {
            it.reset()
            it.setRegister("ESI", 0x0064, "Preset: Source String")
            it.setRegister("EDI", 0x0080, "Preset: Dest Buffer")
            it.setRegister("ECX", 32, "Preset: String Length")
        }
    )
}

/**
 * Central State Management System to simulate CPU registers for the x86 architecture.
 * Features reactive StateFlows, bit-accurate sub-register propagation, EFLAGS condition
 * code management, watchpoint alerting, micro-instruction dispatch, undo/redo, and
 * full transition audit logging.
 */
class X86RegisterStateManager(
    initialState: X86RegisterSnapshot = X86RegisterSnapshot()
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<X86RegisterSnapshot> = _state.asStateFlow()

    private val _modifiedRegisters = MutableStateFlow<Set<String>>(emptySet())
    val modifiedRegisters: StateFlow<Set<String>> = _modifiedRegisters.asStateFlow()

    private val _history = MutableStateFlow<List<X86RegisterTransition>>(emptyList())
    val history: StateFlow<List<X86RegisterTransition>> = _history.asStateFlow()

    private val undoStack = mutableListOf<X86RegisterSnapshot>()
    private val redoStack = mutableListOf<X86RegisterSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _watchpointAlert = MutableStateFlow<String?>(null)
    val watchpointAlert: StateFlow<String?> = _watchpointAlert.asStateFlow()

    /**
     * Primary mutator for any x86 register name (32-bit, 16-bit, or 8-bit).
     * Handles hardware-accurate sub-register propagation and aliases.
     */
    fun setRegister(name: String, value: Int, reason: String = "Manual Modification") {
        saveSnapshotForUndo()
        val current = _state.value
        val clean = name.uppercase().trim()
        val oldVal = current.getRegisterValue(clean)

        val updated = when (clean) {
            // --- 32-bit General Purpose ---
            "EAX" -> current.copy(eax = value)
            "EBX" -> current.copy(ebx = value)
            "ECX" -> current.copy(ecx = value)
            "EDX" -> current.copy(edx = value)
            "ESI" -> current.copy(esi = value)
            "EDI" -> current.copy(edi = value)
            "EBP" -> current.copy(ebp = value)
            "ESP" -> current.copy(esp = value)
            "EIP" -> current.copy(eip = value)

            // --- 16-bit Word Sub-Registers (Preserves bits 16..31) ---
            "AX" -> {
                val newEax = (current.eax and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(eax = newEax)
            }
            "BX" -> {
                val newEbx = (current.ebx and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(ebx = newEbx)
            }
            "CX" -> {
                val newEcx = (current.ecx and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(ecx = newEcx)
            }
            "DX" -> {
                val newEdx = (current.edx and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(edx = newEdx)
            }
            "SI" -> {
                val newEsi = (current.esi and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(esi = newEsi)
            }
            "DI" -> {
                val newEdi = (current.edi and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(edi = newEdi)
            }
            "BP" -> {
                val newEbp = (current.ebp and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(ebp = newEbp)
            }
            "SP" -> {
                val newEsp = (current.esp and 0xFFFF0000.toInt()) or (value and 0xFFFF)
                current.copy(esp = newEsp)
            }

            // --- 8-bit Low Byte Sub-Registers (Preserves bits 8..31) ---
            "AL" -> {
                val newEax = (current.eax and 0xFFFFFF00.toInt()) or (value and 0xFF)
                current.copy(eax = newEax)
            }
            "BL" -> {
                val newEbx = (current.ebx and 0xFFFFFF00.toInt()) or (value and 0xFF)
                current.copy(ebx = newEbx)
            }
            "CL" -> {
                val newEcx = (current.ecx and 0xFFFFFF00.toInt()) or (value and 0xFF)
                current.copy(ecx = newEcx)
            }
            "DL" -> {
                val newEdx = (current.edx and 0xFFFFFF00.toInt()) or (value and 0xFF)
                current.copy(edx = newEdx)
            }

            // --- 8-bit High Byte Sub-Registers (Preserves bits 0..7 and 16..31) ---
            "AH" -> {
                val newEax = (current.eax and 0xFFFF00FF.toInt()) or ((value and 0xFF) shl 8)
                current.copy(eax = newEax)
            }
            "BH" -> {
                val newEbx = (current.ebx and 0xFFFF00FF.toInt()) or ((value and 0xFF) shl 8)
                current.copy(ebx = newEbx)
            }
            "CH" -> {
                val newEcx = (current.ecx and 0xFFFF00FF.toInt()) or ((value and 0xFF) shl 8)
                current.copy(ecx = newEcx)
            }
            "DH" -> {
                val newEdx = (current.edx and 0xFFFF00FF.toInt()) or ((value and 0xFF) shl 8)
                current.copy(edx = newEdx)
            }

            // --- Segment & Control Registers ---
            "CS" -> current.copy(cs = value and 0xFFFF)
            "DS" -> current.copy(ds = value and 0xFFFF)
            "SS" -> current.copy(ss = value and 0xFFFF)
            "ES" -> current.copy(es = value and 0xFFFF)
            "CR0" -> current.copy(cr0 = value)
            "CR3" -> current.copy(cr3 = value)
            "EFLAGS" -> current.copy(eflags = value or 0x0002) // Maintain bit 1
            else -> current
        }

        val finalized = updated.copy(
            cycleCount = current.cycleCount + 1,
            lastModifiedRegister = clean,
            lastAction = "$reason ($clean: $oldVal -> $value)"
        )

        // Related registers that change because of aliasing
        val modifiedFamily = getRegisterFamily(clean)
        _modifiedRegisters.value = modifiedFamily

        recordTransition(clean, oldVal, value, reason, finalized.cycleCount)
        _state.value = finalized
        checkWatchpoints(clean, value)
    }

    /**
     * Writes a 32-bit dword value into simulated RAM at the specified address.
     */
    fun setMemoryValue(address: Int, value: Int, reason: String = "Manual Memory Write") {
        saveSnapshotForUndo()
        val current = _state.value
        val updatedMem = current.memory.toMutableMap()
        val aligned = (address / 4) * 4
        val oldVal = updatedMem[aligned] ?: 0
        updatedMem[aligned] = value

        val modifiedSet = setOf(aligned, aligned + 1, aligned + 2, aligned + 3)
        val finalized = current.copy(
            memory = updatedMem,
            modifiedMemoryAddresses = modifiedSet,
            cycleCount = current.cycleCount + 1,
            lastAction = "$reason (0x${aligned.toString(16).uppercase().padStart(4, '0')}: $oldVal -> $value)"
        )
        recordTransition("RAM[0x${aligned.toString(16).uppercase()}]", oldVal, value, reason, finalized.cycleCount)
        _state.value = finalized
    }

    /**
     * Reads a 32-bit dword value from simulated RAM.
     */
    fun getMemoryValue(address: Int): Int {
        val aligned = (address / 4) * 4
        return _state.value.memory[aligned] ?: 0
    }

    /**
     * Toggles an individual bit (0 to 31) in a 32-bit register.
     */
    fun toggleRegisterBit(registerName: String, bitIndex: Int) {
        if (bitIndex !in 0..31) return
        val currentVal = _state.value.getRegisterValue(registerName)
        val newVal = currentVal xor (1 shl bitIndex)
        val bitState = (newVal and (1 shl bitIndex)) != 0
        setRegister(registerName, newVal, "Bit $bitIndex set to ${if (bitState) "1" else "0"}")
    }

    /**
     * Toggles an individual EFLAGS condition bit.
     */
    fun toggleFlag(flagName: String) {
        val bitPos = when (flagName.uppercase().trim()) {
            "CF" -> 0
            "PF" -> 2
            "AF" -> 4
            "ZF" -> 6
            "SF" -> 7
            "TF" -> 8
            "IF" -> 9
            "DF" -> 10
            "OF" -> 11
            else -> return
        }

        saveSnapshotForUndo()
        val currentEflags = _state.value.eflags
        val newEflags = (currentEflags xor (1 shl bitPos)) or 0x0002 // maintain bit 1
        val isNowSet = (newEflags and (1 shl bitPos)) != 0

        val updated = _state.value.copy(
            eflags = newEflags,
            cycleCount = _state.value.cycleCount + 1,
            lastModifiedRegister = "EFLAGS",
            lastAction = "Toggled Flag $flagName to ${if (isNowSet) "1" else "0"}"
        )

        _modifiedRegisters.value = setOf("EFLAGS", flagName.uppercase())
        recordTransition(flagName.uppercase(), if (isNowSet) 0 else 1, if (isNowSet) 1 else 0, "Flag toggle", updated.cycleCount)
        _state.value = updated
    }

    /**
     * Executes a single micro-instruction against the central register state,
     * recalculating arithmetic/logical result and EFLAGS condition codes.
     */
    fun executeMicroInstruction(command: String) {
        val clean = command.trim()
        if (clean.isEmpty()) return

        val parts = clean.split(" ", limit = 2)
        val opcode = parts[0].uppercase()
        val operands = parts.getOrNull(1)?.split(",")?.map { it.trim() } ?: emptyList()

        when (opcode) {
            "INC" -> {
                if (operands.isNotEmpty()) {
                    val reg = operands[0].uppercase()
                    val orig = _state.value.getRegisterValue(reg)
                    val result = orig + 1
                    setRegister(reg, result, "INC: Incremented $reg")
                    updateArithmeticFlags(result, orig, 1, isSub = false)
                }
            }
            "DEC" -> {
                if (operands.isNotEmpty()) {
                    val reg = operands[0].uppercase()
                    val orig = _state.value.getRegisterValue(reg)
                    val result = orig - 1
                    setRegister(reg, result, "DEC: Decremented $reg")
                    updateArithmeticFlags(result, orig, 1, isSub = true)
                }
            }
            "MOV" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase().trim()
                    val src = operands[1].uppercase().trim()
                    val isDestMem = dest.startsWith("[") && dest.endsWith("]")
                    val isSrcMem = src.startsWith("[") && src.endsWith("]")

                    if (isDestMem) {
                        val addrExpr = dest.substring(1, dest.length - 1).trim()
                        val addr = resolveAddress(addrExpr)
                        val srcVal = resolveValue(src)
                        setMemoryValue(addr, srcVal, "MOV [$addr]: Wrote $srcVal to RAM")
                    } else if (isSrcMem) {
                        val addrExpr = src.substring(1, src.length - 1).trim()
                        val addr = resolveAddress(addrExpr)
                        val memVal = getMemoryValue(addr)
                        setRegister(dest, memVal, "MOV $dest, [$addr]: Loaded $memVal from RAM")
                    } else {
                        val srcVal = resolveValue(src)
                        setRegister(dest, srcVal, "MOV: Transferred $srcVal into $dest")
                    }
                }
            }
            "PUSH" -> {
                if (operands.isNotEmpty()) {
                    val src = operands[0].uppercase().trim()
                    val srcVal = resolveValue(src)
                    val newEsp = _state.value.esp - 4
                    setRegister("ESP", newEsp, "PUSH: Decremented ESP to $newEsp")
                    setMemoryValue(newEsp, srcVal, "PUSH: Stored $srcVal at [ESP 0x${newEsp.toString(16).uppercase()}]")
                }
            }
            "POP" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase().trim()
                    val currentEsp = _state.value.esp
                    val memVal = getMemoryValue(currentEsp)
                    setRegister(dest, memVal, "POP: Read $memVal from [ESP 0x${currentEsp.toString(16).uppercase()}]")
                    setRegister("ESP", currentEsp + 4, "POP: Incremented ESP to ${currentEsp + 4}")
                }
            }
            "ADD" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    val destVal = _state.value.getRegisterValue(dest)
                    val srcVal = resolveValue(src)
                    val result = destVal + srcVal
                    setRegister(dest, result, "ADD: Added $srcVal to $dest")
                    updateArithmeticFlags(result, destVal, srcVal, isSub = false)
                }
            }
            "SUB" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    val destVal = _state.value.getRegisterValue(dest)
                    val srcVal = resolveValue(src)
                    val result = destVal - srcVal
                    setRegister(dest, result, "SUB: Subtracted $srcVal from $dest")
                    updateArithmeticFlags(result, destVal, srcVal, isSub = true)
                }
            }
            "XOR" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    val destVal = _state.value.getRegisterValue(dest)
                    val srcVal = resolveValue(src)
                    val result = destVal xor srcVal
                    val reason = if (dest == src) "XOR: Cleared $dest to 0 (Zeroing idiom)" else "XOR: Bitwise XOR on $dest"
                    setRegister(dest, result, reason)
                    updateLogicFlags(result)
                }
            }
            "AND" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    val destVal = _state.value.getRegisterValue(dest)
                    val srcVal = resolveValue(src)
                    val result = destVal and srcVal
                    setRegister(dest, result, "AND: Bitwise AND on $dest")
                    updateLogicFlags(result)
                }
            }
            "OR" -> {
                if (operands.size >= 2) {
                    val dest = operands[0].uppercase()
                    val src = operands[1].uppercase()
                    val destVal = _state.value.getRegisterValue(dest)
                    val srcVal = resolveValue(src)
                    val result = destVal or srcVal
                    setRegister(dest, result, "OR: Bitwise OR on $dest")
                    updateLogicFlags(result)
                }
            }
            "NOT" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val orig = _state.value.getRegisterValue(dest)
                    val result = orig.inv()
                    setRegister(dest, result, "NOT: One's complement bitwise inversion")
                }
            }
            "NEG" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val orig = _state.value.getRegisterValue(dest)
                    val result = -orig
                    setRegister(dest, result, "NEG: Two's complement negation")
                    updateArithmeticFlags(result, 0, orig, isSub = true)
                }
            }
            "SHL" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val count = operands.getOrNull(1)?.toIntOrNull() ?: 1
                    val orig = _state.value.getRegisterValue(dest)
                    val result = orig shl count
                    setRegister(dest, result, "SHL: Shifted left by $count bits")
                    updateLogicFlags(result)
                }
            }
            "SHR" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val count = operands.getOrNull(1)?.toIntOrNull() ?: 1
                    val orig = _state.value.getRegisterValue(dest)
                    val result = orig ushr count
                    setRegister(dest, result, "SHR: Logical shifted right by $count bits")
                    updateLogicFlags(result)
                }
            }
            "ROL" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val count = operands.getOrNull(1)?.toIntOrNull() ?: 1
                    val orig = _state.value.getRegisterValue(dest)
                    val result = Integer.rotateLeft(orig, count)
                    setRegister(dest, result, "ROL: Rotated left by $count bits")
                }
            }
            "ROR" -> {
                if (operands.isNotEmpty()) {
                    val dest = operands[0].uppercase()
                    val count = operands.getOrNull(1)?.toIntOrNull() ?: 1
                    val orig = _state.value.getRegisterValue(dest)
                    val result = Integer.rotateRight(orig, count)
                    setRegister(dest, result, "ROR: Rotated right by $count bits")
                }
            }
            "XCHG" -> {
                if (operands.size >= 2) {
                    val reg1 = operands[0].uppercase()
                    val reg2 = operands[1].uppercase()
                    val val1 = _state.value.getRegisterValue(reg1)
                    val val2 = _state.value.getRegisterValue(reg2)
                    saveSnapshotForUndo()
                    setRegister(reg1, val2, "XCHG: Swapped $reg1 <-> $reg2")
                    setRegister(reg2, val1, "XCHG: Swapped $reg1 <-> $reg2")
                }
            }
            "CMP" -> {
                if (operands.size >= 2) {
                    val val1 = _state.value.getRegisterValue(operands[0])
                    val val2 = resolveValue(operands[1])
                    val result = val1 - val2
                    updateArithmeticFlags(result, val1, val2, isSub = true)
                }
            }
            "TEST" -> {
                if (operands.size >= 2) {
                    val val1 = _state.value.getRegisterValue(operands[0])
                    val val2 = resolveValue(operands[1])
                    val result = val1 and val2
                    updateLogicFlags(result)
                }
            }
            else -> {
                // Generic instruction logging
                _state.value = _state.value.copy(
                    cycleCount = _state.value.cycleCount + 1,
                    lastAction = "Dispatched: $command"
                )
            }
        }
    }

    /**
     * Resolves an operand string as either a register name or integer immediate (Dec or 0x Hex).
     */
    private fun resolveValue(token: String): Int {
        val clean = token.uppercase().trim()
        val regVal = _state.value.getRegisterValue(clean)
        if (regVal != 0 || isKnownRegister(clean)) return regVal
        return when {
            clean.startsWith("0X") -> clean.substring(2).toLongOrNull(16)?.toInt() ?: 0
            clean.endsWith("H") -> clean.dropLast(1).toLongOrNull(16)?.toInt() ?: 0
            else -> clean.toIntOrNull() ?: 0
        }
    }

    private fun isKnownRegister(name: String): Boolean {
        return name in listOf(
            "EAX", "EBX", "ECX", "EDX", "ESI", "EDI", "EBP", "ESP", "EIP",
            "AX", "BX", "CX", "DX", "SI", "DI", "BP", "SP",
            "AL", "BL", "CL", "DL", "AH", "BH", "CH", "DH",
            "CS", "DS", "SS", "ES", "FS", "GS", "CR0", "CR3", "EFLAGS"
        )
    }

    private fun updateArithmeticFlags(result: Int, op1: Int, op2: Int, isSub: Boolean) {
        val zf = result == 0
        val sf = result < 0
        val cf = if (isSub) (op1.toLong() and 0xFFFFFFFFL) < (op2.toLong() and 0xFFFFFFFFL)
                 else ((op1.toLong() and 0xFFFFFFFFL) + (op2.toLong() and 0xFFFFFFFFL)) > 0xFFFFFFFFL
        
        // Signed overflow detection
        val of = if (isSub) {
            ((op1 xor op2) and (op1 xor result) and 0x80000000.toInt()) != 0
        } else {
            ((op1 xor result) and (op2 xor result) and 0x80000000.toInt()) != 0
        }

        // Parity flag: parity of low 8 bits (1 if even number of 1 bits)
        val lowByte = result and 0xFF
        val pf = Integer.bitCount(lowByte) % 2 == 0

        var eflags = _state.value.eflags
        eflags = setFlagBit(eflags, 0, cf)
        eflags = setFlagBit(eflags, 2, pf)
        eflags = setFlagBit(eflags, 6, zf)
        eflags = setFlagBit(eflags, 7, sf)
        eflags = setFlagBit(eflags, 11, of)

        _state.value = _state.value.copy(eflags = eflags or 0x0002)
    }

    private fun updateLogicFlags(result: Int) {
        val zf = result == 0
        val sf = result < 0
        val lowByte = result and 0xFF
        val pf = Integer.bitCount(lowByte) % 2 == 0

        // In x86, logic instructions (AND, OR, XOR) always clear CF and OF to 0!
        var eflags = _state.value.eflags
        eflags = setFlagBit(eflags, 0, false) // CF = 0
        eflags = setFlagBit(eflags, 11, false) // OF = 0
        eflags = setFlagBit(eflags, 2, pf)
        eflags = setFlagBit(eflags, 6, zf)
        eflags = setFlagBit(eflags, 7, sf)

        _state.value = _state.value.copy(eflags = eflags or 0x0002)
    }

    private fun setFlagBit(eflags: Int, bit: Int, value: Boolean): Int {
        return if (value) eflags or (1 shl bit) else eflags and (1 shl bit).inv()
    }

    private fun getRegisterFamily(name: String): Set<String> {
        return when (name) {
            "EAX", "AX", "AH", "AL" -> setOf("EAX", "AX", "AH", "AL")
            "EBX", "BX", "BH", "BL" -> setOf("EBX", "BX", "BH", "BL")
            "ECX", "CX", "CH", "CL" -> setOf("ECX", "CX", "CH", "CL")
            "EDX", "DX", "DH", "DL" -> setOf("EDX", "DX", "DH", "DL")
            "ESI", "SI" -> setOf("ESI", "SI")
            "EDI", "DI" -> setOf("EDI", "DI")
            "EBP", "BP" -> setOf("EBP", "BP")
            "ESP", "SP" -> setOf("ESP", "SP")
            else -> setOf(name)
        }
    }

    private fun saveSnapshotForUndo() {
        undoStack.add(_state.value)
        if (undoStack.size > 50) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_state.value)
            val prev = undoStack.removeAt(undoStack.size - 1)
            _state.value = prev.copy(lastAction = "Undo State Restoration")
            _canUndo.value = undoStack.isNotEmpty()
            _canRedo.value = true
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_state.value)
            val next = redoStack.removeAt(redoStack.size - 1)
            _state.value = next.copy(lastAction = "Redo State Restoration")
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
        }
    }

    fun reset() {
        saveSnapshotForUndo()
        _state.value = X86RegisterSnapshot(
            cycleCount = _state.value.cycleCount + 1,
            lastAction = "Central Registers Reset to IA-32 Defaults"
        )
        _modifiedRegisters.value = setOf("EAX", "EBX", "ECX", "EDX", "ESI", "EDI", "EBP", "ESP", "EFLAGS")
    }

    fun applyPreset(preset: X86ScenarioPreset) {
        saveSnapshotForUndo()
        preset.setupAction(this)
    }

    private fun resolveAddress(expr: String): Int {
        val clean = expr.uppercase().trim()
        val regVal = _state.value.getRegisterValue(clean)
        if (regVal != 0 || isKnownRegister(clean)) return regVal
        return when {
            clean.startsWith("0X") -> clean.substring(2).toLongOrNull(16)?.toInt() ?: 0
            clean.endsWith("H") -> clean.dropLast(1).toLongOrNull(16)?.toInt() ?: 0
            else -> clean.toIntOrNull() ?: 0
        }
    }

    fun syncFromMap(
        map: Map<String, Int>,
        flags: Map<String, Boolean> = emptyMap(),
        memory: Map<Int, Int>? = null,
        modifiedAddrs: Set<Int>? = null
    ) {
        var current = _state.value
        map.forEach { (name, value) ->
            when (name.uppercase()) {
                "EAX" -> current = current.copy(eax = value)
                "EBX" -> current = current.copy(ebx = value)
                "ECX" -> current = current.copy(ecx = value)
                "EDX" -> current = current.copy(edx = value)
                "ESI" -> current = current.copy(esi = value)
                "EDI" -> current = current.copy(edi = value)
                "EBP" -> current = current.copy(ebp = value)
                "ESP" -> current = current.copy(esp = value)
                "EIP" -> current = current.copy(eip = value)
            }
        }
        var eflags = current.eflags
        flags["ZF"]?.let { eflags = setFlagBit(eflags, 6, it) }
        flags["CF"]?.let { eflags = setFlagBit(eflags, 0, it) }
        flags["SF"]?.let { eflags = setFlagBit(eflags, 7, it) }
        flags["OF"]?.let { eflags = setFlagBit(eflags, 11, it) }
        if (memory != null) {
            current = current.copy(memory = memory)
        }
        if (modifiedAddrs != null) {
            current = current.copy(modifiedMemoryAddresses = modifiedAddrs)
        }
        _state.value = current.copy(eflags = eflags or 0x0002)
    }

    private fun recordTransition(name: String, oldVal: Int, newVal: Int, reason: String, cycle: Long) {
        val trans = X86RegisterTransition(name, oldVal, newVal, reason, cycle)
        _history.value = (_history.value + trans).takeLast(40)
    }

    private fun checkWatchpoints(reg: String, value: Int) {
        if (reg == "EAX" && value == 0) {
            _watchpointAlert.value = "Watchpoint: EAX reached 0x00000000 (Zero)"
        } else if (reg == "ESP" && value < 500) {
            _watchpointAlert.value = "Watchpoint: ESP Stack Pointer critical depth ($value)"
        }
    }

    fun dismissWatchpointAlert() {
        _watchpointAlert.value = null
    }
}
