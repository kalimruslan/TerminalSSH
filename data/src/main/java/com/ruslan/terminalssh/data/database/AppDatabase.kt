package com.ruslan.terminalssh.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ruslan.terminalssh.data.database.dao.ConnectionDao
import com.ruslan.terminalssh.data.database.dao.FavoriteCommandDao
import com.ruslan.terminalssh.data.database.entity.ConnectionEntity
import com.ruslan.terminalssh.data.database.entity.FavoriteCommandEntity

@Database(
    entities = [ConnectionEntity::class, FavoriteCommandEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun favoriteCommandDao(): FavoriteCommandDao
}
