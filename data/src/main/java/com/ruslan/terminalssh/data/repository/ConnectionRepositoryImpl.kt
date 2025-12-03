package com.ruslan.terminalssh.data.repository

import com.ruslan.terminalssh.data.database.dao.ConnectionDao
import com.ruslan.terminalssh.data.database.entity.ConnectionEntity
import com.ruslan.terminalssh.domain.model.SavedConnection
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionDao: ConnectionDao
) : ConnectionRepository {

    override fun getAllConnections(): Flow<List<SavedConnection>> {
        return connectionDao.getAllConnections().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): SavedConnection? {
        return connectionDao.getById(id)?.toDomain()
    }

    override suspend fun save(connection: SavedConnection): Long {
        val entity = connection.toEntity()
        return if (connection.id == 0L) {
            connectionDao.insert(entity)
        } else {
            connectionDao.update(entity)
            connection.id
        }
    }

    override suspend fun delete(connection: SavedConnection) {
        connectionDao.delete(connection.toEntity())
    }

    override suspend fun updateLastUsed(id: Long) {
        connectionDao.updateLastUsed(id, System.currentTimeMillis())
    }

    private fun ConnectionEntity.toDomain() = SavedConnection(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        lastUsedAt = lastUsedAt
    )

    private fun SavedConnection.toEntity() = ConnectionEntity(
        id = id,
        name = name,
        host = host,
        port = port,
        username = username,
        password = password,
        createdAt = System.currentTimeMillis(),
        lastUsedAt = lastUsedAt
    )
}
