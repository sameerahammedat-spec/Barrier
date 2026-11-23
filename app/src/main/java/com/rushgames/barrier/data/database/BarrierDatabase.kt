package com.rushgames.barrier.data.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE sessions ADD COLUMN appFilterMode TEXT NOT NULL DEFAULT 'NONE'"
                )
                database.execSQL(
                    "ALTER TABLE sessions ADD COLUMN blockedAppPackages TEXT NOT NULL DEFAULT 'json:[]'"
                )
                // Preserve the old implicit allowlist behaviour for existing sessions.
                database.execSQL(
                    "UPDATE sessions SET appFilterMode = 'ALLOWLIST_ONLY' " +
                        "WHERE allowedAppPackages != 'json:[]' AND allowedAppPackages != ''"
                )
            }
        }
    }
}
