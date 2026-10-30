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

enum class ThemePalette(val label: String) {
    GREEN("Зелёная"),
    BLUE("Синяя"),
    PURPLE("Фиолетовая"),
    ORANGE("Оранжевая")
}

private data class PaletteColors(val primary: Color, val secondary: Color, val tertiary: Color)

private fun paletteColors(palette: ThemePalette) = when (palette) {
    ThemePalette.GREEN -> PaletteColors(Color(0xFF00C853), Color(0xFF2979FF), Color(0xFFFF9100))
    ThemePalette.BLUE -> PaletteColors(Color(0xFF448AFF), Color(0xFF00B8D4), Color(0xFFFFC400))
    ThemePalette.PURPLE -> PaletteColors(Color(0xFFB388FF), Color(0xFF7C4DFF), Color(0xFFFF80AB))
    ThemePalette.ORANGE -> PaletteColors(Color(0xFFFF9100), Color(0xFFFF5252), Color(0xFF00BFA5))
}

@Composable
fun NinebotControlTheme(
    palette: ThemePalette = ThemePalette.GREEN,
    content: @Composable () -> Unit
) {
    val p = paletteColors(palette)
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = p.primary,
            secondary = p.secondary,
            tertiary = p.tertiary,
            background = Color(0xFF0E1013),
            surface = Color(0xFF17191D),
            surfaceVariant = Color(0xFF242830),
            onBackground = Color(0xFFF4F6F8),
            onSurface = Color(0xFFF4F6F8)
        )
    } else {
        lightColorScheme(
            primary = p.primary,
            secondary = p.secondary,
            tertiary = p.tertiary,
            surfaceVariant = Color(0xFFF0F2F5)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
