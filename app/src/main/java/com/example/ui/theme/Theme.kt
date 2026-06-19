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

private val ProfessionalPolishColorScheme = lightColorScheme(
    primary = CyberPrimary,
    onPrimary = Color.White,
    primaryContainer = CyberSecondary,
    onPrimaryContainer = Color(0xFF001D35),
    secondary = CyberSecondary,
    onSecondary = Color(0xFF001D35),
    secondaryContainer = Color(0xFFE1E2EC),
    onSecondaryContainer = Color(0xFF191C1E),
    tertiary = CyberTertiary,
    onTertiary = Color(0xFF43474E),
    background = CyberBackground,
    onBackground = CyberText,
    surface = CyberSurface,
    onSurface = CyberText,
    outline = Color(0xFFC4C6D0)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic colors by default so Aura UI glows with brand design!
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> ProfessionalPolishColorScheme // Force Professional Polish light theme for elegance!
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
