package com.phishdb.app.sync

import com.phishdb.app.BuildConfig
import com.phishdb.app.data.api.PhishApi
import com.phishdb.app.data.api.PhishApiError
import com.phishdb.app.data.db.PhishDatabase
import com.phishdb.app.data.repo.derivedShows
import com.phishdb.app.data.repo.derivedSongs
import com.phishdb.app.data.repo.derivedVenues
import com.phishdb.app.data.repo.toEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.OffsetDateTime
import java.time.Year

class SyncManager(
    private val api: PhishApi,
    private val db: PhishDatabase,
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val mutex = Mutex()

    private fun apiKeyOrNull(): String? = BuildConfig.PHISH_API_KEY.takeIf { it.isNotBlank() }

    /** Weekly update: refresh last year and this year on top of the bundled database. */
    suspend fun syncRecent() = syncAll(startYear = Year.now().value - 1)

    suspend fun syncAll(startYear: Int = FIRST_SHOW_YEAR, endYear: Int? = null) {
        if (!mutex.tryLock()) return
        try {
            val key = apiKeyOrNull() ?: run {
                _state.value = SyncState.Error(NO_KEY_MESSAGE)
                return
            }
            val finalEnd = endYear ?: Year.now().value
            val stats = SyncStats().let { s ->
                _state.value = SyncState.Running("Fetching venues…", 0.01f)
                val venues = api.venues(key).let { env ->
                    PhishApiError.message(env)?.let { throw IllegalStateException(it) }
                    env.data
                }
                val venueEnts = venues.mapNotNull { it.toEntity() }
                db.venueDao().upsertAll(venueEnts)

                _state.value = SyncState.Running("Fetching songs…", 0.05f)
                val songs = api.songs(key).let { env ->
                    PhishApiError.message(env)?.let { throw IllegalStateException(it) }
                    env.data
                }
                val songEnts = songs.mapNotNull { it.toEntity() }
                db.songDao().upsertAll(songEnts)

                _state.value = SyncState.Running("Fetching all Phish shows…", 0.10f)
                val shows = api.shows(key).let { env ->
                    PhishApiError.message(env)?.let { throw IllegalStateException(it) }
                    env.data
                }
                val showEnts = shows.mapNotNull { it.toEntity() }
                db.showDao().upsertAll(showEnts)

                var running = s.copy(
                    venues = venueEnts.size,
                    songs = songEnts.size,
                    shows = showEnts.size,
                )

                val totalYears = (finalEnd - startYear + 1).coerceAtLeast(1)
                val yearsDone = mutableListOf<Int>()
                var entryTotal = 0
                for ((idx, year) in (startYear..finalEnd).withIndex()) {
                    val progress = 0.15f + (idx.toFloat() / totalYears) * 0.80f
                    _state.value = SyncState.Running("Fetching setlists for $year…", progress)
                    val env = api.setlistsForYear(year, key)
                    PhishApiError.message(env)?.let { throw IllegalStateException(it) }
                    val entries = env.data
                    if (entries.isNotEmpty()) {
                        db.venueDao().upsertAll(entries.derivedVenues())
                        db.songDao().upsertAll(entries.derivedSongs())
                        db.showDao().upsertAll(entries.derivedShows())
                        val setEnts = entries.mapNotNull { it.toEntity() }
                        db.setlistDao().upsertAll(setEnts)
                        entryTotal += setEnts.size
                        yearsDone += year
                    }
                    delay(THROTTLE_MS)
                }

                running = running.copy(setlistEntries = entryTotal, yearsSynced = yearsDone)
                running
            }

            db.metaDao().set(
                com.phishdb.app.data.db.SyncMetaEntity(
                    key = "last_sync",
                    value = OffsetDateTime.now().toString(),
                    updated_at = OffsetDateTime.now().toString(),
                )
            )
            _state.value = SyncState.Success(stats)
        } catch (t: Throwable) {
            _state.value = SyncState.Error(t.message ?: t::class.simpleName.orEmpty())
        } finally {
            mutex.unlock()
        }
    }

    suspend fun syncYear(year: Int) {
        if (!mutex.tryLock()) return
        try {
            val key = apiKeyOrNull() ?: run {
                _state.value = SyncState.Error(NO_KEY_MESSAGE)
                return
            }
            _state.value = SyncState.Running("Fetching setlists for $year…", 0.5f)
            val env = api.setlistsForYear(year, key)
            PhishApiError.message(env)?.let { throw IllegalStateException(it) }
            val entries = env.data
            if (entries.isNotEmpty()) {
                db.venueDao().upsertAll(entries.derivedVenues())
                db.songDao().upsertAll(entries.derivedSongs())
                db.showDao().upsertAll(entries.derivedShows())
                db.setlistDao().upsertAll(entries.mapNotNull { it.toEntity() })
            }
            db.metaDao().set(
                com.phishdb.app.data.db.SyncMetaEntity(
                    key = "year_sync_$year",
                    value = OffsetDateTime.now().toString(),
                    updated_at = OffsetDateTime.now().toString(),
                )
            )
            _state.value = SyncState.Success(SyncStats(setlistEntries = entries.size, yearsSynced = listOf(year)))
        } catch (t: Throwable) {
            _state.value = SyncState.Error(t.message ?: t::class.simpleName.orEmpty())
        } finally {
            mutex.unlock()
        }
    }

    fun reset() { _state.value = SyncState.Idle }

    companion object {
        const val FIRST_SHOW_YEAR = 1983
        private const val THROTTLE_MS = 350L
        private const val NO_KEY_MESSAGE =
            "No Phish.net API key was built into the app. Add PHISH_API_KEY to the desktop .env and rebuild."
    }
}
