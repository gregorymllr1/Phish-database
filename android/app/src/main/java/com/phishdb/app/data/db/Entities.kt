package com.phishdb.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class VenueEntity(
    @PrimaryKey val venueid: Long,
    val venuename: String,
    val city: String?,
    val state: String?,
    val country: String?,
    val venuenotes: String?,
    val alias: String?,
    val short_name: String?,
)

@Entity(
    tableName = "songs",
    indices = [Index(value = ["song"]), Index(value = ["slug"])]
)
data class SongEntity(
    @PrimaryKey val songid: Long,
    val song: String,
    val slug: String?,
    val abbr: String?,
    val artist: String?,
    val debut: String?,
    val last_played: String?,
    val times_played: Int?,
    val last_permalink: String?,
    val debut_permalink: String?,
    val gap: Double?,
)

@Entity(
    tableName = "shows",
    indices = [
        Index(value = ["showdate"]),
        Index(value = ["showyear"]),
        Index(value = ["venueid"]),
        Index(value = ["city"]),
        Index(value = ["state"]),
    ]
)
data class ShowEntity(
    @PrimaryKey val showid: Long,
    val showdate: String,
    val showyear: Int?,
    val showmonth: Int?,
    val showday: Int?,
    val permalink: String?,
    val exclude_from_stats: Int = 0,
    val venueid: Long?,
    val venue: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val artistid: Long?,
    val artist_name: String?,
    val tourid: Long?,
    val tour_name: String?,
    val setlist_notes: String?,
    val soundcheck: String?,
    val created_at: String?,
    val updated_at: String?,
)

@Entity(
    tableName = "setlist_entries",
    indices = [
        Index(value = ["showid"]),
        Index(value = ["showdate"]),
        Index(value = ["song"]),
        Index(value = ["slug"]),
    ]
)
data class SetlistEntryEntity(
    @PrimaryKey val uniqueid: Long,
    val showid: Long,
    val showdate: String,
    val songid: Long?,
    val song: String,
    val slug: String?,
    val nickname: String?,
    val set_name: String,
    val position: Int?,
    val transition: Int?,
    val trans_mark: String?,
    val footnote: String?,
    val isjam: Int = 0,
    val isreprise: Int = 0,
    val isjamchart: Int = 0,
    val jamchart_description: String?,
    val tracktime_seconds: Int?,
    val tracktime_raw: String?,
    val gap: Double?,
    val tourid: Long?,
    val tourname: String?,
    val tourwhen: String?,
    val venueid: Long?,
    val venue: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val artistid: Long?,
    val artist_name: String?,
    val setlistnotes: String?,
    val soundcheck: String?,
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updated_at: String?,
)
