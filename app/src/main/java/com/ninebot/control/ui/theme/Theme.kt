package com.ninebot.control.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF00C853)
val BrandBlue = Color(0xFF2979FF)
val BrandOrange = Color(0xFFFF6D00)
val BrandRed = Color(0xFFD50000)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    secondary = BrandBlue,
    tertiary = BrandOrange,
    background = Color(0xFF0E1013),
    surface = Color(0xFF17191D)
)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    secondary = BrandBlue,
    tertiary = BrandOrange
)

@Composable
fun NinebotControlTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
