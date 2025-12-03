package com.ruslan.terminalssh.domain.model

data class FavoriteCommand(
    val id: Long = 0,
    val connectionId: Long,
    val command: String,
    val description: String? = null
)
