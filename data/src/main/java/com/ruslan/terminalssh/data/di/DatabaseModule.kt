package com.ruslan.terminalssh.data.di

import android.content.Context
import androidx.room.Room
import com.ruslan.terminalssh.data.database.AppDatabase
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "terminalssh.db"
        ).build()
    }

    @Provides
    fun provideConnectionDao(db: AppDatabase): ConnectionDao = db.connectionDao()

    @Provides
    fun provideFavoriteCommandDao(db: AppDatabase): FavoriteCommandDao = db.favoriteCommandDao()
}
