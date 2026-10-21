package com.example.model

enum class Architecture(val displayName: String, val codeName: String, val tag: String) {
    X86("x86-64", "x86", "System Programming"),
    ARM("ARM64", "arm", "Embedded & Mobile"),
    OS_DEV("OS Kernel", "os", "Kernel & Compilers"),
    GPU_DEV("GPU Compute", "gpu", "Graphics & SIMD")
}

data class Chapter(
    val id: Int,
    val title: String,
    val architecture: Architecture,
    val description: String,
    val topics: List<Topic>
)

data class Topic(
    val id: String,
    val title: String,
    val overview: String,
    val detailedContent: String = "",
    val codeTemplate: String = "",
    val quiz: Quiz? = null,
    val estimatedMinutes: Int = 10
)

data class Quiz(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

/**
 * Interactive Lesson class representing an x86 assembly tutorial with
 * conceptual explanations, code snippets, hardware register context,
 * memory operations, and embedded quiz checkpoints.
 */
data class Lesson(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val architecture: Architecture = Architecture.X86,
    val difficulty: String = "Beginner", // Beginner, Intermediate, Advanced
    val estimatedMinutes: Int = 8,
    val conceptualExplanation: String,
    val keyTakeaways: List<String> = emptyList(),
    val registersInvolved: List<String> = emptyList(),
    val memoryOperationsExplained: String = "",
    val codeSnippet: String,
    val initialRegisters: Map<String, Int> = emptyMap(),
    val initialMemory: Map<Int, Int> = emptyMap(),
    val quiz: Quiz? = null,
    val exerciseChallenge: String = ""
)

object X86TutorialLessons {
    val LESSONS = listOf(
        Lesson(
            id = "x86_lesson_1_registers",
            title = "x86 General Registers & Immediate Moves",
            subtitle = "Foundations of 64-bit and 32-bit CPU State",
            architecture = Architecture.X86,
            difficulty = "Beginner",
            estimatedMinutes = 8,
            conceptualExplanation = """
# Architectural Foundations of x86 Registers

Registers are ultra-fast physical storage cells situated directly inside the silicon core of the CPU. Unlike system RAM, which requires bus transfers taking tens or hundreds of nanoseconds, register accesses take a fraction of a single clock cycle (< 1 ns).

### The General-Purpose Register File
In x86-64 architecture, there are 16 general-purpose 64-bit registers:
- **RAX / EAX**: The Accumulator register. Universally used for arithmetic and function return values.
- **RBX / EBX**: Base register. Traditionally used as a pointer to structured data.
- **RCX / ECX**: Counter register. Used by loop instructions and bit shifts.
- **RDX / EDX**: Data register. Extended accumulator for 128-bit multiplications and division.
- **RSI / ESI**: Source index for buffer transfers.
- **RDI / EDI**: Destination index for buffer transfers and first argument in SysV AMD64 ABI.

### Immediate Addressing & Register Transfers
The `MOV` instruction copies bit patterns from a source operand into a destination operand:
`MOV dest, src`

1. **Immediate to Register**: `MOV EAX, 42` places the immediate scalar literal 42 directly into register EAX.
2. **Register to Register**: `MOV EBX, EAX` clones the current 32-bit contents of EAX into EBX.

> **Critical Rule**: x86-64 enforces that any 32-bit write (such as to `EAX`) will automatically zero-extend the upper 32 bits of the corresponding 64-bit register (`RAX`) to prevent stale dirty high-bit anomalies.
            """.trimIndent(),
            keyTakeaways = listOf(
                "Registers reside directly on CPU silicon with zero bus latency.",
                "Syntax format is Intel style: MOV destination, source.",
                "Immediate values are constant numbers baked into the instruction bytes.",
                "Writing to a 32-bit subregister (EAX) zeroes the upper 32 bits of RAX."
            ),
            registersInvolved = listOf("EAX", "EBX", "ECX", "EDX"),
            memoryOperationsExplained = "None in this tutorial. Operations are strictly register-internal without RAM bus arbitration.",
            codeSnippet = """
; Lesson 1: Register Transfers & Immediates
MOV EAX, 42       ; Load immediate integer 42 into EAX
MOV EBX, EAX      ; Copy EAX (42) into EBX
ADD EBX, 10       ; Add 10 to EBX (EBX = 52)
MOV ECX, EBX      ; Duplicate EBX into ECX
SUB ECX, 2        ; Subtract 2 from ECX (ECX = 50)
MOV EDX, 100      ; Initialize EDX with base threshold
            """.trimIndent(),
            initialRegisters = mapOf("EAX" to 0, "EBX" to 0, "ECX" to 0, "EDX" to 0),
            initialMemory = mapOf(100 to 0),
            quiz = Quiz(
                question = "What value is held in EBX after executing 'MOV EAX, 42' followed by 'MOV EBX, EAX' and 'ADD EBX, 10'?",
                options = listOf("42", "52", "10", "0"),
                correctAnswerIndex = 1,
                explanation = "EAX is set to 42. Then EBX receives 42 from EAX. Finally, ADD EBX, 10 increases EBX by 10 to yield 52."
            ),
            exerciseChallenge = "Try stepping through each instruction to observe how registers change step by step."
        ),
        Lesson(
            id = "x86_lesson_2_memory_deref",
            title = "Memory Dereferencing & Direct Addressing",
            subtitle = "Reading and Writing RAM via Pointers",
            architecture = Architecture.X86,
            difficulty = "Beginner",
            estimatedMinutes = 10,
            conceptualExplanation = """
# Memory Addressing in x86

While registers provide immediate speed, physical memory (RAM) is where applications hold millions of variables, strings, dynamic heaps, and global arrays.

### The Dereference Operator: Square Brackets `[...]`
In Intel x86 syntax, square brackets `[...]` represent memory dereferencing. It behaves identically to the unary dereference operator `*ptr` in C/C++:
- `MOV EAX, 100` loads the raw numeric literal `100` into `EAX`.
- `MOV EAX, [100]` interprets `100` as an address in RAM and loads the value stored *at address 100* into `EAX`.

### Reading vs Writing Memory
1. **Reading RAM (Memory -> Register)**:
   `MOV EAX, [100]` reads 4 bytes starting at address 100 from memory and deposits them into register EAX.
2. **Writing RAM (Register -> Memory)**:
   `MOV [104], EBX` copies the value in register EBX across the system memory bus and writes it into memory at address 104.

### The Golden Rule of x86 Memory Operations
> **CRITICAL RESTRICTION**: x86 CPUs do **NOT** support memory-to-memory operations in a single instruction!
> `MOV [100], [104]` is an **ILLEGAL INSTRUCTION**.
> To copy from one memory location to another, you MUST use a register as an intermediary:
> 1. `MOV EAX, [100]` (load RAM into register)
> 2. `MOV [104], EAX` (store register to target RAM)
            """.trimIndent(),
            keyTakeaways = listOf(
                "Square brackets [address] indicate a RAM dereference (*ptr).",
                "Direct addressing uses an absolute constant memory address inside brackets.",
                "x86 strictly prohibits memory-to-memory moves (MOV [dst], [src] is invalid).",
                "A temporary register is required as a stepping stone when copying RAM to RAM."
            ),
            registersInvolved = listOf("EAX", "EBX", "ECX"),
            memoryOperationsExplained = "Direct memory read from RAM address [100] and direct store into RAM address [104].",
            codeSnippet = """
; Lesson 2: Direct Memory Dereferencing
MOV EAX, [100]    ; Read value at RAM addr 100 (42) into EAX
MOV EBX, 55       ; Load immediate 55 into EBX
MOV [104], EBX    ; Store EBX (55) into RAM address 104
ADD EAX, 8        ; Calculate EAX = 42 + 8 = 50
MOV [108], EAX    ; Store updated 50 into RAM address 108
MOV ECX, [104]    ; Verify written value from address 104
            """.trimIndent(),
            initialRegisters = mapOf("EAX" to 0, "EBX" to 0, "ECX" to 0),
            initialMemory = mapOf(100 to 42, 104 to 0, 108 to 0),
            quiz = Quiz(
                question = "Which of the following x86 instructions is illegal and rejected by hardware decoders?",
                options = listOf(
                    "MOV EAX, [100]",
                    "MOV [100], EAX",
                    "MOV [100], [104]",
                    "MOV EBX, EAX"
                ),
                correctAnswerIndex = 2,
                explanation = "x86 architecture cannot have both operands as memory locations. MOV [100], [104] is invalid; a register must be used in between."
            ),
            exerciseChallenge = "Watch the RAM Table update in real time as the store instructions execute!"
        ),
        Lesson(
            id = "x86_lesson_3_base_displacement",
            title = "Base + Displacement Addressing",
            subtitle = "Accessing Struct Fields and Stack Variables",
            architecture = Architecture.X86,
            difficulty = "Intermediate",
            estimatedMinutes = 10,
            conceptualExplanation = """
# Pointer Indirection & Displacements

In real software, program data rarely sits at hardcoded constant memory addresses. Data structures such as C structs, objects, and local stack frames are accessed through a base pointer register plus an offset.

### Register Indirect Addressing: `[RBX]`
When a register holds the numerical address of memory, placing brackets around the register accesses that target location:
```nasm
MOV EBX, 100       ; EBX points to memory address 100
MOV EAX, [EBX]     ; EAX receives the value at address 100
```

### Base + Displacement Addressing: `[Base + Offset]`
To read contiguous fields of a struct or items in an array:
`Effective Address = Base Register + Constant Offset`

For example, given a struct:
```c
struct Player {
    int id;      // offset +0
    int score;   // offset +4
    int level;   // offset +8
};
```
If `EBX` holds the base pointer to the player instance (say address 100):
- `[EBX + 0]` accesses `player.id` (address 100)
- `[EBX + 4]` accesses `player.score` (address 104)
- `[EBX + 8]` accesses `player.level` (address 108)

Hardware performs this addition `EBX + 4` inside the CPU Memory Management Unit (MMU) address generation unit before initiating the bus read!
            """.trimIndent(),
            keyTakeaways = listOf(
                "Base registers (EBX, EBP, RSI, RDI) act as pointers to allocated objects.",
                "Displacement is an immediate constant added to the base pointer.",
                "Used by compilers to access struct members (e.g. obj->field) with zero overhead.",
                "The base register itself remains unmodified; only the accessed address shifts."
            ),
            registersInvolved = listOf("EBX", "EAX", "EDX"),
            memoryOperationsExplained = "Indirect base addressing [EBX] and offsetted member reads [EBX + 4] and [EBX + 8].",
            codeSnippet = """
; Lesson 3: Struct Field Access via Base + Displacement
MOV EBX, 100      ; EBX is base pointer to Player struct (RAM 100)
MOV EAX, [EBX]    ; Read player.id at [100] (val: 42)
MOV EDX, [EBX + 4]; Read player.score at [104] (val: 99)
ADD EDX, 1        ; Increment score: 99 + 1 = 100
MOV [EBX + 4], EDX; Write updated score back to struct field
MOV EAX, [EBX + 8]; Read player.level at [108] (val: 15)
            """.trimIndent(),
            initialRegisters = mapOf("EBX" to 100, "EAX" to 0, "EDX" to 0),
            initialMemory = mapOf(100 to 42, 104 to 99, 108 to 15),
            quiz = Quiz(
                question = "If EBX = 100, what physical RAM address is accessed by 'MOV EDX, [EBX + 4]'?",
                options = listOf("Address 100", "Address 104", "Address 400", "Address 140"),
                correctAnswerIndex = 1,
                explanation = "The MMU computes 100 + 4 = 104, so RAM memory location 104 is accessed."
            ),
            exerciseChallenge = "Observe that EBX remains 100 while memory at address 104 updates to 100."
        ),
        Lesson(
            id = "x86_lesson_4_sib_addressing",
            title = "SIB Addressing: Base + Index * Scale + Disp",
            subtitle = "Hardware-Accelerated Array Indexing",
            architecture = Architecture.X86,
            difficulty = "Intermediate",
            estimatedMinutes = 12,
            conceptualExplanation = """
# Scaled Index Base (SIB) Addressing

High-level languages frequently index arrays: `array[i]`. Because integers are 4 bytes (or 8 bytes for 64-bit pointers), finding the byte address requires multiplying the element index `i` by the element size.

### The x86 SIB Formula
x86 provides dedicated hardware circuitry (the AGU - Address Generation Unit) that computes the full array equation in a single clock cycle:
`Effective Address = Base + (Index * Scale) + Displacement`

Where:
- **Base**: Any General-Purpose Register holding the array starting address (e.g., `EBX`).
- **Index**: Any General-Purpose Register holding the element index (e.g., `ESI` = 0, 1, 2, ...).
- **Scale**: A power of 2: **1, 2, 4, or 8** (matching Byte, Word, DWord, or QWord sizes).
- **Displacement**: Optional fixed offset constant.

### Example in Action:
Suppose an array of 32-bit integers (`scale = 4`) begins at address 100 (`EBX = 100`):
- Element 0 (`ESI = 0`): `[EBX + ESI * 4]` -> `100 + 0 = 100`
- Element 1 (`ESI = 1`): `[EBX + ESI * 4]` -> `100 + 4 = 104`
- Element 2 (`ESI = 2`): `[EBX + ESI * 4]` -> `100 + 8 = 108`

This enables iterating through arrays without manually multiplying or updating pointers!
            """.trimIndent(),
            keyTakeaways = listOf(
                "Scale must be 1, 2, 4, or 8 to align with native data type sizes.",
                "Index register is multiplied by scale in hardware without altering the index register.",
                "Eliminates the need for separate multiply instructions when indexing arrays.",
                "Compilers rely heavily on SIB addressing for loops and multi-dimensional matrices."
            ),
            registersInvolved = listOf("EBX", "ESI", "EAX"),
            memoryOperationsExplained = "Hardware scaled indexing [EBX + ESI * 4] reading sequential elements from a 4-byte integer array.",
            codeSnippet = """
; Lesson 4: SIB Array Indexing
MOV EBX, 100      ; Array base pointer = RAM address 100
MOV ESI, 0        ; Index i = 0
MOV EAX, [EBX + ESI * 4] ; Read array[0] at [100] -> val: 42
INC ESI           ; Index i = 1
MOV EAX, [EBX + ESI * 4] ; Read array[1] at [104] -> val: 99
INC ESI           ; Index i = 2
MOV EAX, [EBX + ESI * 4] ; Read array[2] at [108] -> val: 15
            """.trimIndent(),
            initialRegisters = mapOf("EBX" to 100, "ESI" to 0, "EAX" to 0),
            initialMemory = mapOf(100 to 42, 104 to 99, 108 to 15),
            quiz = Quiz(
                question = "Why does the x86 SIB scale factor only allow 1, 2, 4, or 8?",
                options = listOf(
                    "They correspond directly to 1, 2, 4, and 8 byte standard computer data types",
                    "The instruction format only has 2 bits to encode the scale multiplier",
                    "Both A and B are true",
                    "CPU registers cannot perform multiplications by odd numbers"
                ),
                correctAnswerIndex = 2,
                explanation = "Both are true: 2 bits encode {1, 2, 4, 8}, perfectly matching 1-byte chars, 2-byte shorts, 4-byte ints, and 8-byte pointers/doubles."
            ),
            exerciseChallenge = "Step through the array traversal and watch ESI increment as EAX retrieves each sequential item!"
        ),
        Lesson(
            id = "x86_lesson_5_lea_vs_mov",
            title = "LEA vs MOV: Pointer Math vs Dereferencing",
            subtitle = "Mastering Load Effective Address",
            architecture = Architecture.X86,
            difficulty = "Intermediate",
            estimatedMinutes = 10,
            conceptualExplanation = """
# The Fundamental Difference Between LEA and MOV

One of the most common stumbling blocks for assembly programmers is understanding the difference between `MOV` and `LEA` (`Load Effective Address`).

### Look at the Brackets:
Both instructions accept square brackets, but they do completely different things!

1. **`MOV EAX, [EBX + 4]` (Dereferences Memory)**:
   - Computes address: `EBX + 4`
   - Contacts the RAM memory bus
   - Reads the 32-bit data *stored at that memory address*
   - Places the data into `EAX`
   - *Analogy*: Going to an address on a street, knocking on the door, and meeting the person inside.

2. **`LEA EAX, [EBX + 4]` (Calculates the Address Only)**:
   - Computes address: `EBX + 4`
   - Does **NOT** access memory! Zero RAM bus traffic!
   - Directly places the calculated address number into `EAX`
   - *Analogy*: Writing down an address on a notepad without traveling there.

### The Compiler's Favorite Arithmetic Trick
Because `LEA` uses the address generation unit (capable of `Base + Index * Scale + Disp`), optimizing compilers frequently use `LEA` as a fast general-purpose math instruction for three-operand additions and multiplications!
- `LEA EAX, [EBX + ECX * 2 + 5]` calculates `EAX = EBX + 2*ECX + 5` in 1 single clock cycle without touching flags or memory!
            """.trimIndent(),
            keyTakeaways = listOf(
                "MOV dereferences and fetches the data stored inside memory.",
                "LEA calculates the address itself and stores the address in the destination register.",
                "LEA never touches RAM and never triggers memory page faults.",
                "Compilers use LEA as a 3-operand math shortcut (e.g. y = x * 3 + 4)."
            ),
            registersInvolved = listOf("EBX", "EAX", "ECX", "EDX"),
            memoryOperationsExplained = "Contrast of memory fetch with MOV [EBX + 4] vs pure address arithmetic with LEA [EBX + 4].",
            codeSnippet = """
; Lesson 5: LEA vs MOV Comparison
MOV EBX, 100      ; EBX is base address 100 (contains value 42)
MOV EAX, [EBX]    ; MOV dereferences: EAX = 42 (content of RAM 100)
LEA ECX, [EBX]    ; LEA computes address: ECX = 100 (the address itself!)
MOV EDX, [EBX + 4]; MOV dereferences: EDX = 99 (content of RAM 104)
LEA ECX, [EBX + 4]; LEA computes address: ECX = 104 (100 + 4)
            """.trimIndent(),
            initialRegisters = mapOf("EBX" to 100, "EAX" to 0, "ECX" to 0, "EDX" to 0),
            initialMemory = mapOf(100 to 42, 104 to 99),
            quiz = Quiz(
                question = "If EBX = 100 and memory at 100 contains 42, what is stored in EAX after 'LEA EAX, [EBX]'?",
                options = listOf("42", "100", "0", "An invalid pointer fault"),
                correctAnswerIndex = 1,
                explanation = "LEA loads the effective address itself (100), not the memory contents (42). MOV EAX, [EBX] would have loaded 42."
            ),
            exerciseChallenge = "Run both instructions and contrast the values stored into EAX (value 42) versus ECX (address 100)."
        ),
        Lesson(
            id = "x86_lesson_6_stack_operations",
            title = "x86 Stack Mechanics: PUSH & POP",
            subtitle = "LIFO Storage, RSP Tracking, and Call Frames",
            architecture = Architecture.X86,
            difficulty = "Advanced",
            estimatedMinutes = 12,
            conceptualExplanation = """
# The Hardware Execution Stack

The stack is a contiguous region of memory organized as a Last-In, First-Out (LIFO) data structure. It is managed automatically by hardware through the Stack Pointer register (`RSP` / `ESP`).

### Downward Growth Direction
In x86 architecture, the stack **grows downwards** toward lower memory addresses:
- **`PUSH src`**:
  1. Decrements the stack pointer: `RSP = RSP - 8` (or `ESP = ESP - 4` for 32-bit operands)
  2. Writes the value of `src` to memory at `[RSP]`
- **`POP dest`**:
  1. Reads the value from memory at `[RSP]` into `dest`
  2. Increments the stack pointer: `RSP = RSP + 8` (or `ESP = ESP + 4`)

### Stack Invariants & Register Preservation
When calling a subroutine or function:
1. Volatile registers can be saved using `PUSH` before a computation.
2. After the computation completes, `POP` restores the registers in reverse order.
3. If you push `EAX` and then `EBX`, you must pop `EBX` first, then `EAX`!
            """.trimIndent(),
            keyTakeaways = listOf(
                "The x86 stack grows downward in memory towards lower addresses.",
                "PUSH decrements the stack pointer, then writes to memory.",
                "POP reads from memory, then increments the stack pointer.",
                "LIFO order requires popping in exact reverse sequence of pushing."
            ),
            registersInvolved = listOf("ESP", "EAX", "EBX", "ECX"),
            memoryOperationsExplained = "Stack push writes to [ESP - 4] and stack pop reads from [ESP].",
            codeSnippet = """
; Lesson 6: Stack PUSH and POP
MOV ESP, 120      ; Initialize stack pointer to address 120
MOV EAX, 777      ; Place 777 in EAX
PUSH EAX          ; ESP drops to 116, [116] = 777
MOV EBX, 888      ; Place 888 in EBX
PUSH EBX          ; ESP drops to 112, [112] = 888
MOV ECX, 0        ; Reset ECX
POP ECX           ; ECX receives 888, ESP rises to 116
POP EBX           ; EBX receives 777, ESP rises to 120
            """.trimIndent(),
            initialRegisters = mapOf("ESP" to 120, "EAX" to 0, "EBX" to 0, "ECX" to 0),
            initialMemory = mapOf(112 to 0, 116 to 0, 120 to 0),
            quiz = Quiz(
                question = "When executing a 32-bit 'PUSH EAX' instruction in x86, how does ESP change?",
                options = listOf(
                    "ESP is incremented by 4 (+4)",
                    "ESP is decremented by 4 (-4)",
                    "ESP remains unchanged",
                    "ESP is multiplied by 4"
                ),
                correctAnswerIndex = 1,
                explanation = "In x86, the stack grows downwards toward lower memory addresses, so PUSH decrements ESP by 4 bytes (or 8 bytes for 64-bit RSP)."
            ),
            exerciseChallenge = "Watch the stack pointer ESP change and memory cells update in real time as values are pushed and popped!"
        )
    )
}

