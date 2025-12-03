package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.domain.model.SavedConnection
import kotlinx.coroutines.flow.Flow

interface ConnectionRepository {
    fun getAllConnections(): Flow<List<SavedConnection>>
    suspend fun getById(id: Long): SavedConnection?
    suspend fun save(connection: SavedConnection): Long
    suspend fun delete(connection: SavedConnection)
    suspend fun updateLastUsed(id: Long)
}
