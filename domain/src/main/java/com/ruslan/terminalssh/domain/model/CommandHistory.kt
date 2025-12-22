package com.ruslan.terminalssh.domain.model

data class CommandHistory(
    val id: Long = 0,
    val connectionId: Long,
    val command: String,
    val executedAt: Long = System.currentTimeMillis()
)
