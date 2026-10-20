package com.example.model

object Syllabus {

    val PATHS = Architecture.values()

    // Retrieve the chapters for a given architecture
    fun getChaptersFor(architecture: Architecture): List<Chapter> {
        val list = mutableListOf<Chapter>()
        
        // Let's generate 5 chapters per architecture.
        // Each chapter will contain 10 topics, total 50 topics per architecture -> 4 architectures = 200 lessons!
        for (chapId in 1..5) {
            val title = getChapterTitle(architecture, chapId)
            val desc = getChapterDescription(architecture, chapId)
            val topics = mutableListOf<Topic>()
            
            for (topicId in 1..10) {
                val absoluteTopicNumber = (chapId - 1) * 10 + topicId
                val topicKey = "${architecture.codeName}_chap${chapId}_top${topicId}"
                val topicTitle = getTopicTitle(architecture, chapId, topicId)
                val topicOverview = getTopicOverview(architecture, chapId, topicId)
                
                // Detailed text content
                val detailedContent = getDetailedLessonText(architecture, chapId, topicId, topicTitle)
                
                // Code templates
                val template = getCodeTemplate(architecture, chapId, topicId)
                
                // Quiz
                val quiz = getQuizForTopic(architecture, chapId, topicId, topicTitle)
                
                topics.add(
                    Topic(
                        id = topicKey,
                        title = topicTitle,
                        overview = topicOverview,
                        detailedContent = detailedContent,
                        codeTemplate = template,
                        quiz = quiz,
                        estimatedMinutes = 8 + (topicId % 5)
                    )
                )
            }
            
            list.add(
                Chapter(
                    id = chapId,
                    title = "Chapter $chapId: $title",
                    architecture = architecture,
                    description = desc,
                    topics = topics
                )
            )
        }
        return list
    }

    private fun getChapterTitle(arch: Architecture, id: Int): String {
        return when (arch) {
            Architecture.X86 -> when (id) {
                1 -> "Silicon & 64-bit Registers"
                2 -> "Memory Layout & Addressing Modes"
                3 -> "Arithmetic, Flags & Boolean Logic"
                4 -> "The Stack, Frames & Subroutines"
                5 -> "OS Integration & Linux Syscalls"
                else -> "Advanced Topics"
            }
            Architecture.ARM -> when (id) {
                1 -> "Load/Store Architecture & R0-R30"
                2 -> "The Barrel Shifter & Branching"
                3 -> "Memory Manipulation & Alignments"
                4 -> "Subroutines, Link Registers & Frames"
                5 -> "Vector Processing & NEON SIMD"
                else -> "Advanced ARM Concepts"
            }
            Architecture.OS_DEV -> when (id) {
                1 -> "CPU Bootstrapping & Real Mode x86"
                2 -> "Memory Segmentation & VGA Graphics"
                3 -> "The Global Descriptor Table (GDT)"
                4 -> "Interrupt Service Routines (ISRs)"
                5 -> "Building Custom Compilers & Linkers"
                else -> "Kernel Internals"
            }
            Architecture.GPU_DEV -> when (id) {
                1 -> "Graphics Hardware & SIMD Concept"
                2 -> "Vector Registers & Scalar Execution"
                3 -> "Execution Masks & Parallel Divergence"
                4 -> "Shader Pipelines & Pixel Processing"
                5 -> "Modern Compute Shaders & CUDA API"
                else -> "High-Performance GPU"
            }
        }
    }

    private fun getChapterDescription(arch: Architecture, id: Int): String {
        return when (arch) {
            Architecture.X86 -> "Master x86-64 assembly, the foundation of modern high-performance desktop and server systems."
            Architecture.ARM -> "Explore AArch64 load-store concepts driving Apple Silicon, Raspberry Pi, and mobile chips."
            Architecture.OS_DEV -> "Step into real mode, configure GDTs, program BIOS interrupts, and design custom compiler parsers."
            Architecture.GPU_DEV -> "Harness massive vector parallelism. Learn execution masks, wave-fronts, and write SIMD compute kernels."
        }
    }

    private fun getTopicTitle(arch: Architecture, chapId: Int, topicId: Int): String {
        val baseNum = (chapId - 1) * 10 + topicId
        return when (arch) {
            Architecture.X86 -> when (baseNum) {
                1 -> "Silicon Architecture & Execution Cycle"
                2 -> "Primary 64-bit General Purpose Registers"
                3 -> "Register Sizing: RAX, EAX, AX, AL"
                4 -> "MOV Instruction & Immediate Data"
                5 -> "Direct & Indirect Register Transfers"
                6 -> "Data Hazards & Pipeline Stalls"
                7 -> "Instruction Pointer (RIP) Mechanics"
                8 -> "Status Register (RFLAGS) Layout"
                9 -> "System Clocks & Instruction Latency"
                10 -> "Chapter 1 Lab: Basic Data Moving"
                
                11 -> "Linear Memory & RAM Physical Model"
                12 -> "Memory Segments & Flat Memory Model"
                13 -> "Base-Displacement Addressing"
                14 -> "Scaled-Indexed Addressing Mode"
                15 -> "Pointer Sizes & Direct Memory Loading"
                16 -> "Data Alignment Rules (32-bit & 64-bit)"
                17 -> "Endianness: Little-Endian Storage"
                18 -> "LEA vs MOV: Computing Addresses"
                19 -> "Memory-to-Memory Rules"
                20 -> "Chapter 2 Lab: Addressing Arrays"
                
                21 -> "Integer Addition (ADD) & Subtraction (SUB)"
                22 -> "Multiplication (MUL/IMUL) & Quadwords"
                23 -> "Division (DIV/IDIV) & Registers"
                24 -> "Increment (INC) & Decrement (DEC) Opcodes"
                25 -> "Bitwise Logic: AND, OR, XOR Operations"
                26 -> "Register Clearing: XOR EAX, EAX Optimization"
                27 -> "Shift Instructions (SHL, SHR, SAR)"
                28 -> "Compare (CMP) & Test (TEST) Mechanics"
                29 -> "Conditional Jumps: JZ, JNZ, JE, JNE"
                30 -> "Chapter 3 Lab: Building Branching Loops"
                
                31 -> "The Stack Architecture & ESP Pointer"
                32 -> "Pushing & Popping Data (PUSH / POP)"
                33 -> "Call Frames & Subroutine Call (CALL)"
                34 -> "Returning from Procedures (RET)"
                35 -> "Stack Alignment & Security Cookies"
                36 -> "Frame Pointer (EBP) & Stack Frames"
                37 -> "Local Variables Allocation on Stack"
                38 -> "Recursion Stack Overflows"
                39 -> "Calling Conventions: System V vs MS"
                40 -> "Chapter 4 Lab: Custom Recursive Math"
                
                41 -> "Introduction to OS Kernel Spaces"
                42 -> "Linux Syscalls Interface: INT 0x80"
                43 -> "64-bit Syscalls: SYSCALL Instruction"
                44 -> "The Write Syscall (sys_write) Format"
                45 -> "The Exit Syscall (sys_exit) Format"
                46 -> "File Open/Close Syscalls"
                47 -> "Memory Buffers Allocation"
                48 -> "AVX/SSE Vector Registers"
                49 -> "Secure Code Writing & Overflow Patches"
                50 -> "Chapter 5 Lab: Interactive Boot Loader Simulation"
                
                else -> "Lesson $baseNum: x86 Deep-Dive"
            }
            Architecture.ARM -> when (baseNum) {
                1 -> "Load-Store Philosophy"
                2 -> "General Registers R0 to R30"
                3 -> "64-bit vs 32-bit: X0 vs W0 registers"
                4 -> "Immediate Bounds & MOV"
                5 -> "Register Zero (XZR/WZR) Magic"
                6 -> "Execution Pipeling & Hazard Slots"
                7 -> "The Program Counter (PC)"
                8 -> "Current Program Status Register (CPSR)"
                9 -> "AArch64 vs AArch32 Execution States"
                10 -> "Chapter 1 Lab: Arm Register Swapping"
                
                11 -> "ARM Barrel Shifter Hardware"
                12 -> "Logical Shift Left (LSL) in ALU"
                13 -> "Logical Shift Right (LSR) & Arithmetic"
                14 -> "Rotate Right (ROR) Operations"
                15 -> "Branch Instruction (B) Mechanics"
                16 -> "Conditional Branching: BEQ & BNE"
                17 -> "Compare & Branch on Zero (CBZ / CBNZ)"
                18 -> "Compare (CMP) & Condition Suffixes"
                19 -> "ARM Instruction Encoding"
                20 -> "Chapter 2 Lab: Bit Manipulation Loops"
                
                21 -> "Loading Memory with LDR"
                22 -> "Storing Memory with STR"
                23 -> "Pre-indexed Memory Addressing"
                24 -> "Post-indexed Memory Addressing"
                25 -> "Register Offset Addressing"
                26 -> "Immediate Offset Alignments"
                27 -> "Loading Signed Values (LDRSB / LDRSH)"
                28 -> "Single-Byte Data Transfers"
                29 -> "Bulk Transfers: LDP & STP Pairs"
                30 -> "Chapter 3 Lab: Memory Copy Program"
                
                31 -> "ARM Link Register (LR / X30)"
                32 -> "Branch with Link (BL) for Subroutines"
                33 -> "Branch Exchange (BX) & Subroutine Returns"
                34 -> "Stack Pointer (SP) Constraints"
                35 -> "Push & Pop Equivalents in ARM64"
                36 -> "Preserving Caller vs Callee Registers"
                37 -> "The Frame Pointer (FP / X29)"
                38 -> "Leaf Function Optimizations"
                39 -> "ARM64 AAPCS Calling Convention"
                40 -> "Chapter 4 Lab: Nested Subroutines"
                
                41 -> "Introduction to ARM SIMD Vectors"
                42 -> "NEON Register File (V0-V31)"
                43 -> "Packed Integer Math"
                44 -> "Floating-point Vector Add (FADD)"
                45 -> "SVE (Scalable Vector Extension)"
                46 -> "Hardware Cryptography Acceleration"
                47 -> "ARM Virtualization Extensions"
                48 -> "System Register Configuration (MSR / MRS)"
                49 -> "Secure Boot & TrustZone Assembly"
                50 -> "Chapter 5 Lab: Vector Dot Product Engine"
                
                else -> "Lesson $baseNum: ARM Deep-Dive"
            }
            Architecture.OS_DEV -> when (baseNum) {
                1 -> "The Power-On Self-Test (POST)"
                2 -> "The BIOS Interrupt Vectors"
                3 -> "Master Boot Record (MBR) Structure"
                4 -> "The Magic Boot Signature (0xAA55)"
                5 -> "x86 16-bit Real Mode Architecture"
                6 -> "Real Mode Addressing: Segment * 16 + Offset"
                7 -> "Segment Registers: CS, DS, ES, SS"
                8 -> "VGA Text Buffer (0xB8000)"
                9 -> "Bios Video Interrupt INT 0x10"
                10 -> "Chapter 1 Lab: Print 'HELLO WORLD' Bootloader"
                
                11 -> "A20 Gate: Unlocking 1MB Memory Barrier"
                12 -> "Global Descriptor Table (GDT) Concept"
                13 -> "GDT Descriptor Struct Layout"
                14 -> "Creating Code & Data Segments"
                15 -> "The GDTR Register & LGDT Opcode"
                16 -> "The Control Register CR0"
                17 -> "Enabling PE (Protected Mode Enable) Bit"
                18 -> "Far Jump (JMP 0x08:Offset) Mechanics"
                19 -> "Protected Mode Stack Configuration"
                20 -> "Chapter 2 Lab: Enter Protected Mode"
                
                21 -> "Interrupt Descriptor Table (IDT)"
                22 -> "Interrupt Gates vs Trap Gates"
                23 -> "Keyboard Controller Port 0x60"
                24 -> "Handling Hardware IRQs (PIC 8259)"
                25 -> "Programmable Interval Timer (PIT)"
                26 -> "Task State Segments (TSS)"
                27 -> "Memory Paging & CR3 Register"
                28 -> "Page Directories & Tables (PTE)"
                29 -> "Enabling Paging (CR0.PG Bit)"
                30 -> "Chapter 3 Lab: Interrupt Keyboard Input"
                
                31 -> "Role of a Compiler: Code Generation"
                32 -> "Abstract Syntax Trees (AST)"
                33 -> "Register Allocation Algorithms"
                34 -> "Three-Address Intermediate Code (IR)"
                35 -> "Stack Machines vs Register Machines"
                36 -> "Generating x86 Opcodes from C"
                37 -> "Linking & ELF Binary Format"
                38 -> "Symbol Resolution & Relocations"
                39 -> "Creating Static Libraries (.a)"
                40 -> "Chapter 4 Lab: Write an Assembly Generator"
                
                41 -> "Multi-Core Bootstrapping (APIC)"
                42 -> "Context Switching Code"
                43 -> "Page Fault Exception Handlers"
                44 -> "User Mode Ring 3 Transitions"
                45 -> "TSS Task Gates"
                46 -> "Syscall Handler Registration"
                47 -> "Memory Managers: Kernel kmalloc"
                48 -> "PCI Bus Scanning Assembly"
                49 -> "Vesa Bios Extensions (VBE) Graphics"
                50 -> "Chapter 5 Lab: Micro-Kernel Scheduler"
                
                else -> "Lesson $baseNum: OS Dev Deep-Dive"
            }
            Architecture.GPU_DEV -> when (baseNum) {
                1 -> "The CPU vs GPU Processing Model"
                2 -> "Understanding CUDA & Shader Pipelines"
                3 -> "SIMD (Single Instruction Multiple Data)"
                4 -> "Wavefronts, Warps & Threads Layout"
                5 -> "Vector ALU Hardware Execution"
                6 -> "Scalar Unit vs Vector Unit"
                7 -> "GPU Global Memory Model"
                8 -> "Shared Memory & Core Cache"
                9 -> "Coalesced Memory Access Rules"
                10 -> "Chapter 1 Lab: Scalar vs Vector Core Setup"
                
                11 -> "GPU Scalar Registers (S0-S3)"
                12 -> "GPU Vector Registers (V0-V31)"
                13 -> "SIMD Lanes Parallel Mapping"
                14 -> "Vector Math: V_ADD & V_SUB"
                15 -> "Multiply-Accumulate (V_MAD) Hardware"
                16 -> "Vector Loading from GPU L1 Cache"
                17 -> "Scalar Branching (S_BRANCH)"
                18 -> "SIMD Thread Synchronization"
                19 -> "Clock Domains in AMD & NVIDIA Architectures"
                20 -> "Chapter 2 Lab: Matrix Multiply SIMD Core"
                
                21 -> "Thread Masking & Execution Mask (EXEC)"
                22 -> "Parallel Execution Divergence"
                23 -> "SIMD Flattening & Wave Control"
                24 -> "Vector Compares: V_CMP"
                25 -> "Predicated Execution Mechanics"
                26 -> "Register Bank Conflicts"
                27 -> "Dynamic Execution Branch Masking"
                28 -> "Texture Sampler Units Assembly"
                29 -> "Constant Cache Buffers"
                30 -> "Chapter 3 Lab: Parallel Conditional Filter"
                
                31 -> "Graphics Shaders Pipelines"
                32 -> "Vertex Shader Assembly (VS)"
                33 -> "Pixel/Fragment Shader Assembly (PS)"
                34 -> "Interpolation of Vertex Attributes"
                35 -> "Frame-buffer Blending Operations"
                36 -> "Color Buffer Registers Map"
                37 -> "SIMD Texture Coordinate Interpolation"
                38 -> "Outputting Colors: V_OUT Instruction"
                39 -> "Double Buffering Graphics Sync"
                40 -> "Chapter 4 Lab: Render Gradient Pixel Shader"
                
                41 -> "Modern Compute Shaders Opcodes"
                42 -> "Thread Blocks & Block Index (CTA)"
                43 -> "CUDA PTX (Parallel Thread Execution) IR"
                44 -> "Atomic Operations on Global Memory"
                45 -> "GPU Register Spills to Local Memory"
                46 -> "SIMD Sorting Algorithms"
                47 -> "Asynchronous Memory Prefetching"
                48 -> "Volatile Keyword in GPU Assembly"
                49 -> "GPU Tensor Core / Matrix Opcodes"
                50 -> "Chapter 5 Lab: Compute Shader Mandelbrot SIMD"
                
                else -> "Lesson $baseNum: GPU Deep-Dive"
            }
        }
    }

    private fun getTopicOverview(arch: Architecture, chapId: Int, topicId: Int): String {
        return "Learn the architectural mechanics of " + getTopicTitle(arch, chapId, topicId) + ". Expand to view detailed code specs, compiler simulations, and custom quizzes."
    }

    private fun getDetailedLessonText(arch: Architecture, chapId: Int, topicId: Int, title: String): String {
        return when (arch) {
            Architecture.OS_DEV -> {
                val coreConcept = when (chapId) {
                    1 -> "Bootloader Development. Booting begins in 16-bit Real Mode on x86, reading sector 0 of the drive into address 0x7C00. This stage configures segment registers and detects memory layout."
                    2 -> "Kernel Context Switching. Saving general-purpose registers (EAX, ESP, EIP) to the process stack, modifying the page directory base register, and restoring the next process's registers."
                    3 -> "Hardware Interrupt Vectors. The CPU queries the Interrupt Descriptor Table (IDT) using an index from an IRQ line, pushing current state to the supervisor stack before execution."
                    4 -> "Memory Page Isolation. Configuring Page Directories (CR3) and Page Tables. This maps virtual addresses to physical pages and implements User/Supervisor ring isolation flags."
                    else -> "Low-level Kernel-to-Hardware Serial and BIOS Interactions. Communicating directly with device controllers using IN/OUT instructions and memory-mapped register regions."
                }
                """
                    # OS Kernel Dev: $title
                    
                    Welcome to the specialized systems-level module on **OS Kernel Development Assembly**!
                    
                    ### Systems Engineering Overview
                    Operating system development requires bypassing high-level runtime boundaries. Here, the assembly programmer is responsible for orchestrating the Silicon directly:
                    
                    - **Silicon Boot Sequence**: Hardware initialization starts in constrained processor modes (e.g., x86 Real Mode). Developers manage segmentation registers and physical device tables before transitioning to higher environments.
                    - **Supervisor Rings & Protections**: Ring 0 (Kernel Space) grants unrestricted opcode access, while Ring 3 (User Space) runs isolated processes. Privilege levels are strictly guarded by Segment Selectors.
                    - **Kernel State Control**: Systems programmers manipulate system Control Registers (e.g., `CR0` to enable protected mode, `CR3` to bind virtual page tables) directly via Assembly opcodes.
                    
                    ### Chapter Objective: $coreConcept
                    
                    ### Key Architectural Mechanics
                    1. **Interrupt Vector Tables (IVT/IDT)**: Core event handlers are registered at static memory ranges. System interrupts push the instruction pointer (`EIP`/`PC`) and processor flags to the stack before jumping to handlers.
                    2. **Hardware IO Ports**: Communication with standard hardware ports (like the Serial UART 0x3F8, or Disk Controller) utilizes physical bus transfer instructions (e.g., x86 `IN` / `OUT` or ARM MMIO registers).
                    3. **The Boot Signature**: A classic x86 boot sector must fit exactly inside 512 bytes, ending in the Magic Word `0xAA55` at offsets 510-511 to trigger BIOS loader handoff.
                    
                    ### Inline Simulation Specs
                    Analyze the source code template below. It contains boot-level interrupt loops, register transitions, and control modifications. Use the **Inline Hardware Emulator** to step instructions and observe how register buffers change state.
                """.trimIndent()
            }
            Architecture.GPU_DEV -> {
                val coreConcept = when (chapId) {
                    1 -> "SIMD Massive Parallelism. GPU architectures utilize Single Instruction, Multiple Data execution blocks to run identical mathematical routines across hundreds of active threads."
                    2 -> "SIMD Masking & Branch Divergence. If threads within a warp diverge on conditional branches, execution lanes are masked off sequentially, resulting in execution penalties."
                    3 -> "Vector Framebuffer Shaders. Writing colors to memory-mapped pixel lanes in massive buffers to compute rendering pipelines, fractals, or geometric transformations."
                    4 -> "Compute Shader Kernels. Performing matrix convolutions, array multiplications, and scientific data manipulation with direct access to low-latency Shared Registers."
                    else -> "GPU Memory Hierarchy and Latency Hiding. Managing registers, thread-local memory, and thread-group shared memory (SRAM) to eliminate DRAM bus stalls."
                }
                """
                    # GPU Compute: $title
                    
                    Welcome to the specialized high-throughput module on **GPU SIMD Compute Assembly**!
                    
                    ### Silicon Architecture Overview
                    Graphics Processing Units depart from latency-optimized CPU architectures in favor of **Massive Throughput-Optimized Thread Parallelism**:
                    
                    - **Vector Engines (SIMD)**: Instead of a single complex core, a GPU comprises hundreds of Arithmetic Logic Units (ALUs) controlled by a single Instruction Scheduler.
                    - **Hardware Lane Masking**: Lanes are active or inactive based on Execution Mask register flags. Conditionally branched threads execute in serialized cycles with appropriate lanes masked.
                    - **Memory Coalescing**: Efficient GPU assemblies align memory requests across adjacent execution lanes into unified memory bus transactions, hiding latency.
                    
                    ### Chapter Objective: $coreConcept
                    
                    ### Key Parallel Mechanics
                    1. **ALU Scalar vs Vector registers**: Scalar registers hold state uniform across all active lanes, while Vector registers hold unique coordinate, depth, and color values for each individual lane.
                    2. **SIMD Compute Kernels**: Compute shaders utilize special index registers (like Lane ID or Thread ID) to calculate memory offsets for mathematical convolutions or raster pipelines.
                    3. **Shader Pipeline Rendering**: Assembly kernels execute mathematical instructions directly onto the Framebuffer (such as our simulated 8x8 pixels grid) to write pixel color channels.
                    
                    ### Inline Simulation Specs
                    Analyze the shader template below. It manipulates SIMD pixel grid loops, loops coordinates, and writes colors. Use the **Inline Hardware Emulator** below to step instructions and visualize execution register changes.
                """.trimIndent()
            }
            else -> {
                """
                    # $title
                    
                    Welcome to this critical core module on **${arch.displayName} Assembly**!
                    
                    ### Core Concepts
                    In this lesson, we break down the hardware execution of $title. Writing code in assembly requires working directly with Silicon registers, understanding memory timings, and operating the CPU with surgical precision.
                    
                    ### Key Architectural Insights
                    - **Data Movement**: Opcodes govern data flows directly across silicon buses, from RAM to L1 Cache, and finally into processor register files.
                    - **Registers**: Silicon registers are memory cells built directly inside the CPU core. They execute instructions with zero-latency delays.
                    - **Arithmetic & Logic Unit (ALU)**: The ALU performs logical operations (AND, OR, XOR) and arithmetic, updating status flags (Zero Flag, Carry Flag) in real-time.
                    - **Platform Compatibility**: This course provides deep knowledge compatible with building native OS compilers, loaders, and running ultra-low-latency GPU SIMD compute lanes.
                    
                    ### Interactive Sandbox Instructions
                    Scroll down and use the **Inline Hardware Emulator** to load the pre-coded assembly template. Step through line-by-line to watch how registers, memory, and flags change dynamically!
                """.trimIndent()
            }
        }
    }

    private fun getCodeTemplate(arch: Architecture, chapId: Int, topicId: Int): String {
        return when (arch) {
            Architecture.X86 -> """
                ; x86-64 Assembly Template
                ; Learn registers and addition
                MOV EAX, 10      ; Move immediate value 10 into EAX
                MOV EBX, 20      ; Move immediate value 20 into EBX
                ADD EAX, EBX     ; Add EBX to EAX (EAX becomes 30)
                SUB EAX, 5       ; Subtract 5 from EAX (EAX becomes 25)
                CMP EAX, 25      ; Compare EAX with 25 (sets Zero Flag!)
            """.trimIndent()

            Architecture.ARM -> """
                ; ARM64 Assembly Template
                ; Register operations and bit manipulation
                MOV R0, #5       ; Load immediate 5 into register R0
                MOV R1, #12      ; Load immediate 12 into register R1
                ADD R2, R0, R1   ; R2 = R0 + R1 (R2 becomes 17)
                SUB R3, R2, #2   ; R3 = R2 - 2 (R3 becomes 15)
                CMP R3, #15      ; Compare R3 with 15 (Sets Zero Flag)
            """.trimIndent()

            Architecture.OS_DEV -> """
                ; OS Kernel Boot Sector Code
                ; Simulate Real Mode and BIOS printing
                ORG 0x7C00       ; Bootloader origin offset
                MOV AL, 'O'      ; Place ASCII 'O' in AL
                INT 0x10         ; Trigger BIOS Print interrupt
                MOV AL, 'S'      ; Place ASCII 'S' in AL
                INT 0x10         ; Trigger BIOS Print interrupt
                
                ; Transition to Protected Mode!
                MOV EAX, CR0     ; Get Control Register 0
                OR EAX, 1        ; Set PE (Protected Enable) bit
                MOV CR0, EAX     ; Write CR0 - Switches CPU!
            """.trimIndent()

            Architecture.GPU_DEV -> """
                ; GPU SIMD Shader Code
                ; Run in parallel on 4 Vector Lanes!
                S_MOV S0, 255    ; S0 = Scalar blue channel intensity
                V_MOV V0, THREAD_ID ; V0 = Lane-specific thread ID
                V_MUL V1, V0, 32 ; V1 = Red gradient = thread_id * 32
                V_MOV V2, 0      ; V2 = Green intensity = 0
                V_OUT V1, V2, S0 ; Output colors! Updates 8x8 Canvas
            """.trimIndent()
        }
    }

    private fun getQuizForTopic(arch: Architecture, chapId: Int, topicId: Int, title: String): Quiz {
        val totalIdx = (chapId - 1) * 10 + topicId
        
        return when (arch) {
            Architecture.X86 -> when (totalIdx % 4) {
                0 -> Quiz(
                    question = "In x86-64 assembly, what is the size of the EAX register?",
                    options = listOf("16 bits", "32 bits", "64 bits", "128 bits"),
                    correctAnswerIndex = 1,
                    explanation = "EAX is the 32-bit lower portion of the 64-bit RAX register. AX is 16-bit, and AL is 8-bit."
                )
                1 -> Quiz(
                    question = "Which instruction is used to compare two values in x86-64?",
                    options = listOf("MOV", "ADD", "CMP", "XOR"),
                    correctAnswerIndex = 2,
                    explanation = "The CMP (Compare) instruction subtracts the second operand from the first and updates the flags (Zero Flag, Carry Flag) without modifying the registers."
                )
                2 -> Quiz(
                    question = "What register does ESP refer to in x86-64 architecture?",
                    options = listOf("Extended Accumulator Register", "Extended Base Register", "Extended Source Index", "Extended Stack Pointer"),
                    correctAnswerIndex = 3,
                    explanation = "ESP is the Stack Pointer register, tracking the top address of the current execution call stack."
                )
                else -> Quiz(
                    question = "What is the result of 'XOR EAX, EAX'?",
                    options = listOf("Multiplies EAX by 2", "Adds EAX to itself", "Clears EAX to 0", "Inverts EAX bits"),
                    correctAnswerIndex = 2,
                    explanation = "XORing any register with itself yields 0. This is a common and highly optimized opcode in x86 compiler generation to clear registers."
                )
            }
            Architecture.ARM -> when (totalIdx % 4) {
                0 -> Quiz(
                    question = "Which architectural style does ARM belong to?",
                    options = listOf("CISC (Complex Instruction Set)", "RISC (Reduced Instruction Set)", "VLIW (Very Long Instruction Word)", "MISC"),
                    correctAnswerIndex = 1,
                    explanation = "ARM is a classic RISC architecture, relying on a clean load-store pipeline where data must be loaded into registers before computation."
                )
                1 -> Quiz(
                    question = "How do you distinguish a 64-bit register from a 32-bit register in ARM64?",
                    options = listOf("X0 is 64-bit, W0 is 32-bit", "R0 is 64-bit, S0 is 32-bit", "EAX is 64-bit, AX is 32-bit", "V0 is 64-bit, T0 is 32-bit"),
                    correctAnswerIndex = 0,
                    explanation = "In ARM64, 'X' registers (like X0) represent full 64-bit width, while 'W' registers (like W0) represent the lower 32-bit width."
                )
                2 -> Quiz(
                    question = "What is the purpose of the ARM Link Register (LR)?",
                    options = listOf("Tracks the program stack", "Stores the return address of a subroutine", "Stores bit shift counts", "Performs vector arithmetic"),
                    correctAnswerIndex = 1,
                    explanation = "The Link Register (LR/X30) holds the return address when calling a subroutine (via BL), allowing fast returns without pushing to RAM stack."
                )
                else -> Quiz(
                    question = "What is a Barrel Shifter in ARM chips?",
                    options = listOf("A hardware unit that shifts bits on operands during single-cycle ALU instructions", "A memory segmentation divider", "A floating-point exponent scaler", "A stack overflow detector"),
                    correctAnswerIndex = 0,
                    explanation = "The Barrel Shifter is a hardware accelerator that allows shifting or rotating an operand as part of a standard instruction in a single CPU cycle."
                )
            }
            Architecture.OS_DEV -> when (totalIdx % 4) {
                0 -> Quiz(
                    question = "What is the memory address of the VGA text screen buffer in real-mode x86 booting?",
                    options = listOf("0x7C00", "0x0000", "0xB8000", "0xFFFF"),
                    correctAnswerIndex = 2,
                    explanation = "The BIOS/VGA text mode buffer is physically mapped at memory address 0xB8000. Writing ASCII characters there outputs text to the screen."
                )
                1 -> Quiz(
                    question = "What physical sector address does the BIOS look for to boot a computer?",
                    options = listOf("0x0000", "0x7C00", "0xB800", "0x1000"),
                    correctAnswerIndex = 1,
                    explanation = "The BIOS reads the first sector of the boot drive and loads it at physical memory address 0x7C00, then jumps there to start the bootloader."
                )
                2 -> Quiz(
                    question = "Which Control Register (CR) flag enables x86 32-bit Protected Mode?",
                    options = listOf("CR0.PE (Bit 0)", "CR3.PG (Bit 31)", "CR4.VME", "EFLAGS.IF"),
                    correctAnswerIndex = 0,
                    explanation = "Setting Bit 0 (PE - Protection Enable) of Control Register 0 (CR0) transitions the processor from 16-bit Real Mode to 32-bit Protected Mode."
                )
                else -> Quiz(
                    question = "What is the bootloader magic signature looked for by the BIOS in the last 2 bytes of the sector?",
                    options = listOf("0x0000", "0x9090", "0xAA55", "0x55AA"),
                    correctAnswerIndex = 2,
                    explanation = "The BIOS expects the boot sector (512 bytes) to end with the magic signature 0xAA55 (stored as little-endian) to confirm it is bootable."
                )
            }
            Architecture.GPU_DEV -> when (totalIdx % 4) {
                0 -> Quiz(
                    question = "What does SIMD stand for in GPU architectures?",
                    options = listOf("Single Instruction Multiple Data", "System Integrated Memory Driver", "Scalar Indexing Mode Descriptor", "Simple Interpolation Map Designer"),
                    correctAnswerIndex = 0,
                    explanation = "SIMD stands for Single Instruction Multiple Data. It represents a parallel core running the exact same opcode concurrently on different data lanes."
                )
                1 -> Quiz(
                    question = "How do GPU scalar units differ from vector units?",
                    options = listOf("Scalar units execute globally for the warp, vector units execute per-thread", "Scalar units handle math, vector units handle text", "Scalar units are 64-bit, vector units are 8-bit", "There is no difference"),
                    correctAnswerIndex = 0,
                    explanation = "In GPU architectures (like AMD GCN/RDNA), a scalar unit executes globally for the entire wavefront (e.g., constant loads or branching), whereas vector units execute operations on individual active lanes."
                )
                2 -> Quiz(
                    question = "What happens when parallel threads execute different branches in a shader?",
                    options = listOf("Thread crash", "Dynamic divergence, causing sequential execution of both branches with lane masking", "The GPU re-allocates cores in hardware", "Compilation error"),
                    correctAnswerIndex = 1,
                    explanation = "Branch divergence on GPUs causes lanes that do not take the current path to be masked off (disabled), and both paths are executed sequentially, degrading performance."
                )
                else -> Quiz(
                    question = "What is the role of an Execution Mask (EXEC register) in GPU assembly?",
                    options = listOf("Allocates hardware cache size", "Sets the pixel refresh rate", "Controls which SIMD lanes/threads are active during a vector instruction", "Encrypts vector math"),
                    correctAnswerIndex = 2,
                    explanation = "The Execution Mask (EXEC) is a bitfield (e.g., 64-bit for Nvidia warps or AMD waves) where each bit controls whether a specific SIMD lane executes the current vector opcode."
                )
            }
        }
    }
}
