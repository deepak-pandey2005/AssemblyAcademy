package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.LessonsScreen
import com.example.ui.screens.PlaygroundScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SiliconRegisterLabScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.X86ReactiveDashboardScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PureWhite
import com.example.ui.theme.TerminalBlack
import com.example.ui.theme.TerminalDarkGray
import com.example.viewmodel.AssemblyViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: AssemblyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentRoute by remember { mutableStateOf("syllabus") }
                var activeTutorialLesson by remember { mutableStateOf<com.example.model.Lesson?>(null) }
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DarkBackground,
                    bottomBar = {
                        NavigationBar(
                            containerColor = TerminalDarkGray,
                            tonalElevation = 8.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            // Syllabus Navigation
                            NavigationBarItem(
                                selected = currentRoute == "syllabus" || currentRoute == "study" || currentRoute == "x86_curriculum" || currentRoute == "interactive_tutorial",
                                onClick = { currentRoute = "syllabus" },
                                label = {
                                    Text(
                                        "SYLLABUS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "Syllabus"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                                    unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                                )
                            )

                            // Silicon Register Lab Navigation
                            NavigationBarItem(
                                selected = currentRoute == "silicon_lab",
                                onClick = { currentRoute = "silicon_lab" },
                                label = {
                                    Text(
                                        "SILICON LAB",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.DeveloperBoard,
                                        contentDescription = "Silicon Lab"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalBlack,
                                    selectedTextColor = NeonGreen,
                                    indicatorColor = NeonGreen,
                                    unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                                    unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                                )
                            )

                            // Playground Debugger Navigation
                            NavigationBarItem(
                                selected = currentRoute == "sandbox",
                                onClick = { currentRoute = "sandbox" },
                                label = {
                                    Text(
                                        "DEBUGGER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.BugReport,
                                        contentDescription = "Playground"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalBlack,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                                    unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                                )
                            )

                            // Profile Navigation
                            NavigationBarItem(
                                selected = currentRoute == "profile",
                                onClick = { currentRoute = "profile" },
                                label = {
                                    Text(
                                        "LIBRARY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Profile"
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TerminalBlack,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = PureWhite.copy(alpha = 0.6f),
                                    unselectedTextColor = PureWhite.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentRoute) {
                            "syllabus" -> {
                                LessonsScreen(
                                    viewModel = viewModel,
                                    onTopicSelected = { topic ->
                                        viewModel.selectTopic(topic)
                                        currentRoute = "study"
                                    },
                                    onOpenX86Curriculum = {
                                        currentRoute = "x86_curriculum"
                                    },
                                    onOpenTutorialLesson = { lesson ->
                                        activeTutorialLesson = lesson
                                        currentRoute = "interactive_tutorial"
                                    },
                                    onOpenSiliconLab = {
                                        currentRoute = "silicon_lab"
                                    },
                                    onOpenX86Dashboard = {
                                        currentRoute = "x86_dashboard"
                                    }
                                )
                            }
                            "x86_dashboard" -> {
                                X86ReactiveDashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentRoute = "syllabus" },
                                    onLaunchInDebugger = { code ->
                                        viewModel.updatePlaygroundCode(code)
                                        viewModel.resetCpu(com.example.model.Architecture.X86)
                                        currentRoute = "sandbox"
                                    }
                                )
                            }
                            "silicon_lab" -> {
                                SiliconRegisterLabScreen(
                                    viewModel = viewModel,
                                    onNavigateToDebugger = { code ->
                                        viewModel.updatePlaygroundCode(code)
                                        viewModel.resetCpu(com.example.model.Architecture.X86)
                                        currentRoute = "sandbox"
                                    }
                                )
                            }
                            "x86_curriculum" -> {
                                com.example.ui.screens.X86CurriculumComponent(
                                    viewModel = viewModel,
                                    onBack = { currentRoute = "syllabus" },
                                    onLaunchSandboxWithCode = { code ->
                                        viewModel.updatePlaygroundCode(code)
                                        viewModel.resetCpu(com.example.model.Architecture.X86)
                                        currentRoute = "sandbox"
                                    }
                                )
                            }
                            "interactive_tutorial" -> {
                                val tutorial = activeTutorialLesson ?: com.example.model.X86TutorialLessons.LESSONS.first()
                                com.example.ui.screens.InteractiveLessonTutorialLayout(
                                    lesson = tutorial,
                                    viewModel = viewModel,
                                    onBack = { currentRoute = "syllabus" },
                                    onNextLesson = { next -> activeTutorialLesson = next },
                                    onPreviousLesson = { prev -> activeTutorialLesson = prev },
                                    onOpenInSandbox = { code ->
                                        viewModel.updatePlaygroundCode(code)
                                        viewModel.resetCpu(com.example.model.Architecture.X86)
                                        currentRoute = "sandbox"
                                    }
                                )
                            }
                            "study" -> {
                                StudyScreen(
                                    viewModel = viewModel,
                                    onBack = { currentRoute = "syllabus" },
                                    onLaunchSandbox = { currentRoute = "sandbox" }
                                )
                            }
                            "sandbox" -> {
                                PlaygroundScreen(
                                    viewModel = viewModel,
                                    onOpenReactiveDashboard = { currentRoute = "x86_dashboard" }
                                )
                            }
                            "profile" -> {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    onLoadProgram = { program ->
                                        viewModel.loadUserProgram(program)
                                        currentRoute = "sandbox"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
