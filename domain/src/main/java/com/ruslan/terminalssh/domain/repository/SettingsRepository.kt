package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.domain.model.ColorScheme
import com.ruslan.terminalssh.domain.model.TerminalSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<TerminalSettings>
    suspend fun setFontSize(size: Int)
    suspend fun setColorScheme(scheme: ColorScheme)
}
