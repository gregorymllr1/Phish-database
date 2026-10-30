package com.phishdb.app.data

import android.content.Context
import androidx.work.WorkerFactory
import com.phishdb.app.data.api.ApiClient
import com.phishdb.app.data.db.PhishDatabase
import com.phishdb.app.data.repo.PhishRepository
import com.phishdb.app.sync.SyncManager
import com.phishdb.app.sync.SyncWorkerFactory

class AppContainer(context: Context) {
    private val database: PhishDatabase = PhishDatabase.build(context.applicationContext)
    val repository: PhishRepository = PhishRepository(database)
    val syncManager: SyncManager = SyncManager(
        api = ApiClient.api,
        db = database,
    )
    val workerFactory: WorkerFactory = SyncWorkerFactory(syncManager)
}
