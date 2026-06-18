package com.calb.qr2card.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepBlue = Color(0xFF23496B)
private val WiseGrey = Color(0xFFCEDBEA)

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = Color.White,
    secondary = WiseGrey,
    onSecondary = DeepBlue,
    surface = Color.White,
    onSurface = Color(0xFF12324F),
    background = Color(0xFFF7FAFD),
    onBackground = Color(0xFF12324F),
)

@Composable
fun QR2CardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
