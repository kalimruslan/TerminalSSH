package com.ruslan.terminalssh.feature.terminal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ruslan.terminalssh.feature.terminal.terminal.TerminalScreen

const val TERMINAL_ROUTE = "terminal/{connectionId}"

fun NavController.navigateToTerminal(connectionId: Long) {
    navigate("terminal/$connectionId") {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

fun NavGraphBuilder.terminalScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSftp: () -> Unit
) {
    composable(
        route = TERMINAL_ROUTE,
        arguments = listOf(
            navArgument("connectionId") { type = NavType.LongType }
        )
    ) {
        TerminalScreen(
            onNavigateBack = onNavigateBack,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSftp = onNavigateToSftp
        )
    }
}
