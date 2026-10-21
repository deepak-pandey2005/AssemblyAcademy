package com.example.model

/**
 * Data structures for the structured x86 Assembly Curriculum focusing on
 * basic instructions, general-purpose registers, and memory operations.
 */

data class X86RegisterInfo(
    val name64: String,
    val name32: String,
    val name16: String,
    val nameHigh8: String?,
    val nameLow8: String,
    val primaryRole: String,
    val callingConvention: String,
    val preserveRule: String
)

data class SIBAddressExpression(
    val baseName: String,
    val baseValue: Int,
    val indexName: String,
    val indexValue: Int,
    val scale: Int,
    val displacement: Int
) {
    val effectiveAddress: Int
        get() = baseValue + (indexValue * scale) + displacement

    val expressionString: String
        get() {
            val scalePart = if (scale > 1) " + $indexName * $scale" else if (indexName.isNotEmpty()) " + $indexName" else ""
            val dispPart = when {
                displacement > 0 -> " + 0x${displacement.toString(16).uppercase()}"
                displacement < 0 -> " - 0x${(-displacement).toString(16).uppercase()}"
                else -> ""
            }
            return "[$baseName$scalePart$dispPart]"
        }
}

data class InstructionSyntaxRule(
    val opcode: String,
    val syntax: String,
    val destination: String,
    val source: String,
    val description: String,
    val allowedOperands: List<String>,
    val forbiddenOperands: List<String>
)

data class X86CurriculumTopic(
    val id: String,
    val unitId: Int,
    val topicNumber: Int,
    val title: String,
    val subtitle: String,
    val opcodeTag: String,
    val keyConcept: String,
    val overviewMarkdown: String,
    val syntaxRule: InstructionSyntaxRule? = null,
    val codeExample: String,
    val commonMistakes: String,
    val bestPractices: String,
    val quizQuestion: String,
    val quizOptions: List<String>,
    val quizCorrectAnswerIndex: Int,
    val quizExplanation: String
)

data class X86CurriculumUnit(
    val id: Int,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val description: String,
    val topics: List<X86CurriculumTopic>
)

object X86CurriculumRepository {

    val REGISTER_CATALOG = listOf(
        X86RegisterInfo(
            name64 = "RAX",
            name32 = "EAX",
            name16 = "AX",
            nameHigh8 = "AH",
            nameLow8 = "AL",
            primaryRole = "Accumulator / Function Return Value",
            callingConvention = "Volatile / Caller-Saved",
            preserveRule = "Caller saves if value needed across call"
        ),
        X86RegisterInfo(
            name64 = "RBX",
            name32 = "EBX",
            name16 = "BX",
            nameHigh8 = "BH",
            nameLow8 = "BL",
            primaryRole = "Base Register (pointer to data structures)",
            callingConvention = "Non-Volatile / Callee-Saved",
            preserveRule = "Must be preserved across function calls"
        ),
        X86RegisterInfo(
            name64 = "RCX",
            name32 = "ECX",
            name16 = "CX",
            nameHigh8 = "CH",
            nameLow8 = "CL",
            primaryRole = "Counter (loops, bit shifts) / 4th arg in Win64",
            callingConvention = "Volatile / Caller-Saved",
            preserveRule = "Caller saves if value needed across call"
        ),
        X86RegisterInfo(
            name64 = "RDX",
            name32 = "EDX",
            name16 = "DX",
            nameHigh8 = "DH",
            nameLow8 = "DL",
            primaryRole = "Data Register (I/O, multiply/divide overflow)",
            callingConvention = "Volatile / Caller-Saved",
            preserveRule = "3rd parameter in System V AMD64 ABI"
        ),
        X86RegisterInfo(
            name64 = "RSI",
            name32 = "ESI",
            name16 = "SI",
            nameHigh8 = null,
            nameLow8 = "SIL",
            primaryRole = "Source Index (string copy, 2nd function argument)",
            callingConvention = "Volatile / Caller-Saved (System V)",
            preserveRule = "Used as source pointer in string operations"
        ),
        X86RegisterInfo(
            name64 = "RDI",
            name32 = "EDI",
            name16 = "DI",
            nameHigh8 = null,
            nameLow8 = "DIL",
            primaryRole = "Destination Index (1st function argument in Linux)",
            callingConvention = "Volatile / Caller-Saved (System V)",
            preserveRule = "Used as destination pointer in string ops"
        ),
        X86RegisterInfo(
            name64 = "RBP",
            name32 = "EBP",
            name16 = "BP",
            nameHigh8 = null,
            nameLow8 = "BPL",
            primaryRole = "Base Pointer (Stack frame anchor for local vars)",
            callingConvention = "Non-Volatile / Callee-Saved",
            preserveRule = "Must restore to original value before RET"
        ),
        X86RegisterInfo(
            name64 = "RSP",
            name32 = "ESP",
            name16 = "SP",
            nameHigh8 = null,
            nameLow8 = "SPL",
            primaryRole = "Stack Pointer (Points to top of hardware stack)",
            callingConvention = "Hardware Stack Pointer",
            preserveRule = "Maintained automatically by PUSH, POP, CALL, RET"
        )
    )

    val CURRICULUM_UNITS: List<X86CurriculumUnit> = listOf(
        X86CurriculumUnit(
            id = 1,
            title = "Unit 1: Register Architecture & Bit Slicing",
            subtitle = "Quadword, Doubleword, Word & Byte Slicing",
            iconName = "Memory",
            description = "Understand the fundamental 64-bit register file, how 32-bit, 16-bit, and 8-bit registers map to the same physical silicon, and the crucial zero-extension rule.",
            topics = listOf(
                X86CurriculumTopic(
                    id = "x86_reg_anatomy",
                    unitId = 1,
                    topicNumber = 1,
                    title = "The 64-Bit Register File",
                    subtitle = "General-Purpose Silicon Storage",
                    opcodeTag = "REGISTERS",
                    keyConcept = "Registers are high-speed internal CPU storage locations operating in a single clock cycle, far faster than L1 cache or RAM.",
                    overviewMarkdown = """
                        # The CPU Register File
                        Modern x86-64 CPUs have 16 general-purpose registers: RAX, RBX, RCX, RDX, RSI, RDI, RBP, RSP, and R8 through R15.
                        
                        ### Why Registers Matter
                        - Access latency: < 1 nanosecond (zero-wait-state silicon).
                        - All arithmetic, logical comparisons, and address calculations require data to reside in registers.
                        - In the Intel syntax used by x86, instruction operands are written as:
                          `OPCODE DESTINATION, SOURCE`
                        
                        ### Key Takeaway
                        Whenever your program needs to compute a value or inspect memory, it must first load data into a register.
                    """.trimIndent(),
                    codeExample = """
                        ; Loading values into general-purpose registers
                        MOV RAX, 100        ; Load immediate 100 into 64-bit RAX
                        MOV RBX, 200        ; Load immediate 200 into 64-bit RBX
                        ADD RAX, RBX        ; RAX = RAX + RBX (RAX now 300)
                    """.trimIndent(),
                    commonMistakes = "Thinking that RAX and EAX are separate physical storage cells. They point to the same physical transistor register!",
                    bestPractices = "Use standard register conventions (e.g. RAX for return values, RCX for loops) to make assembly readable and ABI-compliant.",
                    quizQuestion = "In Intel assembly syntax, what does 'MOV RAX, RBX' do?",
                    quizOptions = listOf(
                        "Copies the contents of RBX into RAX",
                        "Copies the contents of RAX into RBX",
                        "Swaps the values in RAX and RBX",
                        "Compares RAX with RBX"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "In Intel syntax, the format is 'OPCODE DESTINATION, SOURCE'. So 'MOV RAX, RBX' copies from SOURCE (RBX) into DESTINATION (RAX)."
                ),
                X86CurriculumTopic(
                    id = "x86_reg_slicing",
                    unitId = 1,
                    topicNumber = 2,
                    title = "Register Slicing: RAX, EAX, AX, AH, AL",
                    subtitle = "64-bit to 8-bit Sub-Register Hierarchy",
                    opcodeTag = "SLICING",
                    keyConcept = "Lower portions of registers can be addressed independently for backward compatibility and memory efficiency.",
                    overviewMarkdown = """
                        # Register Sub-Division
                        Every 64-bit register is partitioned into smaller addressable units:
                        
                        - **RAX (64-bit / Quadword):** Full 64 bits [63..0]
                        - **EAX (32-bit / Doubleword):** Lower 32 bits [31..0]
                        - **AX (16-bit / Word):** Lower 16 bits [15..0]
                        - **AH (8-bit / High Byte):** Bits [15..8] of AX
                        - **AL (8-bit / Low Byte):** Bits [7..0] of AX
                        
                        ### Example with Hex Value 0x1122334455667788:
                        - Full RAX: `0x1122334455667788`
                        - EAX: `0x55667788`
                        - AX: `0x7788`
                        - AH: `0x77`
                        - AL: `0x88`
                    """.trimIndent(),
                    codeExample = """
                        ; Slicing demonstration
                        MOV RAX, 0x1122334455667788
                        MOV AL, 0xFF        ; Overwrites bits [7..0]. RAX becomes 0x11223344556677FF
                        MOV AH, 0xAA        ; Overwrites bits [15..8]. RAX becomes 0x112233445566AAFF
                    """.trimIndent(),
                    commonMistakes = "Confusing AH with the upper half of EAX or RAX. AH is strictly bits [15..8] of AX (the second lowest byte of the register)!",
                    bestPractices = "Use the smallest operand size that fits your data to conserve memory and cache line bandwidth, but watch out for partial register stalls.",
                    quizQuestion = "If RAX contains 0x12345678ABCD, what does AL contain?",
                    quizOptions = listOf(
                        "0xCD",
                        "0xAB",
                        "0xABCD",
                        "0x12"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "AL is the lowest 8 bits (1 byte) of RAX, which is 0xCD. AH would be 0xAB, and AX would be 0xABCD."
                ),
                X86CurriculumTopic(
                    id = "x86_zero_extension",
                    unitId = 1,
                    topicNumber = 3,
                    title = "The 32-bit Zero-Extension Rule",
                    subtitle = "Critical x86-64 Architectural Behavior",
                    opcodeTag = "ZERO-EXTEND",
                    keyConcept = "In x86-64, any write to a 32-bit register automatically clears (zeros out) the upper 32 bits of the corresponding 64-bit register.",
                    overviewMarkdown = """
                        # Automatic Zero-Extension
                        This is one of the most important rules of x86-64:
                        
                        - Writing to **8-bit** (AL) or **16-bit** (AX): Leaves the remaining upper bits of RAX **unchanged**!
                        - Writing to **32-bit** (EAX): **Automatically zeros out** bits [63..32] of RAX!
                        
                        ### Why Did Intel/AMD Design It This Way?
                        1. **Performance**: Eliminates false dependencies on upper bits in out-of-order execution pipelines.
                        2. **Predictability**: Prevents stale upper garbage bits when converting 32-bit legacy code to 64-bit systems.
                    """.trimIndent(),
                    codeExample = """
                        MOV RAX, 0xFFFFFFFFFFFFFFFF ; RAX is all 1s
                        MOV AL, 0x42                 ; RAX is 0xFFFFFFFFFFFFFF42 (upper bits preserved!)
                        MOV AX, 0x1234               ; RAX is 0xFFFFFFFFFFFF1234 (upper bits preserved!)
                        MOV EAX, 0x00000005          ; RAX is 0x0000000000000005 (UPPER 32 BITS ZEROED!)
                    """.trimIndent(),
                    commonMistakes = "Expecting 'MOV EAX, 5' to preserve the top 32 bits of RAX. It DOES NOT — it sets bits [63..32] to 0!",
                    bestPractices = "Take advantage of 'MOV EAX, value' instead of 'MOV RAX, value' for small positive integers because the 32-bit instruction encoding is smaller (fewer bytes).",
                    quizQuestion = "If RAX = 0xFFFFFFFFFFFFFFFF and you execute 'MOV EAX, 0x10', what is RAX?",
                    quizOptions = listOf(
                        "0x0000000000000010 (upper bits zeroed)",
                        "0xFFFFFFFF00000010 (upper bits preserved)",
                        "0xFFFFFFFFFFFFFF10",
                        "0x1000000000000000"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "Because 32-bit operations in x86-64 automatically zero-extend into the 64-bit destination, the upper 32 bits become 0x00000000, leaving 0x0000000000000010."
                )
            )
        ),
        X86CurriculumUnit(
            id = 2,
            title = "Unit 2: Basic Instructions & Data Movement",
            subtitle = "MOV, XCHG, PUSH, POP & Legal Operand Rules",
            iconName = "SwapHoriz",
            description = "Master fundamental x86 data transfer opcodes, understand Intel operand ordering, stack manipulation with RSP, and learn the inviolable memory-to-memory restriction.",
            topics = listOf(
                X86CurriculumTopic(
                    id = "x86_mov_instruction",
                    unitId = 2,
                    topicNumber = 1,
                    title = "The MOV Instruction & Legal Operands",
                    subtitle = "Destination ← Source Data Transfer",
                    opcodeTag = "MOV",
                    keyConcept = "MOV copies data from source to destination without modifying flags or changing the source.",
                    overviewMarkdown = """
                        # MOV dest, src
                        The MOV instruction is the workhorse of assembly language. It copies data from `src` to `dest`.
                        
                        ### Valid Operand Combinations
                        - **Register ← Immediate**: `MOV EAX, 42`
                        - **Register ← Register**: `MOV EBX, EAX`
                        - **Register ← Memory**: `MOV EAX, [100]`
                        - **Memory ← Register**: `MOV [100], EBX`
                        - **Memory ← Immediate**: `MOV DWORD PTR [100], 99`
                        
                        ### Operand Size Match Requirement
                        Both operands must be the same size! You cannot do `MOV EAX, AL` or `MOV [100], RAX` without matching specifiers.
                    """.trimIndent(),
                    syntaxRule = InstructionSyntaxRule(
                        opcode = "MOV",
                        syntax = "MOV dest, src",
                        destination = "Register or Memory [addr]",
                        source = "Register, Memory [addr], or Immediate value",
                        description = "Copies source operand to destination operand.",
                        allowedOperands = listOf("REG, REG", "REG, IMM", "REG, [MEM]", "[MEM], REG", "[MEM], IMM"),
                        forbiddenOperands = listOf("[MEM], [MEM] (Illegal in x86 hardware!)")
                    ),
                    codeExample = """
                        MOV EAX, 15          ; Immediate to register
                        MOV EBX, EAX         ; Register to register
                        MOV [100], EBX       ; Register to memory address 100
                        MOV ECX, [100]       ; Memory to register (ECX becomes 15)
                    """.trimIndent(),
                    commonMistakes = "Attempting to assign an immediate to memory without a size qualifier (e.g. 'MOV [100], 5' is ambiguous: is 5 a Byte, Word, or DWord?).",
                    bestPractices = "Always specify size qualifiers when loading immediates into memory, like: 'MOV DWORD PTR [100], 5'.",
                    quizQuestion = "Which of the following MOV operations is strictly illegal in x86 assembly?",
                    quizOptions = listOf(
                        "MOV [200], [100] (Memory to Memory)",
                        "MOV [100], EAX (Register to Memory)",
                        "MOV EAX, [100] (Memory to Register)",
                        "MOV EBX, 50 (Immediate to Register)"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "x86 architecture does not support memory-to-memory MOV instructions in a single opcode. You must move memory to a register first, then register to destination memory."
                ),
                X86CurriculumTopic(
                    id = "x86_mem_to_mem_rule",
                    unitId = 2,
                    topicNumber = 2,
                    title = "The Memory-to-Memory Restriction",
                    subtitle = "Why MOV [mem1], [mem2] Fails & The 2-Step Pattern",
                    opcodeTag = "RULE",
                    keyConcept = "x86 CPU instruction decoders only support at most ONE memory dereference per standard ALU/MOV instruction.",
                    overviewMarkdown = """
                        # The One-Memory-Operand Rule
                        Why is `MOV [dest], [src]` impossible in x86?
                        
                        1. **Hardware Microarchitecture**: A single instruction cannot generate two simultaneous memory addresses, perform two bus cycles, and manage two cache lookups in one core cycle.
                        2. **CISC Encoding Constraints**: Opcode byte modR/M formats allow encoding only one base/index/displacement address specifier.
                        
                        ### The Standard 2-Step Workaround
                        To copy data from one memory location to another, use an intermediate scratch register (like EAX, ECX, or EDX):
                        
                        ```assembly
                        ; ILLEGAL: MOV [200], [100]
                        
                        ; LEGAL 2-STEP PATTERN:
                        MOV EAX, [100]       ; Step 1: Read memory into register
                        MOV [200], EAX       ; Step 2: Write register into target memory
                        ```
                    """.trimIndent(),
                    codeExample = """
                        ; Copying memory variable at address 100 to address 200:
                        MOV [100], 777       ; Initialize source memory
                        MOV EAX, [100]       ; Read into scratch register EAX
                        MOV [200], EAX       ; Write from scratch register to address 200
                    """.trimIndent(),
                    commonMistakes = "Writing 'MOV [varA], [varB]' in high-level assembly and getting a compiler assembler error: 'invalid operand combination'.",
                    bestPractices = "Use volatile caller-saved scratch registers like EAX or ECX for two-step memory copies.",
                    quizQuestion = "How do you copy a 32-bit value from memory address 104 to memory address 108?",
                    quizOptions = listOf(
                        "MOV EAX, [104] followed by MOV [108], EAX",
                        "MOV [108], [104]",
                        "COPY [104], [108]",
                        "LEA [108], [104]"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "Since memory-to-memory is prohibited in x86 hardware, you read into a scratch register (MOV EAX, [104]) and write it out (MOV [108], EAX)."
                ),
                X86CurriculumTopic(
                    id = "x86_stack_push_pop",
                    unitId = 2,
                    topicNumber = 3,
                    title = "Stack Operations: PUSH & POP",
                    subtitle = "LIFO Storage & RSP Stack Pointer Mechanics",
                    opcodeTag = "STACK",
                    keyConcept = "The x86 hardware stack grows downward in memory (toward lower addresses). PUSH decrements RSP; POP increments RSP.",
                    overviewMarkdown = """
                        # The Hardware Stack
                        x86 CPUs feature a built-in Last-In-First-Out (LIFO) stack managed by the RSP register (or ESP in 32-bit).
                        
                        ### What Happens During PUSH RAX:
                        1. `RSP = RSP - 8` (Stack pointer decreases by 8 bytes)
                        2. `[RSP] = RAX` (Value stored at the new top of stack)
                        
                        ### What Happens During POP RBX:
                        1. `RBX = [RSP]` (Value read from current top of stack)
                        2. `RSP = RSP + 8` (Stack pointer increases by 8 bytes)
                        
                        ### Common Uses
                        - Saving and restoring registers across subroutine calls.
                        - Passing parameters to functions.
                        - Allocating local variable space on the stack frame.
                    """.trimIndent(),
                    codeExample = """
                        MOV RAX, 42
                        MOV RBX, 99
                        
                        PUSH RAX             ; ESP/RSP drops, 42 saved to stack
                        PUSH RBX             ; ESP/RSP drops, 99 saved to stack
                        
                        POP RAX              ; RAX receives 99 (last pushed!)
                        POP RBX              ; RBX receives 42 (first pushed!)
                        ; Swapped RAX and RBX without a third register!
                    """.trimIndent(),
                    commonMistakes = "Assuming the stack grows upwards (towards higher addresses). In x86, PUSH decreases the pointer address!",
                    bestPractices = "Always ensure every PUSH has a corresponding POP before returning from a function to prevent stack corruption and crashes.",
                    quizQuestion = "If ESP = 1000 and you execute 'PUSH EAX', what is the new value of ESP?",
                    quizOptions = listOf(
                        "996 (decrements by 4 bytes)",
                        "1004 (increments by 4 bytes)",
                        "1000 (unchanged)",
                        "992 (decrements by 8 bytes)"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "In 32-bit mode, PUSH decrements the stack pointer by 4 bytes (size of a DWORD). 1000 - 4 = 996."
                )
            )
        ),
        X86CurriculumUnit(
            id = 3,
            title = "Unit 3: Memory Operations & Addressing Modes",
            subtitle = "Pointers, SIB Formulas, Brackets & LEA",
            iconName = "Layers",
            description = "Explore direct, indirect, base-displacement, and scaled-indexed addressing modes. Learn the critical difference between MOV and LEA.",
            topics = listOf(
                X86CurriculumTopic(
                    id = "x86_brackets_dereferencing",
                    unitId = 3,
                    topicNumber = 1,
                    title = "Brackets & Memory Dereferencing",
                    subtitle = "Values vs Addresses: [RAX] vs RAX",
                    opcodeTag = "DEREFERENCE",
                    keyConcept = "Square brackets '[...]' in x86 indicate dereferencing: access the memory content located at that calculated address.",
                    overviewMarkdown = """
                        # Square Brackets = Pointer Dereference
                        In C/C++, if `ptr` is a pointer, `*ptr` means dereference. In x86 assembly, square brackets `[ptr]` represent the exact same concept!
                        
                        ### Look at the Difference:
                        - `MOV EAX, 100` -> EAX becomes the literal integer number **100**.
                        - `MOV EAX, [100]` -> Read whatever 4-byte value is stored in **RAM at address 100**.
                        
                        ### Register Indirect:
                        ```assembly
                        MOV EBX, 100         ; EBX holds memory address 100
                        MOV EAX, [EBX]       ; Dereference: EAX gets contents of memory at EBX
                        ```
                    """.trimIndent(),
                    codeExample = """
                        MOV [100], 42        ; Write 42 to RAM address 100
                        MOV EBX, 100         ; Put address 100 in EBX pointer
                        MOV EAX, [EBX]       ; EAX = *EBX -> EAX is now 42!
                    """.trimIndent(),
                    commonMistakes = "Forgetting brackets when you want to read memory: 'MOV EAX, EBX' copies the address number, not the data at that address!",
                    bestPractices = "Think of square brackets as pointer dereferences `*(address)` in C.",
                    quizQuestion = "If EBX = 200 and memory at 200 contains 88, what does 'MOV EAX, [EBX]' put into EAX?",
                    quizOptions = listOf(
                        "88 (the memory content)",
                        "200 (the address)",
                        "288",
                        "0"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "Square brackets dereference the pointer in EBX (200), fetching the stored value 88 into EAX."
                ),
                X86CurriculumTopic(
                    id = "x86_addressing_modes",
                    unitId = 3,
                    topicNumber = 2,
                    title = "Addressing Modes & The SIB Formula",
                    subtitle = "[Base + Index * Scale + Displacement]",
                    opcodeTag = "SIB",
                    keyConcept = "x86 hardware features a dedicated Memory Management Unit capable of calculating complex address expressions in hardware without extra instructions.",
                    overviewMarkdown = """
                        # The Complete x86 SIB Formula
                        x86 supports the most versatile addressing modes in modern computing:
                        
                        `[ Base + Index * Scale + Displacement ]`
                        
                        - **Base**: Any general register (e.g. RBX, RBP, RDI).
                        - **Index**: Any register (except RSP/ESP) holding an array index or counter.
                        - **Scale**: Multiplier strictly limited to **1, 2, 4, or 8** (matching byte, word, dword, qword element sizes!).
                        - **Displacement**: Constant offset (e.g. struct member offset or stack displacement like +16, -8).
                        
                        ### Real-World Example: Array Access in C
                        `int value = array[i];`
                        In assembly where RBX = array pointer and RCX = index `i`:
                        `MOV EAX, [RBX + RCX * 4]` (since sizeof(int) is 4 bytes!)
                    """.trimIndent(),
                    codeExample = """
                        ; Accessing 32-bit array: int arr[4] = {10, 20, 30, 40};
                        MOV EBX, 100         ; Base address of array
                        MOV ECX, 2           ; Index = 2 (points to 3rd element)
                        MOV EAX, [EBX + ECX * 4] ; Reads address 100 + 2*4 = 108!
                    """.trimIndent(),
                    commonMistakes = "Trying to use scale values other than 1, 2, 4, or 8 (e.g. scale 3 or 5 is impossible in x86 hardware).",
                    bestPractices = "Use scaled indexing for tight loops traversing typed arrays instead of manually multiplying with arithmetic instructions.",
                    quizQuestion = "In the expression '[RBX + RCX * 4 + 8]', if RBX = 100 and RCX = 3, what memory address is accessed?",
                    quizOptions = listOf(
                        "120 (100 + 3*4 + 8 = 120)",
                        "112",
                        "132",
                        "108"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "100 + (3 * 4) + 8 = 100 + 12 + 8 = 120."
                ),
                X86CurriculumTopic(
                    id = "x86_lea_vs_mov",
                    unitId = 3,
                    topicNumber = 3,
                    title = "LEA vs MOV: The Ultimate Contrast",
                    subtitle = "Load Effective Address vs Memory Dereference",
                    opcodeTag = "LEA",
                    keyConcept = "LEA calculates the memory address and stores the calculated number itself into the destination register without touching RAM!",
                    overviewMarkdown = """
                        # LEA: Load Effective Address
                        `LEA dest, [address_expression]`
                        
                        Compare these two instructions:
                        - `MOV EAX, [EBX + ECX*4 + 8]` -> Calculates address, **contacts RAM/cache**, and loads the **data stored there** into EAX.
                        - `LEA EAX, [EBX + ECX*4 + 8]` -> Calculates the address math and places the **calculated address itself** into EAX! No memory is ever accessed!
                        
                        ### Why Compilers Love LEA for Math
                        Because LEA doesn't touch memory and doesn't modify condition flags (RFLAGS), compilers use LEA as a super-fast 3-operand math instruction!
                        Example: `LEA EAX, [EAX + EAX*2]` multiplies EAX by 3 in a single clock cycle!
                    """.trimIndent(),
                    codeExample = """
                        MOV EBX, 100         ; Address
                        MOV [100], 999       ; Value stored at address 100
                        
                        MOV EAX, [EBX]       ; EAX = 999 (fetches data from memory)
                        LEA EDX, [EBX + 16]  ; EDX = 116 (calculates address arithmetic only!)
                    """.trimIndent(),
                    commonMistakes = "Thinking that LEA loads data from memory. LEA NEVER reads or writes RAM!",
                    bestPractices = "Use LEA for computing array pointer offsets or performing quick multiplications by 2, 3, 4, 5, 8, or 9 without clobbering flags.",
                    quizQuestion = "If EBX = 50 and memory at 54 contains 1000, what does 'LEA EAX, [EBX + 4]' put into EAX?",
                    quizOptions = listOf(
                        "54 (the calculated address)",
                        "1000 (the memory content)",
                        "50",
                        "0"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "LEA computes the address math (50 + 4 = 54) and puts the result directly into EAX without touching memory."
                )
            )
        ),
        X86CurriculumUnit(
            id = 4,
            title = "Unit 4: Arithmetic & Logic with Registers/Memory",
            subtitle = "ADD, SUB, XOR, Flags & In-Place Memory Updates",
            iconName = "Calculate",
            description = "Explore arithmetic directly on memory cells, flags in RFLAGS (ZF, CF, SF), and the compiler's signature XOR register-clearing idiom.",
            topics = listOf(
                X86CurriculumTopic(
                    id = "x86_arithmetic_memory",
                    unitId = 4,
                    topicNumber = 1,
                    title = "In-Place Memory Arithmetic",
                    subtitle = "ADD [addr], reg & SUB [addr], imm",
                    opcodeTag = "ADD/SUB",
                    keyConcept = "x86 allows modifying memory locations directly with arithmetic instructions (read-modify-write cycle).",
                    overviewMarkdown = """
                        # In-Place Memory ALU Operations
                        Unlike RISC architectures (like ARM or RISC-V) which can only do math on registers, x86 CISC can perform arithmetic directly on memory!
                        
                        ### Operations Supported:
                        - `ADD [100], EAX` -> Read memory at 100, add EAX, write sum back to memory at 100.
                        - `SUB [100], 5`   -> Decrement the value in memory at 100 by 5.
                        - `INC DWORD PTR [EBX]` -> Increment counter stored in memory.
                        
                        ### Hardware Cost:
                        In-place memory updates require a Read-Modify-Write memory bus cycle, which is slower than register arithmetic but saves instruction count.
                    """.trimIndent(),
                    codeExample = """
                        MOV [100], 10        ; Initialize counter at memory 100
                        MOV EAX, 5
                        ADD [100], EAX       ; Memory at 100 becomes 15!
                        INC [100]            ; Memory at 100 becomes 16!
                    """.trimIndent(),
                    commonMistakes = "Attempting memory-to-memory arithmetic like 'ADD [200], [100]'. Remember: only ONE memory operand is allowed!",
                    bestPractices = "When modifying shared memory between threads, memory ALU instructions need the 'LOCK' prefix for thread safety.",
                    quizQuestion = "If memory at 100 contains 20 and EAX = 10, what does 'ADD [100], EAX' do?",
                    quizOptions = listOf(
                        "Updates memory at 100 to 30",
                        "Updates EAX to 30",
                        "Updates memory at 100 to 10",
                        "Causes a compilation error"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "The destination is [100], so memory at 100 is read (20), EAX is added (10), and the result (30) is written back to memory at 100."
                ),
                X86CurriculumTopic(
                    id = "x86_xor_zeroing",
                    unitId = 4,
                    topicNumber = 2,
                    title = "XOR Register Clearing Idiom",
                    subtitle = "Why 'XOR EAX, EAX' Beats 'MOV EAX, 0'",
                    opcodeTag = "XOR",
                    keyConcept = "Bitwise XOR-ing any register with itself produces zero in fewer instruction bytes and resets flags efficiently.",
                    overviewMarkdown = """
                        # The Classic Compiler Idiom
                        Every assembly programmer and optimizing compiler uses `XOR reg, reg` to set a register to zero.
                        
                        ### Why Not Just Use MOV EAX, 0?
                        1. **Instruction Size**:
                           - `MOV EAX, 0` takes 5 bytes in machine code (`B8 00 00 00 00`).
                           - `XOR EAX, EAX` takes only **2 bytes** (`31 C0`)!
                        2. **Zero-Idiom Recognition**:
                           - Modern CPUs have special silicon circuits that recognize `XOR reg, reg` as a zeroing idiom.
                           - The CPU clears the register directly in the rename stage without waiting for execution units or generating pipeline stalls!
                    """.trimIndent(),
                    codeExample = """
                        ; Compare the two approaches:
                        MOV EAX, 0           ; 5 bytes of machine code
                        XOR EAX, EAX         ; 2 bytes, instantly recognized by CPU rename stage!
                        
                        ; Zeroing flags:
                        ; XOR also sets ZF (Zero Flag) to 1 and clears CF (Carry Flag)
                    """.trimIndent(),
                    commonMistakes = "Using MOV RAX, 0 in loops where code density and instruction cache misses degrade performance.",
                    bestPractices = "Always use XOR to initialize registers to 0, matching what GCC, Clang, and MSVC generate.",
                    quizQuestion = "Why do compilers prefer 'XOR EAX, EAX' over 'MOV EAX, 0'?",
                    quizOptions = listOf(
                        "It produces smaller machine code (2 bytes vs 5) and avoids pipeline stalls",
                        "It sets the register to 1 instead of 0",
                        "MOV EAX, 0 is deprecated in 64-bit mode",
                        "XOR accesses CPU cache faster"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "XOR EAX, EAX takes only 2 opcode bytes compared to 5 bytes for MOV with immediate zero, and modern CPUs execute it with zero latency in the register renamer."
                ),
                X86CurriculumTopic(
                    id = "x86_flags_cmp",
                    unitId = 4,
                    topicNumber = 3,
                    title = "CMP, TEST & The RFLAGS Register",
                    subtitle = "Zero Flag (ZF), Sign Flag (SF) & Carry Flag (CF)",
                    opcodeTag = "FLAGS",
                    keyConcept = "CMP subtracts source from destination without storing the result, only updating RFLAGS for subsequent conditional jumps.",
                    overviewMarkdown = """
                        # The Status Flags (RFLAGS)
                        Comparison instructions update the CPU's status register without changing operand values:
                        
                        - **Zero Flag (ZF)**: Set to 1 if result is 0 (operands were equal).
                        - **Sign Flag (SF)**: Set to 1 if result is negative (most significant bit is 1).
                        - **Carry Flag (CF)**: Set to 1 if an unsigned subtraction borrowed or an add overflowed.
                        
                        ### CMP vs SUB:
                        - `SUB EAX, EBX` -> Calculates `EAX - EBX`, writes difference to EAX, and updates flags.
                        - `CMP EAX, EBX` -> Calculates `EAX - EBX`, discards the difference, and **ONLY** updates flags!
                    """.trimIndent(),
                    codeExample = """
                        MOV EAX, 42
                        MOV EBX, 42
                        CMP EAX, EBX         ; EAX - EBX = 0. ZF becomes 1! EAX is still 42.
                        
                        JE labels_equal      ; Jumps because ZF == 1
                    """.trimIndent(),
                    commonMistakes = "Expecting CMP to alter the destination register. CMP only updates the condition flags!",
                    bestPractices = "Use 'TEST reg, reg' to check if a register is zero or negative without doing a subtraction.",
                    quizQuestion = "If EAX = 50 and EBX = 50, what flag is set by 'CMP EAX, EBX'?",
                    quizOptions = listOf(
                        "ZF (Zero Flag) is set to 1",
                        "CF (Carry Flag) is set to 1",
                        "SF (Sign Flag) is set to 1",
                        "No flags are modified"
                    ),
                    quizCorrectAnswerIndex = 0,
                    quizExplanation = "Since 50 - 50 = 0, the Zero Flag (ZF) is set to 1, indicating that both operands are equal."
                )
            )
        )
    )

    fun getAllTopics(): List<X86CurriculumTopic> {
        return CURRICULUM_UNITS.flatMap { it.topics }
    }

    fun findTopicById(id: String): X86CurriculumTopic? {
        return getAllTopics().find { it.id == id }
    }
}
