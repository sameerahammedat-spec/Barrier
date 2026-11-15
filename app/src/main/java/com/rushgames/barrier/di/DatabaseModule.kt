package com.rushgames.barrier.di

import android.content.Context
import androidx.room.Room
import com.rushgames.barrier.data.dao.BlocklistDao
import com.rushgames.barrier.data.dao.SessionDao
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.dao.UsageLimitDao
import com.rushgames.barrier.data.database.BarrierDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): BarrierDatabase =
        Room.databaseBuilder(context, BarrierDatabase::class.java, BarrierDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideBlocklistDao(db: BarrierDatabase): BlocklistDao = db.blocklistDao()

    @Provides
    fun provideSessionDao(db: BarrierDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideStatsDao(db: BarrierDatabase): StatsDao = db.statsDao()

    @Provides
    fun provideUsageLimitDao(db: BarrierDatabase): UsageLimitDao = db.usageLimitDao()
}
