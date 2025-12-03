package com.ruslan.terminalssh.data.di

import com.ruslan.terminalssh.data.repository.ConnectionRepositoryImpl
import com.ruslan.terminalssh.data.repository.FavoriteCommandRepositoryImpl
import com.ruslan.terminalssh.data.repository.SshRepositoryImpl
import com.ruslan.terminalssh.domain.repository.ConnectionRepository
import com.ruslan.terminalssh.domain.repository.FavoriteCommandRepository
import com.ruslan.terminalssh.domain.repository.SshRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSshRepository(impl: SshRepositoryImpl): SshRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteCommandRepository(impl: FavoriteCommandRepositoryImpl): FavoriteCommandRepository
}
