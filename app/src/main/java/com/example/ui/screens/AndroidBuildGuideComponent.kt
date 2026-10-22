package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BuildGuideTab(val title: String, val icon: ImageVector) {
    AUTOMATED("Automated Script", Icons.Default.PlayArrow),
    MANUAL("Gradle & ADB", Icons.Default.Terminal),
    USB_SETUP("USB Setup", Icons.Default.Usb),
    WIRELESS("Wireless ADB", Icons.Default.Wifi),
    TROUBLESHOOTING("Fix Issues", Icons.Default.BugReport)
}

data class CommandSnippet(
    val title: String,
    val description: String,
    val command: String
)

@Composable
fun AndroidBuildGuideCard(
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenGuide() }
            .testTag("card_android_build_guide"),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.5.dp, CyberCyan)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "Android Build Guide",
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "BUILD & INSTALL ON DEVICE",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "SCRIPT READY",
                            color = NeonGreen,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Deploy APK to physical Android phone or emulator via ADB",
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Includes build_and_install.sh & complete manual instructions",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open Guide",
                tint = CyberCyan,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AndroidBuildGuideDialog(
    onDismissRequest: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(BuildGuideTab.AUTOMATED) }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var copiedCommand by remember { mutableStateOf<String?>(null) }

    fun copyToClipboard(cmd: String) {
        clipboardManager.setText(AnnotatedString(cmd))
        copiedCommand = cmd
        coroutineScope.launch {
            delay(2200)
            if (copiedCommand == cmd) {
                copiedCommand = null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, TerminalLightGray, RoundedCornerShape(16.dp))
                .testTag("dialog_build_guide"),
            color = DarkBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalDarkGray)
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Android Installation Guide",
                                    color = PureWhite,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Package: com.aistudio.assemblytutor.axpkqd",
                                    color = MutedText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.testTag("btn_close_build_guide")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = PureWhite
                            )
                        }
                    }
                }

                // Copy Notification Banner
                AnimatedVisibility(visible = copiedCommand != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonGreen.copy(alpha = 0.15f))
                            .border(1.dp, NeonGreen)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Command copied to clipboard!",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Tabs Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalDarkGray.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BuildGuideTab.values()) { tab ->
                        val isSelected = tab == selectedTab
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                selectedLeadingIconColor = CyberCyan
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) CyberCyan else TerminalLightGray
                            )
                        )
                    }
                }

                HorizontalDivider(color = TerminalLightGray, thickness = 1.dp)

                // Tab Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        BuildGuideTab.AUTOMATED -> {
                            item {
                                GuideIntroCard(
                                    title = "Automated Bash Script: build_and_install.sh",
                                    description = "A production-ready script is included in the project root to automate environment checks (Java 17+, Android SDK, ADB), build debug/release APKs, detect connected devices, install, and launch the app."
                                )
                            }
                            item {
                                CommandItem(
                                    title = "1. Make Script Executable & Run",
                                    description = "Grants execute permissions and runs full build + device install + auto launch:",
                                    command = "chmod +x build_and_install.sh && ./build_and_install.sh",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "chmod +x build_and_install.sh && ./build_and_install.sh"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "2. Quick Install & Launch Flag",
                                    description = "Directly build, install to connected ADB device, and launch:",
                                    command = "./build_and_install.sh --install --launch",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "./build_and_install.sh --install --launch"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "3. Wireless ADB Connect & Install",
                                    description = "Connects to your phone over Wi-Fi without needing a USB cable:",
                                    command = "./build_and_install.sh --wireless 192.168.1.50:5555",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "./build_and_install.sh --wireless 192.168.1.50:5555"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "4. Clean Rebuild",
                                    description = "Purges Gradle build caches before generating a clean APK:",
                                    command = "./build_and_install.sh --clean --install --launch",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "./build_and_install.sh --clean --install --launch"
                                )
                            }
                        }

                        BuildGuideTab.MANUAL -> {
                            item {
                                GuideIntroCard(
                                    title = "Direct Command-Line Build (Gradle & ADB)",
                                    description = "If you prefer running raw commands without the wrapper script, follow these standard steps in your terminal inside the project directory:"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Step 1: Compile Debug APK",
                                    description = "Builds the APK using Gradle (use gradlew or system gradle):",
                                    command = "gradle assembleDebug",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "gradle assembleDebug"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Step 2: Check Connected Device",
                                    description = "Ensure your phone is recognized with status 'device':",
                                    command = "adb devices",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb devices"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Step 3: Install APK to Device",
                                    description = "Installs with -r flag (reinstall keeping user data):",
                                    command = "adb install -r app/build/outputs/apk/debug/app-debug.apk",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb install -r app/build/outputs/apk/debug/app-debug.apk"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Step 4: Launch Assembly Academy",
                                    description = "Starts MainActivity using Android Activity Manager:",
                                    command = "adb shell am start -n com.aistudio.assemblytutor.axpkqd/com.example.MainActivity",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb shell am start -n com.aistudio.assemblytutor.axpkqd/com.example.MainActivity"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "All-In-One One-Liner",
                                    description = "Builds, reinstalls, and launches in a single shell invocation:",
                                    command = "gradle assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.aistudio.assemblytutor.axpkqd/com.example.MainActivity",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand?.startsWith("gradle assembleDebug && adb install") == true
                                )
                            }
                        }

                        BuildGuideTab.USB_SETUP -> {
                            item {
                                GuideIntroCard(
                                    title = "Setting Up Physical Android Phone for USB Debugging",
                                    description = "Follow these 4 simple steps to prepare your Android smartphone or tablet:"
                                )
                            }
                            item {
                                StepItem(
                                    number = "1",
                                    title = "Unlock Developer Options",
                                    details = "Open phone Settings -> About Phone -> Tap 'Build Number' 7 times rapidly until 'You are now a developer!' appears."
                                )
                            }
                            item {
                                StepItem(
                                    number = "2",
                                    title = "Enable USB Debugging",
                                    details = "Go back to Settings -> System -> Developer Options -> Turn ON 'USB Debugging'. (On Xiaomi/MIUI devices, also turn on 'Install via USB')."
                                )
                            }
                            item {
                                StepItem(
                                    number = "3",
                                    title = "Connect via USB & Authorize",
                                    details = "Plug the phone into your computer via USB cable. Check phone screen for 'Allow USB Debugging?' dialog. Check 'Always allow from this computer' and tap 'Allow'."
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Verify Connection",
                                    description = "Terminal output should show your device ID with status 'device' (not 'unauthorized'):",
                                    command = "adb devices",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb devices"
                                )
                            }
                        }

                        BuildGuideTab.WIRELESS -> {
                            item {
                                GuideIntroCard(
                                    title = "Wireless ADB Debugging (Android 11+)",
                                    description = "Deploy and test without any physical cable over local Wi-Fi network:"
                                )
                            }
                            item {
                                StepItem(
                                    number = "1",
                                    title = "Enable Wireless Debugging",
                                    details = "Ensure phone and computer are on the same Wi-Fi network. Go to Settings -> Developer Options -> Toggle 'Wireless Debugging' ON."
                                )
                            }
                            item {
                                StepItem(
                                    number = "2",
                                    title = "Pair Device with Code",
                                    details = "Tap 'Pair device with pairing code'. Note the 6-digit Wi-Fi code, IP address, and 5-digit port."
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Pair Command",
                                    description = "Run this with your phone's displayed IP and pairing port:",
                                    command = "adb pair 192.168.1.50:PORT_FROM_SCREEN",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb pair 192.168.1.50:PORT_FROM_SCREEN"
                                )
                            }
                            item {
                                CommandItem(
                                    title = "Connect Command",
                                    description = "Run this with the main Wireless Debugging IP and port:",
                                    command = "adb connect 192.168.1.50:MAIN_PORT",
                                    onCopy = { copyToClipboard(it) },
                                    isCopied = copiedCommand == "adb connect 192.168.1.50:MAIN_PORT"
                                )
                            }
                        }

                        BuildGuideTab.TROUBLESHOOTING -> {
                            item {
                                GuideIntroCard(
                                    title = "Common Errors & Quick Fixes",
                                    description = "Solutions for standard Android build, signature, and ADB errors."
                                )
                            }
                            item {
                                FixItem(
                                    error = "device unauthorized",
                                    cause = "The phone did not display or accept the computer's RSA authorization key.",
                                    fix = "Run 'adb kill-server && adb start-server', reconnect USB cable, unlock phone screen, and tap 'Always Allow'."
                                )
                            }
                            item {
                                FixItem(
                                    error = "INSTALL_FAILED_UPDATE_INCOMPATIBLE",
                                    cause = "A version signed with a different debug key is already installed.",
                                    fix = "Uninstall the old app first via: adb uninstall com.aistudio.assemblytutor.axpkqd"
                                )
                            }
                            item {
                                FixItem(
                                    error = "INSTALL_FAILED_USER_RESTRICTED (MIUI / HyperOS)",
                                    cause = "Xiaomi devices require enabling 'Install via USB' with a SIM card inserted.",
                                    fix = "Go to Developer Options -> enable 'Install via USB' and 'USB debugging (Security settings)'."
                                )
                            }
                            item {
                                FixItem(
                                    error = "BUILD FAILED: Java version mismatch",
                                    cause = "Gradle requires Java 17 or higher for Android Gradle Plugin 8+.",
                                    fix = "Set JAVA_HOME to JDK 17+ or install OpenJDK 17: sudo apt install openjdk-17-jdk"
                                )
                            }
                            item {
                                FixItem(
                                    error = "SDK location not found (ANDROID_HOME)",
                                    cause = "The terminal does not know where the Android SDK is located.",
                                    fix = "Run ./build_and_install.sh (auto-detects SDK) or run: echo \"sdk.dir=\$HOME/Android/Sdk\" > local.properties"
                                )
                            }
                            item {
                                FixItem(
                                    error = "debug.keystore not found for debugConfig",
                                    cause = "Git ignores binary keystore files; debug.keystore.base64 is bundled instead.",
                                    fix = "Run ./build_and_install.sh (auto-restores) or run: base64 -d debug.keystore.base64 > debug.keystore"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideIntroCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, TerminalLightGray)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = MutedText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun CommandItem(
    title: String,
    description: String,
    command: String,
    onCopy: (String) -> Unit,
    isCopied: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, if (isCopied) NeonGreen else TerminalLightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = CyberCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = MutedText,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Command Box with Copy Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(TerminalBlack)
                    .border(1.dp, TerminalLightGray, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = command,
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState)
                            .padding(end = 8.dp)
                    )

                    Button(
                        onClick = { onCopy(command) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCopied) NeonGreen else CyberCyan,
                            contentColor = TerminalBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy command",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCopied) "Copied" else "Copy",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    number: String,
    title: String,
    details: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, TerminalLightGray)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    color = CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = details,
                    color = MutedText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun FixItem(
    error: String,
    cause: String,
    fix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TerminalDarkGray),
        border = BorderStroke(1.dp, BrightPink.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = BrightPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = error,
                    color = BrightPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cause: $cause",
                color = MutedText,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fix: $fix",
                color = NeonGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
