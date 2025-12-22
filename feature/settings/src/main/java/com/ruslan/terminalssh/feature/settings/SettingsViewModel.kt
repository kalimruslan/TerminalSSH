package com.ruslan.terminalssh.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruslan.terminalssh.domain.model.ColorScheme
import com.ruslan.terminalssh.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update {
                    it.copy(
                        fontSize = settings.fontSize,
                        colorScheme = settings.colorScheme
                    )
                }
            }
        }
    }

    fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetFontSize -> setFontSize(intent.size)
            is SettingsIntent.SetColorScheme -> setColorScheme(intent.scheme)
        }
    }

    private fun setFontSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setFontSize(size)
        }
    }

    private fun setColorScheme(scheme: ColorScheme) {
        viewModelScope.launch {
            settingsRepository.setColorScheme(scheme)
        }
    }
}
