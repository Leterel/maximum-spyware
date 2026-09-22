package com.maximumspyware.parody.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B0E0C)
val Panel = Color(0xFF151A16)
val Border = Color(0xFF2A352B)
val Lime = Color(0xFFC3FC65)
val Muted = Color(0xFFA1ADA3)
val Paper = Color(0xFFF0F3E9)
val Red = Color(0xFFFF7F75)

@Composable
fun SpyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lime, onPrimary = Ink,
            secondary = Lime, onSecondary = Ink,
            background = Ink, onBackground = Paper,
            surface = Panel, onSurface = Paper,
            surfaceVariant = Border, onSurfaceVariant = Muted,
            error = Red,
        ),
        content = content,
    )
}
