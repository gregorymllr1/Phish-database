package com.phishdb.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        syncManager.syncRecent()
        val state = syncManager.state.value
        return if (state is SyncState.Error) Result.retry() else Result.success()
    }

    companion object {
        private const val WEEKLY_WORK = "weekly-sync"

        /** Runs every 7 days, first at the coming Sunday 12:00 AM. Safe to call on every launch. */
        fun scheduleWeekly(context: Context) {
            val now = LocalDateTime.now()
            val nextSundayMidnight = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .atStartOfDay()
            val request = PeriodicWorkRequestBuilder<SyncWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(Duration.between(now, nextSundayMidnight).toMinutes(), TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WEEKLY_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

class SyncWorkerFactory(private val syncManager: SyncManager) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        if (workerClassName == SyncWorker::class.java.name)
            SyncWorker(appContext, workerParameters, syncManager)
        else null
}
