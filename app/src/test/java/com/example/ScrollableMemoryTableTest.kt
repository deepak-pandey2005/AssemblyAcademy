package com.example

import com.example.simulator.X86RegisterStateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ScrollableMemoryTableTest {

    private lateinit var stateManager: X86RegisterStateManager

    @Before
    fun setUp() {
        stateManager = X86RegisterStateManager()
    }

    @Test
    fun testDefaultMemoryInitialized() {
        val snapshot = stateManager.state.value
        assertNotNull(snapshot.memory)
        assertEquals(42, snapshot.memory[100])
        assertEquals(99, snapshot.memory[104])
        assertEquals(15, snapshot.memory[108])
        assertEquals(256, snapshot.memory[112])
    }

    @Test
    fun testSetMemoryValueUpdatesStateAndModifiedAddresses() {
        val testAddr = 128
        val testVal = 0x12345678

        stateManager.setMemoryValue(testAddr, testVal, "Unit Test Memory Write")

        val updated = stateManager.state.value
        assertEquals(testVal, updated.memory[testAddr])
        assertTrue("Modified addresses should contain 128", updated.modifiedMemoryAddresses.contains(128))
        assertTrue("Cycle count should have incremented", updated.cycleCount > 0)
    }

    @Test
    fun testMemoryAddressAlignment() {
        stateManager.setMemoryValue(101, 777)
        val readBack = stateManager.getMemoryValue(100)
        assertEquals(777, readBack)
        assertEquals(777, stateManager.getMemoryValue(101))
    }

    @Test
    fun testMicroInstructionMovToMemory() {
        stateManager.executeMicroInstruction("MOV [100], 999")
        val updated = stateManager.state.value
        assertEquals(999, updated.memory[100])
    }

    @Test
    fun testMicroInstructionMovFromMemoryToRegister() {
        stateManager.setMemoryValue(104, 555)
        stateManager.executeMicroInstruction("MOV EAX, [104]")
        val updated = stateManager.state.value
        assertEquals(555, updated.eax)
    }

    @Test
    fun testPushAndPopStackMemoryInteractions() {
        val initialEsp = 1000
        stateManager.setRegister("ESP", initialEsp)
        stateManager.setRegister("EAX", 0xDEADBEEF.toInt())

        stateManager.executeMicroInstruction("PUSH EAX")
        val afterPush = stateManager.state.value
        assertEquals(996, afterPush.esp)
        assertEquals(0xDEADBEEF.toInt(), afterPush.memory[996])

        stateManager.executeMicroInstruction("POP EBX")
        val afterPop = stateManager.state.value
        assertEquals(0xDEADBEEF.toInt(), afterPop.ebx)
        assertEquals(1000, afterPop.esp)
    }

    @Test
    fun testSyncFromMapWithMemory() {
        val testRegs = mapOf("EAX" to 10, "EBX" to 20, "ESP" to 992)
        val testMem = mapOf(992 to 1234, 100 to 888)
        val modified = setOf(992)

        stateManager.syncFromMap(testRegs, emptyMap(), testMem, modified)

        val snapshot = stateManager.state.value
        assertEquals(10, snapshot.eax)
        assertEquals(20, snapshot.ebx)
        assertEquals(992, snapshot.esp)
        assertEquals(1234, snapshot.memory[992])
        assertEquals(888, snapshot.memory[100])
        assertTrue(snapshot.modifiedMemoryAddresses.contains(992))
    }

    @Test
    fun testUndoRestoresMemoryAndRegisters() {
        val origVal = stateManager.getMemoryValue(100)
        stateManager.setMemoryValue(100, 99999)
        assertEquals(99999, stateManager.getMemoryValue(100))

        stateManager.undo()
        assertEquals(origVal, stateManager.getMemoryValue(100))
    }
}
