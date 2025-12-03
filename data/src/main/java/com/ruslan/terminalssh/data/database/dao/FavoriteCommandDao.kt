package com.ruslan.terminalssh.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ruslan.terminalssh.data.database.entity.FavoriteCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteCommandDao {
    @Query("SELECT * FROM favorite_commands WHERE connectionId = :connectionId ORDER BY createdAt DESC")
    fun getCommandsForConnection(connectionId: Long): Flow<List<FavoriteCommandEntity>>

    @Insert
    suspend fun insert(command: FavoriteCommandEntity): Long

    @Delete
    suspend fun delete(command: FavoriteCommandEntity)

    @Query("DELETE FROM favorite_commands WHERE connectionId = :connectionId AND command = :command")
    suspend fun deleteByCommand(connectionId: Long, command: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_commands WHERE connectionId = :connectionId AND command = :command)")
    suspend fun exists(connectionId: Long, command: String): Boolean
}
