package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = MediaHubPrimary,
    secondary = MediaHubSecondary,
    tertiary = MediaHubAccent,
    background = MediaHubBackground,
    surface = MediaHubSurface,
    surfaceVariant = MediaHubSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = MediaHubTextSecondary
  )

private val LightColorScheme = DarkColorScheme // Keep it consistently dark and premium

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark premium style by default
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Keep custom MediaHub theme colors to maintain matching visuals
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
