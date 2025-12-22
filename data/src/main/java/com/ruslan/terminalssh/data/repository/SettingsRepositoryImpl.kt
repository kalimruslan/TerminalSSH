package com.ruslan.terminalssh.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ruslan.terminalssh.domain.model.ColorScheme
import com.ruslan.terminalssh.domain.model.TerminalSettings
import com.ruslan.terminalssh.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "terminal_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    companion object {
        private val FONT_SIZE_KEY = intPreferencesKey("font_size")
        private val COLOR_SCHEME_KEY = stringPreferencesKey("color_scheme")

        private const val DEFAULT_FONT_SIZE = 14
        private val DEFAULT_COLOR_SCHEME = ColorScheme.DARK
    }

    override val settings: Flow<TerminalSettings> = context.dataStore.data.map { preferences ->
        TerminalSettings(
            fontSize = preferences[FONT_SIZE_KEY] ?: DEFAULT_FONT_SIZE,
            colorScheme = preferences[COLOR_SCHEME_KEY]?.let {
                try { ColorScheme.valueOf(it) } catch (e: Exception) { DEFAULT_COLOR_SCHEME }
            } ?: DEFAULT_COLOR_SCHEME
        )
    }

    override suspend fun setFontSize(size: Int) {
        val validSize = size.coerceIn(10, 24)
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE_KEY] = validSize
        }
    }

    override suspend fun setColorScheme(scheme: ColorScheme) {
        context.dataStore.edit { preferences ->
            preferences[COLOR_SCHEME_KEY] = scheme.name
        }
    }
}
