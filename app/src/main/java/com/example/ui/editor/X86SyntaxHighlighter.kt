package com.example.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Syntax color palette for x86 Assembly mnemonics, registers, directives, and literals.
 */
data class X86SyntaxTheme(
    val mnemonicColor: Color = Color(0xFF005AC1),       // Strong royal blue for instructions
    val registerColor: Color = Color(0xFFBF5000),       // Warm amber/orange for CPU registers
    val numberColor: Color = Color(0xFF7B1FA2),         // Rich purple for hex/dec/bin numbers
    val commentColor: Color = Color(0xFF575E71),        // Muted slate gray for comments
    val labelColor: Color = Color(0xFFBA1A1A),          // Crimson red for jump target labels
    val directiveColor: Color = Color(0xFF00796B),      // Teal for assembler directives
    val stringColor: Color = Color(0xFF2E7D32),         // Forest green for string literals
    val bracketColor: Color = Color(0xFF1565C0),        // Deep blue for memory brackets [ ]
    val defaultTextColor: Color = Color(0xFF1B1B1F)     // Dark neutral text
)

/**
 * High-performance x86 Assembly Syntax Highlighter.
 * Supports Intel-syntax x86/x86-64 mnemonics, registers, memory references, labels, and comments.
 */
object X86SyntaxHighlighter {

    val DefaultTheme = X86SyntaxTheme()

    // Dark-terminal optimized palette
    val DarkTerminalTheme = X86SyntaxTheme(
        mnemonicColor = Color(0xFF4FC3F7),       // Bright cyan
        registerColor = Color(0xFFFFB74D),       // Bright warm amber
        numberColor = Color(0xFFCE93D8),         // Bright lavender
        commentColor = Color(0xFF90A4AE),        // Slate gray
        labelColor = Color(0xFFFF8A80),          // Soft coral red
        directiveColor = Color(0xFF80CBC4),      // Soft mint teal
        stringColor = Color(0xFFA5D6A7),         // Soft spring green
        bracketColor = Color(0xFF81D4FA),        // Sky blue
        defaultTextColor = Color(0xFFECEFF1)     // High-contrast off-white
    )

    // Complete set of x86-32 and x86-64 mnemonics
    val X86_MNEMONICS: Set<String> = setOf(
        // Data Movement
        "MOV", "DATA", "PUSH", "POP", "XCHG", "LEA", "MOVZX", "MOVSX",
        "CBW", "CWDE", "CDQ", "CWD", "CQO", "BSWAP", "CMOVZ", "CMOVNZ",
        "CMOVE", "CMOVNE", "CMOVG", "CMOVGE", "CMOVL", "CMOVLE", "CMOVA", "CMOVB",

        // Arithmetic
        "ADD", "SUB", "INC", "DEC", "MUL", "IMUL", "DIV", "IDIV",
        "NEG", "ADC", "SBB", "DAA", "DAS", "AAA", "AAS", "AAM", "AAD",

        // Logic & Bitwise
        "AND", "OR", "XOR", "NOT", "TEST", "BT", "BTS", "BTR", "BTC",
        "SHL", "SHR", "SAL", "SAR", "ROL", "ROR", "RCL", "RCR", "SHLD", "SHRD",

        // Control Flow & Branching
        "JMP", "JE", "JZ", "JNE", "JNZ", "JG", "JNLE", "JGE", "JNL",
        "JL", "JNGE", "JLE", "JNG", "JA", "JNBE", "JAE", "JNB",
        "JB", "JNAE", "JBE", "JNA", "JC", "JNC", "JO", "JNO",
        "JS", "JNS", "JP", "JPE", "JNP", "JPO", "JCXZ", "JECXZ", "JRCXZ",
        "LOOP", "LOOPE", "LOOPZ", "LOOPNE", "LOOPNZ",
        "CALL", "RET", "RETN", "RETF", "INT", "INTO", "IRET", "IRETD",

        // Compare
        "CMP", "TEST",

        // String Operations
        "MOVS", "MOVSB", "MOVSW", "MOVSD", "MOVSQ",
        "CMPS", "CMPSB", "CMPSW", "CMPSD", "CMPSQ",
        "SCAS", "SCASB", "SCASW", "SCASD", "SCASQ",
        "LODS", "LODSB", "LODSW", "LODSD", "LODSQ",
        "STOS", "STOSB", "STOSW", "STOSD", "STOSQ",
        "REP", "REPE", "REPZ", "REPNE", "REPNZ",

        // Flags & Processor Control
        "CLC", "STC", "CMC", "CLD", "STD", "CLI", "STI",
        "PUSHF", "POPF", "PUSHFD", "POPFD", "PUSHFQ", "POPFQ",
        "LAHF", "SAHF", "NOP", "HLT", "WAIT", "LOCK", "CPUID", "RDTSC"
    )

    // Complete set of x86 registers (32-bit, 16-bit, 8-bit, segments, control)
    val X86_REGISTERS: Set<String> = setOf(
        // 32-bit general purpose
        "EAX", "EBX", "ECX", "EDX", "ESI", "EDI", "EBP", "ESP", "EIP",
        // 16-bit general purpose
        "AX", "BX", "CX", "DX", "SI", "DI", "BP", "SP", "IP",
        // 8-bit registers (low/high)
        "AL", "AH", "BL", "BH", "CL", "CH", "DL", "DH",
        // 64-bit extension registers (commonly encountered in x86-64)
        "RAX", "RBX", "RCX", "RDX", "RSI", "RDI", "RBP", "RSP", "RIP",
        "R8", "R9", "R10", "R11", "R12", "R13", "R14", "R15",
        "R8D", "R9D", "R10D", "R11D", "R12D", "R13D", "R14D", "R15D",
        "R8W", "R9W", "R10W", "R11W", "R12W", "R13W", "R14W", "R15W",
        "R8B", "R9B", "R10B", "R11B", "R12B", "R13B", "R14B", "R15B",
        // Segment registers
        "CS", "DS", "SS", "ES", "FS", "GS",
        // Control & debug registers
        "CR0", "CR2", "CR3", "CR4", "DR0", "DR1", "DR2", "DR3", "DR6", "DR7"
    )

    // Directives and size specifiers
    val X86_DIRECTIVES: Set<String> = setOf(
        "SECTION", "SEGMENT", "GLOBAL", "GLOBL", "EXTERN", "BITS", "ORG", "EQU", "TIMES",
        "DB", "DW", "DD", "DQ", "DT", "RESB", "RESW", "RESD", "RESQ", "REST",
        "BYTE", "WORD", "DWORD", "QWORD", "TWORD", "PTR", "OFFSET", "NEAR", "FAR",
        "SHORT", "ALIGN", "END", ".DATA", ".TEXT", ".BSS", ".RODATA"
    )

    // Regex patterns
    private val HEX_PATTERN = Regex("^(0x[0-9a-fA-F]+|[0-9][0-9a-fA-F]*[hH])$")
    private val BINARY_PATTERN = Regex("^[01]+[bB]$")
    private val DECIMAL_PATTERN = Regex("^[-+]?[0-9]+$")
    private val LABEL_DECL_PATTERN = Regex("^([a-zA-Z_.$][a-zA-Z0-9_.$]*):")
    private val TOKEN_DELIMITERS = Regex("([ \t\r\n,:\\[\\]+*()\\-~;])")

    /**
     * Highlights an assembly source code string into an AnnotatedString with custom syntax styles.
     */
    fun highlight(
        text: String,
        theme: X86SyntaxTheme = DefaultTheme
    ): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            if (text.isEmpty()) return@buildAnnotatedString

            var lineStartIndex = 0
            val lines = text.split("\n")

            for (line in lines) {
                highlightLine(
                    builder = this,
                    line = line,
                    baseOffset = lineStartIndex,
                    theme = theme
                )
                lineStartIndex += line.length + 1 // +1 for the newline character
            }
        }
    }

    private fun highlightLine(
        builder: AnnotatedString.Builder,
        line: String,
        baseOffset: Int,
        theme: X86SyntaxTheme
    ) {
        if (line.isEmpty()) return

        // 1. Identify comment index (`;`, `//`, or `#`)
        var commentStart = -1
        var inQuote = false
        var quoteChar = ' '

        for (i in line.indices) {
            val c = line[i]
            if (inQuote) {
                if (c == quoteChar && (i == 0 || line[i - 1] != '\\')) {
                    inQuote = false
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuote = true
                    quoteChar = c
                } else if (c == ';' || c == '#' || (c == '/' && i + 1 < line.length && line[i + 1] == '/')) {
                    commentStart = i
                    break
                }
            }
        }

        // Apply comment styling
        val codePartEnd = if (commentStart != -1) commentStart else line.length
        if (commentStart != -1) {
            builder.addStyle(
                SpanStyle(
                    color = theme.commentColor,
                    fontStyle = FontStyle.Italic
                ),
                start = baseOffset + commentStart,
                end = baseOffset + line.length
            )
        }

        val codePart = line.substring(0, codePartEnd)
        if (codePart.isEmpty()) return

        // 2. Check for label declaration at start of line (e.g. "loop_start:")
        val trimmedLeading = codePart.trimStart()
        val leadingSpaces = codePart.length - trimmedLeading.length
        val labelMatch = LABEL_DECL_PATTERN.find(trimmedLeading)
        var parseStart = 0

        if (labelMatch != null && labelMatch.range.first == 0) {
            val labelName = labelMatch.groupValues[1]
            val labelStart = leadingSpaces
            val labelEnd = labelStart + labelName.length
            builder.addStyle(
                SpanStyle(
                    color = theme.labelColor,
                    fontWeight = FontWeight.Bold
                ),
                start = baseOffset + labelStart,
                end = baseOffset + labelEnd
            )
            parseStart = leadingSpaces + labelMatch.value.length
        }

        // 3. Tokenize remaining line part
        var currentToken = StringBuilder()
        var tokenStartPos = -1
        var inString = false
        var currentStringQuote = ' '
        var stringStartPos = -1

        var idx = parseStart
        while (idx < codePart.length) {
            val c = codePart[idx]

            // Handle string literal boundaries
            if (inString) {
                if (c == currentStringQuote && codePart.getOrNull(idx - 1) != '\\') {
                    inString = false
                    builder.addStyle(
                        SpanStyle(color = theme.stringColor),
                        start = baseOffset + stringStartPos,
                        end = baseOffset + idx + 1
                    )
                }
                idx++
                continue
            } else if (c == '"' || c == '\'') {
                // Flush pending token before string
                if (currentToken.isNotEmpty()) {
                    flushToken(builder, currentToken.toString(), baseOffset + tokenStartPos, theme)
                    currentToken.clear()
                    tokenStartPos = -1
                }
                inString = true
                currentStringQuote = c
                stringStartPos = idx
                idx++
                continue
            }

            // Memory reference brackets [ ]
            if (c == '[' || c == ']') {
                if (currentToken.isNotEmpty()) {
                    flushToken(builder, currentToken.toString(), baseOffset + tokenStartPos, theme)
                    currentToken.clear()
                    tokenStartPos = -1
                }
                builder.addStyle(
                    SpanStyle(
                        color = theme.bracketColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = baseOffset + idx,
                    end = baseOffset + idx + 1
                )
                idx++
                continue
            }

            // Token boundary check
            if (c == ' ' || c == '\t' || c == ',' || c == ':' || c == '+' || c == '-' || c == '*' || c == '(' || c == ')') {
                if (currentToken.isNotEmpty()) {
                    flushToken(builder, currentToken.toString(), baseOffset + tokenStartPos, theme)
                    currentToken.clear()
                    tokenStartPos = -1
                }
                idx++
                continue
            }

            // Accumulate token character
            if (tokenStartPos == -1) {
                tokenStartPos = idx
            }
            currentToken.append(c)
            idx++
        }

        // Flush trailing token if any
        if (currentToken.isNotEmpty()) {
            flushToken(builder, currentToken.toString(), baseOffset + tokenStartPos, theme)
        }
        // Unclosed string styling fallback
        if (inString && stringStartPos != -1) {
            builder.addStyle(
                SpanStyle(color = theme.stringColor),
                start = baseOffset + stringStartPos,
                end = baseOffset + codePart.length
            )
        }
    }

    private fun flushToken(
        builder: AnnotatedString.Builder,
        token: String,
        tokenStart: Int,
        theme: X86SyntaxTheme
    ) {
        val upper = token.uppercase()
        val tokenEnd = tokenStart + token.length

        when {
            // Mnemonic instruction check
            X86_MNEMONICS.contains(upper) -> {
                builder.addStyle(
                    SpanStyle(
                        color = theme.mnemonicColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = tokenStart,
                    end = tokenEnd
                )
            }

            // Register check
            X86_REGISTERS.contains(upper) -> {
                builder.addStyle(
                    SpanStyle(
                        color = theme.registerColor,
                        fontWeight = FontWeight.Bold
                    ),
                    start = tokenStart,
                    end = tokenEnd
                )
            }

            // Assembler directives
            X86_DIRECTIVES.contains(upper) -> {
                builder.addStyle(
                    SpanStyle(
                        color = theme.directiveColor,
                        fontWeight = FontWeight.SemiBold
                    ),
                    start = tokenStart,
                    end = tokenEnd
                )
            }

            // Numeric literals (Hex, Binary, Decimal)
            HEX_PATTERN.matches(token) || BINARY_PATTERN.matches(token) || DECIMAL_PATTERN.matches(token) -> {
                builder.addStyle(
                    SpanStyle(
                        color = theme.numberColor,
                        fontWeight = FontWeight.Medium
                    ),
                    start = tokenStart,
                    end = tokenEnd
                )
            }
        }
    }

    /**
     * Compose VisualTransformation providing zero-latency live syntax highlighting without offset drift.
     */
    fun visualTransformation(theme: X86SyntaxTheme = DefaultTheme): VisualTransformation {
        return VisualTransformation { originalText ->
            val highlighted = highlight(originalText.text, theme)
            TransformedText(
                text = highlighted,
                offsetMapping = OffsetMapping.Identity
            )
        }
    }
}
