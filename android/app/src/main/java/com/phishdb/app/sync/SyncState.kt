package com.phishdb.app.sync

sealed interface SyncState {
    data object Idle : SyncState
    data class Running(val message: String, val progress: Float?) : SyncState
    data class Error(val message: String) : SyncState
    data class Success(val stats: SyncStats) : SyncState
}

data class SyncStats(
    val venues: Int = 0,
    val songs: Int = 0,
    val shows: Int = 0,
    val setlistEntries: Int = 0,
    val yearsSynced: List<Int> = emptyList(),
)
