package com.ruslan.terminalssh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.ruslan.terminalssh.core.theme.TerminalSSHTheme
import com.ruslan.terminalssh.feature.connect.navigation.CONNECT_ROUTE
import com.ruslan.terminalssh.feature.connect.navigation.connectScreen
import com.ruslan.terminalssh.feature.settings.navigation.SETTINGS_ROUTE
import com.ruslan.terminalssh.feature.settings.navigation.settingsScreen
import com.ruslan.terminalssh.feature.sftp.navigation.SFTP_ROUTE
import com.ruslan.terminalssh.feature.sftp.navigation.sftpScreen
import com.ruslan.terminalssh.feature.terminal.navigation.terminalScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TerminalSSHTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = CONNECT_ROUTE
                    ) {
                        connectScreen(
                            onNavigateToTerminal = { connectionId ->
                                navController.navigate("terminal/$connectionId") {
                                    popUpTo(CONNECT_ROUTE) { inclusive = true }
                                }
                            }
                        )
                        terminalScreen(
                            onNavigateBack = {
                                navController.navigate(CONNECT_ROUTE) {
                                    popUpTo("terminal/{connectionId}") { inclusive = true }
                                }
                            },
                            onNavigateToSettings = {
                                navController.navigate(SETTINGS_ROUTE)
                            },
                            onNavigateToSftp = {
                                navController.navigate(SFTP_ROUTE)
                            }
                        )
                        settingsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                        sftpScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
