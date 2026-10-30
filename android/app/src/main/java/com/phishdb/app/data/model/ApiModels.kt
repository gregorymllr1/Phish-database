package com.phishdb.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiEnvelope<T>(
    val data: List<T> = emptyList(),
    @Json(name = "error_code") val errorCode: Int? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
)

@JsonClass(generateAdapter = true)
data class ShowDto(
    val showid: Long? = null,
    val showdate: String? = null,
    val showyear: String? = null,
    val showmonth: String? = null,
    val showday: String? = null,
    val permalink: String? = null,
    @Json(name = "exclude_from_stats") val excludeFromStats: String? = null,
    val venueid: Long? = null,
    val venue: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val artistid: Long? = null,
    @Json(name = "artist_name") val artistName: String? = null,
    val tourid: Long? = null,
    @Json(name = "tour_name") val tourName: String? = null,
    @Json(name = "setlist_notes") val setlistNotes: String? = null,
    val soundcheck: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SetlistEntryDto(
    val uniqueid: Long? = null,
    val showid: Long? = null,
    val showdate: String? = null,
    val songid: Long? = null,
    val song: String? = null,
    val slug: String? = null,
    val nickname: String? = null,
    @Json(name = "set") val setName: String? = null,
    val position: Int? = null,
    val transition: Int? = null,
    @Json(name = "trans_mark") val transMark: String? = null,
    val footnote: String? = null,
    val isjam: Int? = null,
    val isreprise: Int? = null,
    val isjamchart: Int? = null,
    @Json(name = "jamchart_description") val jamchartDescription: String? = null,
    val tracktime: String? = null,
    val gap: String? = null,
    val tourid: Long? = null,
    val tourname: String? = null,
    val tourwhen: String? = null,
    val venueid: Long? = null,
    val venue: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val artistid: Long? = null,
    @Json(name = "artist_name") val artistName: String? = null,
    @Json(name = "setlistnotes") val setlistNotes: String? = null,
    val soundcheck: String? = null,
    @Json(name = "setlist_notes") val setlistNotesAlt: String? = null,
    @Json(name = "tour_name") val tourNameAlt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SongDto(
    val songid: Long? = null,
    val song: String? = null,
    val slug: String? = null,
    val abbr: String? = null,
    val artist: String? = null,
    val debut: String? = null,
    @Json(name = "last_played") val lastPlayed: String? = null,
    @Json(name = "times_played") val timesPlayed: Int? = null,
    @Json(name = "last_permalink") val lastPermalink: String? = null,
    @Json(name = "debut_permalink") val debutPermalink: String? = null,
    val gap: String? = null,
)

@JsonClass(generateAdapter = true)
data class VenueDto(
    val venueid: Long? = null,
    val venuename: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val venuenotes: String? = null,
    val alias: String? = null,
    @Json(name = "short_name") val shortName: String? = null,
)
