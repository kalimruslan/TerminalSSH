package com.ruslan.terminalssh.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TerminalColorScheme(
    val background: Color,
    val text: Color,
    val prompt: Color,
    val error: Color,
    val success: Color,
    val command: Color
)

object TerminalColors {
    // Default dark theme (backwards compatibility)
    val Background = Color(0xFF1E1E1E)
    val Text = Color(0xFFD4D4D4)
    val Prompt = Color(0xFF4EC9B0)
    val Error = Color(0xFFF44747)
    val Success = Color(0xFF6A9955)
    val Command = Color(0xFFDCDCAA)

    val Dark = TerminalColorScheme(
        background = Color(0xFF1E1E1E),
        text = Color(0xFFD4D4D4),
        prompt = Color(0xFF4EC9B0),
        error = Color(0xFFF44747),
        success = Color(0xFF6A9955),
        command = Color(0xFFDCDCAA)
    )

    val Light = TerminalColorScheme(
        background = Color(0xFFF5F5F5),
        text = Color(0xFF1E1E1E),
        prompt = Color(0xFF007ACC),
        error = Color(0xFFD32F2F),
        success = Color(0xFF388E3C),
        command = Color(0xFF795548)
    )

    val SolarizedDark = TerminalColorScheme(
        background = Color(0xFF002B36),
        text = Color(0xFF839496),
        prompt = Color(0xFF2AA198),
        error = Color(0xFFDC322F),
        success = Color(0xFF859900),
        command = Color(0xFFB58900)
    )

    val Monokai = TerminalColorScheme(
        background = Color(0xFF272822),
        text = Color(0xFFF8F8F2),
        prompt = Color(0xFF66D9EF),
        error = Color(0xFFF92672),
        success = Color(0xFFA6E22E),
        command = Color(0xFFE6DB74)
    )
}
