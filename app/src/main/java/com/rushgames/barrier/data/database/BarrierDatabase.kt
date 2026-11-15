package com.rushgames.barrier.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rushgames.barrier.data.dao.BlocklistDao
import com.rushgames.barrier.data.dao.SessionDao
import com.rushgames.barrier.data.dao.StatsDao
import com.rushgames.barrier.data.dao.UsageLimitDao
import com.rushgames.barrier.data.model.*

@Database(
    entities = [
        Blocklist::class,
        BlockedApp::class,
        BlockedWebsite::class,
        Session::class,
        BlockedAttempt::class,
        FocusStats::class,
        AppUsageLimit::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(StringListConverter::class, BreakSettingsConverter::class)
abstract class BarrierDatabase : RoomDatabase() {
    abstract fun blocklistDao(): BlocklistDao
    abstract fun sessionDao(): SessionDao
    abstract fun statsDao(): StatsDao
    abstract fun usageLimitDao(): UsageLimitDao

    companion object {
        const val DATABASE_NAME = "barrier.db"
    }
}
