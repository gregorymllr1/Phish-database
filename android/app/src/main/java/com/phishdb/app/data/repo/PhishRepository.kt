package com.phishdb.app.data.repo

import androidx.sqlite.db.SimpleSQLiteQuery
import com.phishdb.app.data.db.PhishDatabase
import com.phishdb.app.data.db.SetlistEntryEntity
import com.phishdb.app.data.db.ShowEntity
import com.phishdb.app.data.db.SongEntity
import com.phishdb.app.data.db.SyncMetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.OffsetDateTime

data class DbStats(
    val shows: Int,
    val songs: Int,
    val performances: Int,
    val venues: Int,
    val firstDate: String?,
    val lastDate: String?,
    val lastSync: String?,
)

data class ShowDetail(
    val show: ShowEntity,
    val sets: List<SetBlock>,
)

data class SetBlock(
    val key: String,
    val label: String,
    val entries: List<SetlistEntryEntity>,
)

class PhishRepository(private val db: PhishDatabase) {

    fun statsFlow(): Flow<DbStats> {
        return combine(
            db.showDao().count(),
            db.songDao().count(),
            db.setlistDao().count(),
            db.venueDao().count(),
            db.metaDao().observe("last_sync"),
        ) { shows, songs, perfs, venues, lastSync ->
            DbStats(
                shows = shows,
                songs = songs,
                performances = perfs,
                venues = venues,
                firstDate = db.showDao().firstDate(),
                lastDate = db.showDao().lastDate(),
                lastSync = lastSync,
            )
        }
    }

    suspend fun markSyncedNow() {
        db.metaDao().set(
            SyncMetaEntity(
                key = "last_sync",
                value = OffsetDateTime.now().toString(),
                updated_at = OffsetDateTime.now().toString(),
            )
        )
    }

    suspend fun getShowByDate(date: String): ShowDetail? {
        val show = db.showDao().getByDate(date) ?: return null
        val entries = db.setlistDao().byShowDate(date)
        val grouped = LinkedHashMap<String, MutableList<SetlistEntryEntity>>()
        for (e in entries) grouped.getOrPut(e.set_name) { mutableListOf() }.add(e)
        val sets = grouped.entries
            .sortedBy { SetLabels.order[it.key] ?: 99 }
            .map { (key, items) -> SetBlock(key, SetLabels.label(key), items) }
        return ShowDetail(show, sets)
    }

    suspend fun songHistory(name: String, limit: Int = 200): List<SetlistEntryEntity> {
        val filters = SearchFilters(song = name, limit = limit)
        return performances(filters)
    }

    suspend fun songByName(name: String): SongEntity? = db.songDao().getByName(name)

    suspend fun searchAll(filters: SearchFilters): List<SearchResult> {
        if (!filters.hasAny()) return emptyList()
        val results = mutableListOf<SearchResult>()
        val type = filters.resultType

        if (type == null || type == ResultType.SONG_PERFORMANCE) {
            performances(filters).forEach { results += it.toSearchResult() }
        }
        if ((type == null || type == ResultType.SHOW) && filters.song.isNullOrBlank()) {
            shows(filters).forEach { results += it.toSearchResult() }
        }
        if ((type == null || type == ResultType.SONG) &&
            filters.venue.isNullOrBlank() && filters.tour.isNullOrBlank() &&
            filters.date.isNullOrBlank() && filters.dateFrom.isNullOrBlank() &&
            filters.dateTo.isNullOrBlank() && filters.year == null &&
            filters.state.isNullOrBlank() && filters.city.isNullOrBlank()
        ) {
            songs(filters).forEach { results += it.toSearchResult() }
        }

        if (type == null) {
            val seen = mutableSetOf<Pair<String, String>>()
            return results.filter { r ->
                val key = r::class.simpleName.orEmpty() to r.entityId
                seen.add(key)
            }.take(filters.limit)
        }
        return results.take(filters.limit)
    }

    private suspend fun performances(filters: SearchFilters): List<SetlistEntryEntity> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        filters.song?.takeIf { it.isNotBlank() }?.let {
            clauses += "(se.song LIKE ? OR se.slug LIKE ?)"
            args += "%$it%"; args += "%$it%"
        }
        filters.tour?.takeIf { it.isNotBlank() }?.let {
            clauses += "(COALESCE(s.tour_name, se.tourname) LIKE ?)"
            args += "%$it%"
        }
        filters.venue?.takeIf { it.isNotBlank() }?.let {
            clauses += "(COALESCE(s.venue, se.venue) LIKE ?)"
            args += "%$it%"
        }
        filters.date?.takeIf { it.isNotBlank() }?.let {
            clauses += "se.showdate = ?"; args += it
        }
        filters.dateFrom?.takeIf { it.isNotBlank() }?.let {
            clauses += "se.showdate >= ?"; args += it
        }
        filters.dateTo?.takeIf { it.isNotBlank() }?.let {
            clauses += "se.showdate <= ?"; args += it
        }
        filters.year?.let {
            clauses += "se.showdate LIKE ?"; args += "$it-%"
        }
        filters.state?.takeIf { it.isNotBlank() }?.let {
            clauses += "LOWER(COALESCE(s.state, se.state)) = LOWER(?)"
            args += it
        }
        filters.city?.takeIf { it.isNotBlank() }?.let {
            clauses += "LOWER(COALESCE(s.city, se.city)) LIKE LOWER(?)"
            args += "%$it%"
        }
        filters.query?.takeIf { it.isNotBlank() }?.let {
            clauses += """(
                se.song LIKE ? OR se.slug LIKE ? OR
                COALESCE(s.venue, se.venue) LIKE ? OR
                COALESCE(s.city, se.city) LIKE ? OR
                COALESCE(s.tour_name, se.tourname) LIKE ? OR
                COALESCE(se.footnote, '') LIKE ? OR
                COALESCE(s.setlist_notes, se.setlistnotes, '') LIKE ? OR
                COALESCE(s.soundcheck, se.soundcheck, '') LIKE ?
            )""".trimIndent()
            repeat(8) { args += "%$it%" }
        }

        val where = if (clauses.isEmpty()) "1=1" else clauses.joinToString(" AND ")
        val sql = """
            SELECT se.* FROM setlist_entries se
            LEFT JOIN shows s ON s.showid = se.showid
            WHERE $where
            ORDER BY se.showdate DESC, se.showid, se.set_name, se.position
            LIMIT ?
        """.trimIndent()
        args += filters.limit
        return db.setlistDao().rawSearch(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    private suspend fun shows(filters: SearchFilters): List<ShowEntity> {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<Any>()

        filters.tour?.takeIf { it.isNotBlank() }?.let {
            clauses += "s.tour_name LIKE ?"; args += "%$it%"
        }
        filters.venue?.takeIf { it.isNotBlank() }?.let {
            clauses += "s.venue LIKE ?"; args += "%$it%"
        }
        filters.date?.takeIf { it.isNotBlank() }?.let {
            clauses += "s.showdate = ?"; args += it
        }
        filters.dateFrom?.takeIf { it.isNotBlank() }?.let {
            clauses += "s.showdate >= ?"; args += it
        }
        filters.dateTo?.takeIf { it.isNotBlank() }?.let {
            clauses += "s.showdate <= ?"; args += it
        }
        filters.year?.let {
            clauses += "s.showdate LIKE ?"; args += "$it-%"
        }
        filters.state?.takeIf { it.isNotBlank() }?.let {
            clauses += "LOWER(s.state) = LOWER(?)"; args += it
        }
        filters.city?.takeIf { it.isNotBlank() }?.let {
            clauses += "LOWER(s.city) LIKE LOWER(?)"; args += "%$it%"
        }
        filters.query?.takeIf { it.isNotBlank() }?.let {
            clauses += """(s.venue LIKE ? OR s.city LIKE ? OR s.tour_name LIKE ?
                OR COALESCE(s.setlist_notes, '') LIKE ?
                OR COALESCE(s.soundcheck, '') LIKE ?)""".trimIndent()
            repeat(5) { args += "%$it%" }
        }

        val where = if (clauses.isEmpty()) "1=1" else clauses.joinToString(" AND ")
        val sql = """
            SELECT s.* FROM shows s
            WHERE $where
            ORDER BY s.showdate DESC
            LIMIT ?
        """.trimIndent()
        args += filters.limit
        return db.showDao().rawSearch(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    private suspend fun songs(filters: SearchFilters): List<SongEntity> {
        val q = filters.song ?: filters.query ?: return emptyList()
        if (q.isBlank()) return emptyList()
        return db.songDao().search("%$q%", filters.limit)
    }

    private fun ShowEntity.toSearchResult(): SearchResult =
        SearchResult.Show(
            entityId = showid.toString(),
            title = "$showdate — ${venue.orEmpty()}",
            subtitle = listOfNotNull(city, state, country).joinToString(", "),
            showdate = showdate,
        )

    private fun SetlistEntryEntity.toSearchResult(): SearchResult.Performance {
        val duration = formatDuration(tracktime_seconds).ifBlank { tracktime_raw.orEmpty() }
        val parts = mutableListOf(listOfNotNull(venue, city, state).joinToString(", "))
        if (duration.isNotBlank()) parts += duration
        if (!tourname.isNullOrBlank()) parts += tourname
        return SearchResult.Performance(
            entityId = uniqueid.toString(),
            title = "$song — $showdate",
            subtitle = parts.filter { it.isNotBlank() }.joinToString(" · "),
            showdate = showdate,
            song = song,
        )
    }

    private fun SongEntity.toSearchResult(): SearchResult.Song {
        val sub = buildList {
            times_played?.let { add("Played $it times") }
            debut?.takeIf { it.isNotBlank() }?.let { add("Debut $it") }
        }.joinToString(" · ")
        return SearchResult.Song(
            entityId = songid.toString(),
            title = song,
            subtitle = sub,
        )
    }
}
