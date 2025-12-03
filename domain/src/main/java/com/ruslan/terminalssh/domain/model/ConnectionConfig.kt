package com.ruslan.terminalssh.domain.model

data class ConnectionConfig(
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String
)
