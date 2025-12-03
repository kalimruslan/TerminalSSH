package com.ruslan.terminalssh.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class ConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val createdAt: Long,
    val lastUsedAt: Long?
)
