package com.ruslan.terminalssh.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruslan.terminalssh.data.database.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {

    @Query("SELECT * FROM command_history WHERE connectionId = :connectionId ORDER BY executedAt DESC")
    fun getHistoryForConnection(connectionId: Long): Flow<List<CommandHistoryEntity>>

    @Query("SELECT * FROM command_history WHERE connectionId = :connectionId ORDER BY executedAt DESC LIMIT 1 OFFSET :index")
    suspend fun getCommandAt(connectionId: Long, index: Int): CommandHistoryEntity?

    @Query("SELECT COUNT(*) FROM command_history WHERE connectionId = :connectionId")
    suspend fun getHistorySize(connectionId: Long): Int

    @Insert
    suspend fun insert(entity: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history WHERE connectionId = :connectionId")
    suspend fun clearHistory(connectionId: Long)

    @Query("""
        DELETE FROM command_history
        WHERE connectionId = :connectionId
        AND id NOT IN (
            SELECT id FROM command_history
            WHERE connectionId = :connectionId
            ORDER BY executedAt DESC
            LIMIT :maxSize
        )
    """)
    suspend fun trimHistory(connectionId: Long, maxSize: Int)
}
