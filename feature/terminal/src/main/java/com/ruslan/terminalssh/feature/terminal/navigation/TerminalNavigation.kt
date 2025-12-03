package com.ruslan.terminalssh.feature.terminal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ruslan.terminalssh.feature.terminal.connect.ConnectScreen
import com.ruslan.terminalssh.feature.terminal.terminal.TerminalScreen

const val TERMINAL_GRAPH_ROUTE = "terminal_graph"
const val CONNECT_ROUTE = "connect"
const val TERMINAL_ROUTE = "terminal/{connectionId}"

fun NavGraphBuilder.terminalNavGraph(navController: NavController) {
    navigation(
        startDestination = CONNECT_ROUTE,
        route = TERMINAL_GRAPH_ROUTE
    ) {
        composable(CONNECT_ROUTE) {
            ConnectScreen(
                onNavigateToTerminal = { connectionId ->
                    navController.navigate("terminal/$connectionId") {
                        popUpTo(CONNECT_ROUTE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = TERMINAL_ROUTE,
            arguments = listOf(
                navArgument("connectionId") { type = NavType.LongType }
            )
        ) {
            TerminalScreen(
                onNavigateBack = {
                    navController.navigate(CONNECT_ROUTE) {
                        popUpTo(TERMINAL_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}
