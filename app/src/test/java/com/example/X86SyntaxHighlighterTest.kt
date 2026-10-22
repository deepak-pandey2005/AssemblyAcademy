package com.example

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.editor.X86SyntaxHighlighter
import com.example.ui.editor.X86SyntaxTheme
import org.junit.Assert.*
import org.junit.Test

class X86SyntaxHighlighterTest {

    private val theme = X86SyntaxHighlighter.DefaultTheme

    @Test
    fun testMnemonicKeywordsIdentified() {
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("MOV"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("ADD"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("SUB"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("CMP"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("JMP"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("JE"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("JNE"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("XOR"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("LEA"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("PUSH"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("POP"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("RET"))
        assertTrue(X86SyntaxHighlighter.X86_MNEMONICS.contains("INT"))
    }

    @Test
    fun testRegisterKeywordsIdentified() {
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("EAX"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("EBX"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("ECX"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("EDX"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("ESP"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("EBP"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("AX"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("AL"))
        assertTrue(X86SyntaxHighlighter.X86_REGISTERS.contains("RAX"))
    }

    @Test
    fun testDirectivesIdentified() {
        assertTrue(X86SyntaxHighlighter.X86_DIRECTIVES.contains("SECTION"))
        assertTrue(X86SyntaxHighlighter.X86_DIRECTIVES.contains("GLOBAL"))
        assertTrue(X86SyntaxHighlighter.X86_DIRECTIVES.contains("DB"))
        assertTrue(X86SyntaxHighlighter.X86_DIRECTIVES.contains("DWORD"))
        assertTrue(X86SyntaxHighlighter.X86_DIRECTIVES.contains("PTR"))
    }

    @Test
    fun testMnemonicHighlighting() {
        val code = "MOV EAX, 10"
        val annotated = X86SyntaxHighlighter.highlight(code, theme)

        assertEquals("MOV EAX, 10", annotated.text)

        // Find span for "MOV"
        val movSpan = annotated.spanStyles.find { it.start == 0 && it.end == 3 }
        assertNotNull("MOV should have a SpanStyle", movSpan)
        assertEquals(theme.mnemonicColor, movSpan!!.item.color)
        assertEquals(FontWeight.Bold, movSpan.item.fontWeight)

        // Find span for "EAX"
        val eaxSpan = annotated.spanStyles.find { it.start == 4 && it.end == 7 }
        assertNotNull("EAX should have a SpanStyle", eaxSpan)
        assertEquals(theme.registerColor, eaxSpan!!.item.color)

        // Find span for number "10"
        val numSpan = annotated.spanStyles.find { it.start == 9 && it.end == 11 }
        assertNotNull("10 should have a SpanStyle", numSpan)
        assertEquals(theme.numberColor, numSpan!!.item.color)
    }

    @Test
    fun testCommentHighlighting() {
        val code = "ADD EBX, 5 ; Increment by five"
        val annotated = X86SyntaxHighlighter.highlight(code, theme)

        val commentSpan = annotated.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
        assertNotNull("Comment span should exist", commentSpan)
        assertEquals(theme.commentColor, commentSpan!!.item.color)
        assertEquals(11, commentSpan.start)
        assertEquals(code.length, commentSpan.end)
    }

    @Test
    fun testLabelHighlighting() {
        val code = "loop_start:\n    DEC ECX\n    JNZ loop_start"
        val annotated = X86SyntaxHighlighter.highlight(code, theme)

        val labelSpan = annotated.spanStyles.find { it.start == 0 && it.end == 10 }
        assertNotNull("Label loop_start should be highlighted", labelSpan)
        assertEquals(theme.labelColor, labelSpan!!.item.color)
    }

    @Test
    fun testHexLiteralHighlighting() {
        val code = "MOV EDX, 0xFF"
        val annotated = X86SyntaxHighlighter.highlight(code, theme)

        val hexSpan = annotated.spanStyles.find { it.start == 9 && it.end == 13 }
        assertNotNull("0xFF should be styled as number", hexSpan)
        assertEquals(theme.numberColor, hexSpan!!.item.color)
    }

    @Test
    fun testMemoryBracketsHighlighting() {
        val code = "MOV EAX, [EBX + 4]"
        val annotated = X86SyntaxHighlighter.highlight(code, theme)

        val openBracketSpan = annotated.spanStyles.find { it.start == 9 && it.end == 10 }
        val closeBracketSpan = annotated.spanStyles.find { it.start == 17 && it.end == 18 }
        assertNotNull("Open bracket [ should have bracket style", openBracketSpan)
        assertNotNull("Close bracket ] should have bracket style", closeBracketSpan)
        assertEquals(theme.bracketColor, openBracketSpan!!.item.color)
        assertEquals(theme.bracketColor, closeBracketSpan!!.item.color)
    }

    @Test
    fun testVisualTransformationOffsetIdentity() {
        val transformation = X86SyntaxHighlighter.visualTransformation(theme)
        val input = androidx.compose.ui.text.AnnotatedString("MOV EAX, 42")
        val transformed = transformation.filter(input)

        assertEquals("MOV EAX, 42", transformed.text.text)
        // Verify offset mapping is Identity so cursor does not jump
        assertEquals(0, transformed.offsetMapping.originalToTransformed(0))
        assertEquals(5, transformed.offsetMapping.originalToTransformed(5))
        assertEquals(11, transformed.offsetMapping.originalToTransformed(11))
        assertEquals(0, transformed.offsetMapping.transformedToOriginal(0))
        assertEquals(5, transformed.offsetMapping.transformedToOriginal(5))
        assertEquals(11, transformed.offsetMapping.transformedToOriginal(11))
    }
}
