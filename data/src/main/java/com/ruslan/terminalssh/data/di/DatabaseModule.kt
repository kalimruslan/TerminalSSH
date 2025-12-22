package com.ruslan.terminalssh.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruslan.terminalssh.data.database.AppDatabase
import com.ruslan.terminalssh.data.database.dao.CommandHistoryDao
import com.ruslan.terminalssh.data.database.dao.ConnectionDao
import com.ruslan.terminalssh.data.database.dao.FavoriteCommandDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS command_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    connectionId INTEGER NOT NULL,
                    command TEXT NOT NULL,
                    executedAt INTEGER NOT NULL,
                    FOREIGN KEY (connectionId) REFERENCES connections(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_command_history_connectionId ON command_history(connectionId)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "terminalssh.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideConnectionDao(db: AppDatabase): ConnectionDao = db.connectionDao()

    @Provides
    fun provideFavoriteCommandDao(db: AppDatabase): FavoriteCommandDao = db.favoriteCommandDao()

    @Provides
    fun provideCommandHistoryDao(db: AppDatabase): CommandHistoryDao = db.commandHistoryDao()
}
