package com.rushgames.barrier

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rushgames.barrier.data.datastore.BarrierPreferences
import com.rushgames.barrier.data.model.StoredDataRecoveryReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BarrierApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferences: BarrierPreferences

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            StoredDataRecoveryReporter.affectedValues.collect { affected ->
                if (affected.isNotEmpty()) {
                    preferences.setStorageRecoveryNotice(affected.sorted().joinToString())
                }
            }
        }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
