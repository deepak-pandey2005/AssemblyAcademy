package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
  lightColorScheme(
    primary = NeonGreen,
    secondary = CyberCyan,
    tertiary = BrightPink,
    background = DarkBackground,
    surface = TerminalDarkGray,
    onPrimary = TerminalBlack,
    onSecondary = TerminalBlack,
    onTertiary = TerminalBlack,
    onBackground = PureWhite,
    onSurface = PureWhite,
    surfaceVariant = TerminalLightGray,
    onSurfaceVariant = MutedText
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Clean Minimalism is a pristine light theme
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme
  val view = LocalView.current

  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.surface.toArgb()
      val insetsController = WindowCompat.getInsetsController(window, view)
      // Since background is light, we want dark icons (isAppearanceLightStatusBars = true)
      insetsController.isAppearanceLightStatusBars = true
      insetsController.isAppearanceLightNavigationBars = true
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
