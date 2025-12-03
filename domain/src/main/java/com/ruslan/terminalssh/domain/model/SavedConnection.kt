package com.ruslan.terminalssh.domain.model

data class SavedConnection(
    val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val lastUsedAt: Long? = null
)
