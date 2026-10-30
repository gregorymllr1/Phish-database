package com.phishdb.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.phishdb.app.data.AppContainer
import com.phishdb.app.sync.SyncWorker

class PhishApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WorkManager.initialize(this, workManagerConfiguration)
        SyncWorker.scheduleWeekly(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(container.workerFactory)
            .build()
}
