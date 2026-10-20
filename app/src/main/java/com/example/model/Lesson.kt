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
