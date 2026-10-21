package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.CompletedLesson
import com.example.database.UserProgram
import com.example.database.UserProgressRepository
import com.example.database.UserStats
import com.example.model.Architecture
import com.example.model.Syllabus
import com.example.model.Topic
import com.example.network.GeminiService
import com.example.simulator.AssemblySimulator
import com.example.simulator.CpuState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssemblyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserProgressRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = UserProgressRepository(database.userProgressDao())
        
        // Initialize UserStats if empty in database
        viewModelScope.launch {
            val stats = repository.userStats.firstOrNull()
            if (stats == null) {
                repository.updateStats(UserStats())
            }
        }
    }

    // --- database flows ---
    val completedLessons: StateFlow<List<CompletedLesson>> = repository.completedLessons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPrograms: StateFlow<List<UserProgram>> = repository.userPrograms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserStats?> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- UI State managers ---
    private val _selectedArchitecture = MutableStateFlow(Architecture.X86)
    val selectedArchitecture: StateFlow<Architecture> = _selectedArchitecture.asStateFlow()

    private val _selectedTopic = MutableStateFlow<Topic?>(null)
    val selectedTopic: StateFlow<Topic?> = _selectedTopic.asStateFlow()

    // --- Study Screen States ---
    private val _selectedQuizOption = MutableStateFlow<Int?>(null)
    val selectedQuizOption: StateFlow<Int?> = _selectedQuizOption.asStateFlow()

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted.asStateFlow()

    private val _quizIsCorrect = MutableStateFlow(false)
    val quizIsCorrect: StateFlow<Boolean> = _quizIsCorrect.asStateFlow()

    // --- Playground & Debugger States ---
    private val _playgroundCode = MutableStateFlow("")
    val playgroundCode: StateFlow<String> = _playgroundCode.asStateFlow()

    private val _cpuState = MutableStateFlow(AssemblySimulator.createInitialState(Architecture.X86))
    val cpuState: StateFlow<CpuState> = _cpuState.asStateFlow()

    private val cpuHistory = mutableListOf<CpuState>()

    // Real-time register monitoring tracking
    data class RegisterChange(
        val regName: String,
        val oldValue: Int,
        val newValue: Int,
        val stepIndex: Int,
        val instruction: String = ""
    )

    private val _modifiedRegisters = MutableStateFlow<Set<String>>(emptySet())
    val modifiedRegisters: StateFlow<Set<String>> = _modifiedRegisters.asStateFlow()

    private val _previousRegisters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val previousRegisters: StateFlow<Map<String, Int>> = _previousRegisters.asStateFlow()

    private val _registerHistory = MutableStateFlow<List<RegisterChange>>(emptyList())
    val registerHistory: StateFlow<List<RegisterChange>> = _registerHistory.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var playJob: Job? = null

    private val _breakpoints = MutableStateFlow<Set<Int>>(emptySet())
    val breakpoints: StateFlow<Set<Int>> = _breakpoints.asStateFlow()

    // --- AI Tutor States ---
    private val _aiTutorLoading = MutableStateFlow(false)
    val aiTutorLoading: StateFlow<Boolean> = _aiTutorLoading.asStateFlow()

    private val _aiTutorResponse = MutableStateFlow<String?>(null)
    val aiTutorResponse: StateFlow<String?> = _aiTutorResponse.asStateFlow()

    // --- Logic functions ---

    fun selectArchitecture(arch: Architecture) {
        _selectedArchitecture.value = arch
        _selectedTopic.value = null
        resetCpu(arch)
        
        // Load default architecture template
        val defaultTemplate = Syllabus.getChaptersFor(arch).firstOrNull()?.topics?.firstOrNull()?.codeTemplate ?: ""
        _playgroundCode.value = defaultTemplate
    }

    fun selectTopic(topic: Topic) {
        _selectedTopic.value = topic
        _selectedQuizOption.value = null
        _quizSubmitted.value = false
        _quizIsCorrect.value = false
        
        // If topic has code, pre-load it into sandbox
        if (topic.codeTemplate.isNotEmpty()) {
            _playgroundCode.value = topic.codeTemplate
            resetCpu(_selectedArchitecture.value)
        }
    }

    fun updatePlaygroundCode(newCode: String) {
        _playgroundCode.value = newCode
    }

    // --- SIMULATOR CONTROLS ---

    fun resetCpu(arch: Architecture = _selectedArchitecture.value) {
        stopPlayback()
        cpuHistory.clear()
        _previousRegisters.value = emptyMap()
        _modifiedRegisters.value = emptySet()
        _registerHistory.value = emptyList()
        _cpuState.value = AssemblySimulator.createInitialState(arch)
    }

    fun toggleBreakpoint(lineIndex: Int) {
        val current = _breakpoints.value
        _breakpoints.value = if (current.contains(lineIndex)) {
            current - lineIndex
        } else {
            current + lineIndex
        }
    }

    fun clearBreakpoints() {
        _breakpoints.value = emptySet()
    }

    fun clearRegisterHistory() {
        _registerHistory.value = emptyList()
    }

    fun updateRegisterValue(regName: String, value: Int) {
        val currentRegs = _cpuState.value.registers.toMutableMap()
        val oldVal = currentRegs[regName.uppercase()] ?: 0
        currentRegs[regName.uppercase()] = value
        
        _previousRegisters.value = _cpuState.value.registers
        _modifiedRegisters.value = setOf(regName.uppercase())
        _registerHistory.value = (_registerHistory.value + RegisterChange(
            regName = regName.uppercase(),
            oldValue = oldVal,
            newValue = value,
            stepIndex = _cpuState.value.ip + 1,
            instruction = "Manual Edit: $regName = $value"
        )).takeLast(40)

        _cpuState.value = _cpuState.value.copy(
            registers = currentRegs,
            explanation = "Manual Override: Register $regName updated to $value."
        )
    }

    fun updateMemoryValue(address: Int, value: Int) {
        val currentMem = _cpuState.value.memory.toMutableMap()
        currentMem[address] = value
        _cpuState.value = _cpuState.value.copy(
            memory = currentMem,
            explanation = "Manual Override: Memory address $address updated to $value."
        )
    }

    fun stepForward() {
        val lines = _playgroundCode.value.lines()
        val currentState = _cpuState.value
        
        // Add to history
        cpuHistory.add(currentState)
        if (cpuHistory.size > 50) {
            cpuHistory.removeAt(0) // Cap history buffer
        }

        val nextState = AssemblySimulator.executeStep(currentState, lines)
        
        // Real-time register change detection
        val changedRegs = mutableSetOf<String>()
        val currentLineCode = lines.getOrNull(currentState.ip)?.trim() ?: ""
        val newChanges = mutableListOf<RegisterChange>()

        nextState.registers.forEach { (reg, newVal) ->
            val oldVal = currentState.registers[reg] ?: 0
            if (oldVal != newVal) {
                changedRegs.add(reg)
                newChanges.add(
                    RegisterChange(
                        regName = reg,
                        oldValue = oldVal,
                        newValue = newVal,
                        stepIndex = currentState.ip + 1,
                        instruction = currentLineCode
                    )
                )
            }
        }

        _previousRegisters.value = currentState.registers
        _modifiedRegisters.value = changedRegs
        if (newChanges.isNotEmpty()) {
            _registerHistory.value = (_registerHistory.value + newChanges).takeLast(40)
        }

        _cpuState.value = nextState

        // Record execution count in stats
        viewModelScope.launch {
            repository.recordExecution(1)
        }
    }

    fun stepBackward() {
        if (cpuHistory.isNotEmpty()) {
            val prevState = cpuHistory.removeAt(cpuHistory.size - 1)
            val changedRegs = mutableSetOf<String>()
            _cpuState.value.registers.forEach { (reg, currentVal) ->
                if ((prevState.registers[reg] ?: 0) != currentVal) {
                    changedRegs.add(reg)
                }
            }
            _previousRegisters.value = _cpuState.value.registers
            _modifiedRegisters.value = changedRegs
            _cpuState.value = prevState
        }
    }

    fun togglePlay() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        playJob = viewModelScope.launch {
            val lines = _playgroundCode.value.lines()
            var firstStep = true
            while (_isPlaying.value) {
                val currentState = _cpuState.value
                if (!firstStep && _breakpoints.value.contains(currentState.ip)) {
                    _cpuState.value = currentState.copy(
                        explanation = "⏸ Paused at Breakpoint on Line ${currentState.ip + 1}."
                    )
                    stopPlayback()
                    break
                }
                firstStep = false

                if (currentState.ip < 0 || currentState.ip >= lines.size) {
                    stopPlayback()
                    break
                }
                stepForward()
                delay(1200) // 1.2 seconds execution interval per line
            }
        }
    }

    private fun stopPlayback() {
        _isPlaying.value = false
        playJob?.cancel()
        playJob = null
    }

    // --- STUDY & QUIZ INTERACTION ---

    fun selectQuizOption(optionIdx: Int) {
        if (!_quizSubmitted.value) {
            _selectedQuizOption.value = optionIdx
        }
    }

    fun submitQuiz() {
        val topic = _selectedTopic.value ?: return
        val quiz = topic.quiz ?: return
        val selected = _selectedQuizOption.value ?: return

        _quizSubmitted.value = true
        val correct = selected == quiz.correctAnswerIndex
        _quizIsCorrect.value = correct

        if (correct) {
            viewModelScope.launch {
                // Earn 100 points
                repository.completeLesson(topic.id, 100)
            }
        }
    }

    fun recordCurriculumCompletion(topicId: String, points: Int = 100) {
        viewModelScope.launch {
            repository.completeLesson(topicId, points)
        }
    }

    // --- AI TUTOR API OPERATIONS ---

    fun askAiToExplain() {
        val code = _playgroundCode.value
        val arch = _selectedArchitecture.value
        if (code.isBlank()) return

        _aiTutorLoading.value = true
        _aiTutorResponse.value = null

        viewModelScope.launch {
            val result = GeminiService.explainCode(code, arch)
            _aiTutorResponse.value = result
            _aiTutorLoading.value = false
        }
    }

    fun clearAiResponse() {
        _aiTutorResponse.value = null
    }

    // --- CUSTOM USER FILE OPERATIONS ---

    fun saveProgram(title: String) {
        val code = _playgroundCode.value
        val arch = _selectedArchitecture.value.codeName
        if (code.isBlank() || title.isBlank()) return

        viewModelScope.launch {
            repository.saveProgram(
                UserProgram(
                    title = title,
                    code = code,
                    architecture = arch
                )
            )
        }
    }

    fun loadUserProgram(program: UserProgram) {
        val matchedArch = Architecture.values().find { it.codeName == program.architecture } ?: Architecture.X86
        _selectedArchitecture.value = matchedArch
        _playgroundCode.value = program.code
        resetCpu(matchedArch)
    }

    fun deleteProgram(id: Int) {
        viewModelScope.launch {
            repository.deleteProgram(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
