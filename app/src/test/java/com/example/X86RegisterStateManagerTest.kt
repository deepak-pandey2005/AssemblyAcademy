package com.example

import com.example.simulator.X86RegisterStateManager
import com.example.simulator.X86ScenarioPreset
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class X86RegisterStateManagerTest {

    private lateinit var manager: X86RegisterStateManager

    @Before
    fun setUp() {
        manager = X86RegisterStateManager()
    }

    @Test
    fun testInitialPowerOnDefaults() {
        val state = manager.state.value
        assertEquals(0, state.eax)
        assertEquals(0, state.ebx)
        assertEquals(0, state.ecx)
        assertEquals(0, state.edx)
        assertEquals(1000, state.esp)
        assertEquals(1000, state.ebp)
        assertEquals(0, state.eip)
        // Bit 1 is reserved and always 1
        assertEquals(0x0002, state.eflags and 0x0002)
    }

    @Test
    fun testSubRegisterAliasingPropagation() {
        // Load full 32-bit register
        manager.setRegister("EAX", 0x12345678, "Load 32-bit EAX")
        val state1 = manager.state.value

        assertEquals(0x12345678, state1.eax)
        assertEquals(0x5678, state1.ax)
        assertEquals(0x56, state1.ah)
        assertEquals(0x78, state1.al)

        // Modify AL (low 8 bits) only -> should preserve AH (0x56) and upper word (0x1234)
        manager.setRegister("AL", 0x99, "Update AL")
        val state2 = manager.state.value
        assertEquals(0x12345699, state2.eax)
        assertEquals(0x5699, state2.ax)
        assertEquals(0x56, state2.ah)
        assertEquals(0x99, state2.al)

        // Modify AH (high 8 bits) only -> should preserve AL (0x99) and upper word (0x1234)
        manager.setRegister("AH", 0xAA, "Update AH")
        val state3 = manager.state.value
        assertEquals(0x1234AA99, state3.eax)
        assertEquals(0xAA99, state3.ax)
        assertEquals(0xAA, state3.ah)
        assertEquals(0x99, state3.al)

        // Modify AX (low 16 bits) -> should preserve upper word (0x1234)
        manager.setRegister("AX", 0x0042, "Update AX")
        val state4 = manager.state.value
        assertEquals(0x12340042, state4.eax)
        assertEquals(0x0042, state4.ax)
        assertEquals(0x00, state4.ah)
        assertEquals(0x42, state4.al)
    }

    @Test
    fun testBitToggleInTransistorMatrix() {
        manager.setRegister("EBX", 0, "Clear EBX")
        assertEquals(0, manager.state.value.ebx)

        // Toggle bit 0 -> should become 1
        manager.toggleRegisterBit("EBX", 0)
        assertEquals(1, manager.state.value.ebx)

        // Toggle bit 8 -> should add 256 (0x0101)
        manager.toggleRegisterBit("EBX", 8)
        assertEquals(0x0101, manager.state.value.ebx)

        // Toggle bit 0 back -> should remove 1 (0x0100)
        manager.toggleRegisterBit("EBX", 0)
        assertEquals(0x0100, manager.state.value.ebx)
    }

    @Test
    fun testEflagsConditionCalculation() {
        // XOR EAX, EAX should clear EAX and set ZF = true, CF = false
        manager.setRegister("EAX", 12345)
        manager.executeMicroInstruction("XOR EAX, EAX")
        val state = manager.state.value

        assertEquals(0, state.eax)
        assertTrue("Zero Flag should be set", state.zf)
        assertFalse("Carry Flag should be cleared on XOR", state.cf)
        assertFalse("Sign Flag should be cleared for zero", state.sf)
        assertFalse("Overflow Flag should be cleared on XOR", state.of)
    }

    @Test
    fun testArithmeticFlagsOnNegativeAndBorrow() {
        // SUB 0 - 5 -> should produce negative signed result and borrow
        manager.setRegister("EDX", 0)
        manager.executeMicroInstruction("SUB EDX, 5")
        val state = manager.state.value

        assertEquals(-5, state.edx)
        assertFalse("Zero Flag should not be set", state.zf)
        assertTrue("Sign Flag should be set for negative result", state.sf)
        assertTrue("Carry Flag should be set on borrow", state.cf)
    }

    @Test
    fun testUndoAndRedoTimeTravel() {
        assertFalse(manager.canUndo.value)
        assertFalse(manager.canRedo.value)

        manager.setRegister("ECX", 100)
        assertTrue(manager.canUndo.value)
        assertEquals(100, manager.state.value.ecx)

        manager.setRegister("ECX", 200)
        assertEquals(200, manager.state.value.ecx)

        // Undo once
        manager.undo()
        assertEquals(100, manager.state.value.ecx)
        assertTrue(manager.canRedo.value)

        // Redo once
        manager.redo()
        assertEquals(200, manager.state.value.ecx)
    }

    @Test
    fun testScenarioPresets() {
        manager.applyPreset(X86ScenarioPreset.LOOP_COUNTER)
        val state = manager.state.value

        assertEquals(10, state.ecx)
        assertEquals(0, state.eax)
        assertEquals(5, state.ebx)
    }

    @Test
    fun testOverflowScenario() {
        manager.applyPreset(X86ScenarioPreset.INTEGER_OVERFLOW_TEST)
        assertEquals(0x7FFFFFFF, manager.state.value.eax)
        assertEquals(1, manager.state.value.ebx)

        // ADD EAX, EBX -> 0x7FFFFFFF + 1 = 0x80000000 (signed overflow!)
        manager.executeMicroInstruction("ADD EAX, EBX")
        val state = manager.state.value

        assertEquals(0x80000000.toInt(), state.eax)
        assertTrue("Sign Flag should be set", state.sf)
        assertTrue("Overflow Flag should be set for signed overflow", state.of)
    }
}
