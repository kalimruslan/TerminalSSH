package com.ruslan.terminalssh.domain.model

import java.util.UUID

data class TerminalOutput(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: OutputType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OutputType {
    COMMAND,
    OUTPUT,
    ERROR
}
