package com.ruslan.terminalssh.domain.model

data class TerminalSettings(
    val fontSize: Int = 14,
    val colorScheme: ColorScheme = ColorScheme.DARK
)

enum class ColorScheme {
    DARK,
    LIGHT,
    SOLARIZED_DARK,
    MONOKAI
}
