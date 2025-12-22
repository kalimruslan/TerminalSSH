package com.ruslan.terminalssh.feature.connect.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ruslan.terminalssh.feature.connect.ConnectScreen

const val CONNECT_ROUTE = "connect"

fun NavController.navigateToConnect() {
    navigate(CONNECT_ROUTE) {
        popUpTo(graph.startDestinationId) { inclusive = true }
    }
}

fun NavGraphBuilder.connectScreen(
    onNavigateToTerminal: (Long) -> Unit
) {
    composable(CONNECT_ROUTE) {
        ConnectScreen(
            onNavigateToTerminal = onNavigateToTerminal
        )
    }
}
