package com.ruslan.terminalssh.feature.sftp.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ruslan.terminalssh.feature.sftp.SftpScreen

const val SFTP_ROUTE = "sftp"

fun NavController.navigateToSftp() {
    navigate(SFTP_ROUTE)
}

fun NavGraphBuilder.sftpScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = SFTP_ROUTE) {
        SftpScreen(onNavigateBack = onNavigateBack)
    }
}
