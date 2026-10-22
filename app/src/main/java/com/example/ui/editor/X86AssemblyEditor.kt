package com.example.ui.editor

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/**
 * Modern Jetpack Compose Text Editor component with syntax highlighting support for x86 Assembly mnemonics,
 * registers, numbers, comments, and directives.
 */
@Composable
fun X86AssemblyEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    activeLineIndex: Int? = null,
    breakpoints: Set<Int> = emptySet(),
    onToggleBreakpoint: ((Int) -> Unit)? = null,
    showLineNumbers: Boolean = true,
    showQuickToolbar: Boolean = true,
    height: Dp = 260.dp,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 22.sp,
    theme: X86SyntaxTheme = X86SyntaxHighlighter.DarkTerminalTheme
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    // Keep internal TextFieldValue in sync if external value changes (e.g. program load / reset)
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            val newSelection = textFieldValue.selection.let { sel ->
                val max = value.length
                TextRange(sel.start.coerceIn(0, max), sel.end.coerceIn(0, max))
            }
            textFieldValue = textFieldValue.copy(text = value, selection = newSelection)
        }
    }

    var showLegendDialog by remember { mutableStateOf(false) }

    fun insertTextAtCursor(snippet: String) {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val newText = buildString {
            append(currentText.substring(0, selection.min))
            append(snippet)
            append(currentText.substring(selection.max))
        }
        val newCursor = selection.min + snippet.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
        onValueChange(newText)
    }

    val visualTransformation = remember(theme) {
        X86SyntaxHighlighter.visualTransformation(theme)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("x86_assembly_editor"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalBlack),
        border = BorderStroke(1.dp, TerminalLightGray)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // --- TOP STATUS BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalDarkGray)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CyberCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "x86 INTEL ASM",
                            color = CyberCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val lineCount = value.lines().size.coerceAtLeast(1)
                    Text(
                        text = "$lineCount lines | ${value.length} chars",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Syntax Legend helper button
                    IconButton(
                        onClick = { showLegendDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_syntax_legend")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Syntax Legend",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!readOnly) {
                        // Quick Indent (Tab) button
                        IconButton(
                            onClick = { insertTextAtCursor("    ") },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_indent_tab")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardTab,
                                contentDescription = "Insert Tab Indent",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Comment Line helper button
                        IconButton(
                            onClick = { insertTextAtCursor("; ") },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("btn_insert_comment")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = "Insert Comment",
                                tint = PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = TerminalLightGray, thickness = 1.dp)

            // --- EDITOR WORKSPACE (LINE NUMBERS + TEXT FIELD) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(TerminalBlack)
            ) {
                val scrollState = rememberScrollState()

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    val linesList = value.lines()
                    val totalLines = linesList.size.coerceAtLeast(1)

                    // Line Number & Breakpoint Gutter
                    if (showLineNumbers) {
                        Column(
                            modifier = Modifier
                                .width(46.dp)
                                .background(TerminalDarkGray)
                                .padding(vertical = 10.dp)
                                .testTag("x86_line_gutter"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            for (lineIdx in 0 until totalLines) {
                                val isActiveLine = activeLineIndex == lineIdx
                                val hasBreakpoint = breakpoints.contains(lineIdx)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .clickable(enabled = onToggleBreakpoint != null) {
                                            onToggleBreakpoint?.invoke(lineIdx)
                                        }
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (hasBreakpoint) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(BrightPink)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                    } else if (isActiveLine) {
                                        Text(
                                            text = "▶",
                                            color = NeonGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }

                                    Text(
                                        text = "${lineIdx + 1}",
                                        color = when {
                                            isActiveLine -> NeonGreen
                                            hasBreakpoint -> BrightPink
                                            else -> MutedText
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isActiveLine || hasBreakpoint) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(TerminalLightGray)
                        )
                    }

                    // Main Code Input Area with active line highlight overlay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp, horizontal = 10.dp)
                    ) {
                        // Background row highlight for active line pointer
                        if (activeLineIndex != null) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                linesList.forEachIndexed { idx, _ ->
                                    val isActive = idx == activeLineIndex
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(22.dp)
                                            .background(
                                                if (isActive) CyberCyan.copy(alpha = 0.18f) else Color.Transparent
                                            )
                                    )
                                }
                            }
                        }

                        // BasicTextField with live syntax highlighting
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newVal ->
                                textFieldValue = newVal
                                onValueChange(newVal.text)
                            },
                            readOnly = readOnly,
                            textStyle = TextStyle(
                                color = theme.defaultTextColor,
                                fontSize = fontSize,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = lineHeight
                            ),
                            cursorBrush = SolidColor(CyberCyan),
                            visualTransformation = visualTransformation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("x86_code_textfield")
                        )
                    }
                }
            }

            // --- BOTTOM QUICK INPUT TOOLBAR ---
            if (showQuickToolbar && !readOnly) {
                HorizontalDivider(color = TerminalLightGray, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalDarkGray)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val quickSnippets = listOf(
                        "MOV", "ADD", "SUB", "CMP", "JMP", "JE", "JNE",
                        "XOR", "PUSH", "POP", "LEA", "RET",
                        "EAX", "EBX", "ECX", "EDX",
                        ",", "[", "]", "0x", ";"
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(quickSnippets) { snippet ->
                            val isMnemonic = X86SyntaxHighlighter.X86_MNEMONICS.contains(snippet)
                            val isRegister = X86SyntaxHighlighter.X86_REGISTERS.contains(snippet)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        val textToInsert = when (snippet) {
                                            "MOV", "ADD", "SUB", "CMP", "XOR", "LEA" -> "$snippet "
                                            "," -> ", "
                                            else -> snippet
                                        }
                                        insertTextAtCursor(textToInsert)
                                    }
                                    .testTag("quick_chip_$snippet"),
                                color = when {
                                    isMnemonic -> CyberCyan.copy(alpha = 0.15f)
                                    isRegister -> AlertOrange.copy(alpha = 0.15f)
                                    else -> TerminalBlack
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isMnemonic -> CyberCyan.copy(alpha = 0.4f)
                                        isRegister -> AlertOrange.copy(alpha = 0.4f)
                                        else -> TerminalLightGray
                                    }
                                )
                            ) {
                                Text(
                                    text = snippet,
                                    color = when {
                                        isMnemonic -> CyberCyan
                                        isRegister -> AlertOrange
                                        else -> PureWhite
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SYNTAX HIGHLIGHTING LEGEND MODAL ---
    if (showLegendDialog) {
        Dialog(onDismissRequest = { showLegendDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, TerminalLightGray, RoundedCornerShape(12.dp))
                    .testTag("dialog_syntax_legend"),
                color = DarkBackground
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "x86 Syntax Palette",
                                color = PureWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showLegendDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val legendItems = listOf(
                        Triple("Instructions / Mnemonics", "MOV, ADD, SUB, CMP, JMP, XOR, RET", theme.mnemonicColor),
                        Triple("Hardware Registers", "EAX, EBX, ECX, EDX, ESP, EBP, AX, AL", theme.registerColor),
                        Triple("Numeric Literals & Hex", "0x10, 42, 0FFh, 1010b", theme.numberColor),
                        Triple("Labels & Jump Targets", "start:, loop_top:, exit:", theme.labelColor),
                        Triple("Assembler Directives", "section, db, dw, dd, dword ptr", theme.directiveColor),
                        Triple("Comments & Notes", "; explanation of step", theme.commentColor),
                        Triple("Memory Addressing", "[EAX + EBX*4 + 0x10]", theme.bracketColor)
                    )

                    legendItems.forEach { (category, example, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = category,
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = example,
                                    color = color,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showLegendDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Got It",
                            color = TerminalBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
