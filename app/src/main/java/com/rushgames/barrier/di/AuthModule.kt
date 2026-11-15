package com.rushgames.barrier.di

import com.rushgames.barrier.data.dao.BlocklistDao
import com.rushgames.barrier.data.dao.SessionDao
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.repository.AuthRepository
import com.rushgames.barrier.data.repository.CloudSyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(prefs: BarrierPreferences): AuthRepository {
        return AuthRepository(prefs)
    }

    @Provides
    @Singleton
    fun provideCloudSyncRepository(
        blocklistDao: BlocklistDao,
        sessionDao: SessionDao
    ): CloudSyncRepository {
        return CloudSyncRepository(blocklistDao, sessionDao)
    }
}
