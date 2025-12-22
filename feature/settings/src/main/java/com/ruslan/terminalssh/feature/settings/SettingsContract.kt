package com.ruslan.terminalssh.feature.settings

import com.ruslan.terminalssh.domain.model.ColorScheme

data class SettingsState(
    val fontSize: Int = 14,
    val colorScheme: ColorScheme = ColorScheme.DARK
)

sealed class SettingsIntent {
    data class SetFontSize(val size: Int) : SettingsIntent()
    data class SetColorScheme(val scheme: ColorScheme) : SettingsIntent()
}

sealed class SettingsEffect {
    data object NavigateBack : SettingsEffect()
}
