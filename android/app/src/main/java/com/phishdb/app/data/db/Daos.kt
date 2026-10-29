package com.phishdb.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface VenueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(venues: List<VenueEntity>)

    @Query("DELETE FROM venues")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM venues")
    fun count(): Flow<Int>
}

@Dao
interface SongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("SELECT * FROM songs WHERE song LIKE :q OR slug LIKE :q ORDER BY song LIMIT :limit")
    suspend fun search(q: String, limit: Int): List<SongEntity>

    @Query("SELECT * FROM songs WHERE songid = :id")
    suspend fun get(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE song = :name LIMIT 1")
    suspend fun getByName(name: String): SongEntity?

    @Query("SELECT COUNT(*) FROM songs")
    fun count(): Flow<Int>
}

@Dao
interface ShowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(shows: List<ShowEntity>)

    @Query("SELECT * FROM shows WHERE showdate = :date LIMIT 1")
    suspend fun getByDate(date: String): ShowEntity?

    @Query("SELECT * FROM shows WHERE showid = :id")
    suspend fun getById(id: Long): ShowEntity?

    @Query("SELECT COUNT(*) FROM shows")
    fun count(): Flow<Int>

    @Query("SELECT MIN(showdate) FROM shows")
    suspend fun firstDate(): String?

    @Query("SELECT MAX(showdate) FROM shows")
    suspend fun lastDate(): String?

    @RawQuery(observedEntities = [ShowEntity::class])
    suspend fun rawSearch(query: SupportSQLiteQuery): List<ShowEntity>
}

@Dao
interface SetlistEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<SetlistEntryEntity>)

    @Query("DELETE FROM setlist_entries WHERE showdate >= :startDate AND showdate <= :endDate")
    suspend fun deleteByDateRange(startDate: String, endDate: String)

    @Query(
        """
        SELECT * FROM setlist_entries
        WHERE showdate = :date
        ORDER BY
            CASE set_name
                WHEN '1' THEN 1 WHEN '2' THEN 2 WHEN '3' THEN 3 WHEN '4' THEN 4
                WHEN 'e' THEN 5 WHEN 'e2' THEN 6 WHEN 'e3' THEN 7
                ELSE 99 END,
            position
        """
    )
    suspend fun byShowDate(date: String): List<SetlistEntryEntity>

    @Query("SELECT COUNT(*) FROM setlist_entries")
    fun count(): Flow<Int>

    @RawQuery(observedEntities = [SetlistEntryEntity::class])
    suspend fun rawSearch(query: SupportSQLiteQuery): List<SetlistEntryEntity>
}

@Dao
interface MetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: SyncMetaEntity)

    @Query("SELECT value FROM sync_meta WHERE key = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM sync_meta WHERE key = :key LIMIT 1")
    fun observe(key: String): Flow<String?>
}
