package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.data.database.dao.FavoriteCommandDao
import com.ruslan.terminalssh.data.database.entity.FavoriteCommandEntity
import com.ruslan.terminalssh.domain.model.FavoriteCommand
import com.ruslan.terminalssh.domain.repository.FavoriteCommandRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteCommandRepositoryImpl @Inject constructor(
    private val favoriteCommandDao: FavoriteCommandDao
) : FavoriteCommandRepository {

    override fun getCommandsForConnection(connectionId: Long): Flow<List<FavoriteCommand>> {
        return favoriteCommandDao.getCommandsForConnection(connectionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addToFavorites(connectionId: Long, command: String, description: String?) {
        val entity = FavoriteCommandEntity(
            connectionId = connectionId,
            command = command,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        favoriteCommandDao.insert(entity)
    }

    override suspend fun removeFromFavorites(connectionId: Long, command: String) {
        favoriteCommandDao.deleteByCommand(connectionId, command)
    }

    override suspend fun isFavorite(connectionId: Long, command: String): Boolean {
        return favoriteCommandDao.exists(connectionId, command)
    }

    private fun FavoriteCommandEntity.toDomain() = FavoriteCommand(
        id = id,
        connectionId = connectionId,
        command = command,
        description = description
    )
}
