package com.riftchampions.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ember = Color(0xFFE8B34C)
val Verdant = Color(0xFF7FBF8E)
val Hollow = Color(0xFF8E7CC3)
val Background = Color(0xFF14101A)
val Surface = Color(0xFF221A2B)
val OnSurface = Color(0xFFF0EAF6)
val Danger = Color(0xFFD9534F)

private val RiftColorScheme = darkColorScheme(
    primary = Ember,
    secondary = Verdant,
    tertiary = Hollow,
    background = Background,
    surface = Surface,
    onPrimary = Color(0xFF1A1206),
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = Danger,
)

@Composable
fun RiftChampionsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RiftColorScheme,
        content = content,
    )
}
