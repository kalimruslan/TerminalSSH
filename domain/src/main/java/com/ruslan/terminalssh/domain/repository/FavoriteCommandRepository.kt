package com.ruslan.terminalssh.domain.repository

import com.ruslan.terminalssh.domain.model.FavoriteCommand
import kotlinx.coroutines.flow.Flow

interface FavoriteCommandRepository {
    fun getCommandsForConnection(connectionId: Long): Flow<List<FavoriteCommand>>
    suspend fun addToFavorites(connectionId: Long, command: String, description: String? = null)
    suspend fun removeFromFavorites(connectionId: Long, command: String)
    suspend fun isFavorite(connectionId: Long, command: String): Boolean
}
