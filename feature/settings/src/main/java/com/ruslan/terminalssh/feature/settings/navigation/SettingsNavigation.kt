package com.ruslan.terminalssh.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ruslan.terminalssh.feature.settings.SettingsScreen

const val SETTINGS_ROUTE = "settings"

fun NavController.navigateToSettings() {
    navigate(SETTINGS_ROUTE)
}

fun NavGraphBuilder.settingsScreen(
    onNavigateBack: () -> Unit
) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(
            onNavigateBack = onNavigateBack
        )
    }
}
