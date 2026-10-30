package com.phishdb.app.data.repo

import com.phishdb.app.data.db.SetlistEntryEntity
import com.phishdb.app.data.db.ShowEntity
import com.phishdb.app.data.db.SongEntity
import com.phishdb.app.data.db.VenueEntity
import com.phishdb.app.data.model.SetlistEntryDto
import com.phishdb.app.data.model.ShowDto
import com.phishdb.app.data.model.SongDto
import com.phishdb.app.data.model.VenueDto

internal fun String?.toInt0Or1(): Int =
    if (this?.lowercase() in setOf("1", "true")) 1 else 0

internal fun Int?.toInt0Or1(): Int = if (this == 1) 1 else 0

internal fun String?.toDoubleOrNullSafe(): Double? = this?.toDoubleOrNull()
internal fun String?.toIntOrNullSafe(): Int? = this?.toIntOrNull()

fun parseTrackTime(raw: String?): Pair<Int?, String?> {
    if (raw.isNullOrBlank()) return null to null
    val value = raw.trim()
    if (value.matches(Regex("""^\d+$"""))) return value.toInt() to value
    val hms = Regex("""^(\d+):(\d{2})(?::(\d{2}))?$""").matchEntire(value)
    if (hms != null) {
        val g1 = hms.groupValues[1].toInt()
        val g2 = hms.groupValues[2].toInt()
        val g3 = hms.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
        val secs = if (g3 != null) g1 * 3600 + g2 * 60 + g3 else g1 * 60 + g2
        return secs to value
    }
    return value.toDoubleOrNull()?.toInt() to value
}

fun VenueDto.toEntity(): VenueEntity? {
    val id = venueid ?: return null
    return VenueEntity(
        venueid = id,
        venuename = venuename.orEmpty(),
        city = city,
        state = state,
        country = country,
        venuenotes = venuenotes,
        alias = alias,
        short_name = shortName,
    )
}

fun SongDto.toEntity(): SongEntity? {
    val id = songid ?: return null
    return SongEntity(
        songid = id,
        song = song.orEmpty(),
        slug = slug,
        abbr = abbr,
        artist = artist,
        debut = debut,
        last_played = lastPlayed,
        times_played = timesPlayed,
        last_permalink = lastPermalink,
        debut_permalink = debutPermalink,
        gap = gap?.toDoubleOrNull(),
    )
}

fun ShowDto.toEntity(): ShowEntity? {
    val id = showid ?: return null
    val sd = showdate ?: return null
    return ShowEntity(
        showid = id,
        showdate = sd,
        showyear = showyear?.toIntOrNull() ?: sd.take(4).toIntOrNull(),
        showmonth = showmonth?.toIntOrNull() ?: sd.split("-").getOrNull(1)?.toIntOrNull(),
        showday = showday?.toIntOrNull() ?: sd.split("-").getOrNull(2)?.toIntOrNull(),
        permalink = permalink,
        exclude_from_stats = excludeFromStats.toInt0Or1(),
        venueid = venueid,
        venue = venue,
        city = city,
        state = state,
        country = country,
        artistid = artistid,
        artist_name = artistName,
        tourid = tourid,
        tour_name = tourName,
        setlist_notes = setlistNotes,
        soundcheck = soundcheck,
        created_at = createdAt,
        updated_at = updatedAt,
    )
}

fun SetlistEntryDto.toEntity(): SetlistEntryEntity? {
    val id = uniqueid ?: return null
    val sid = showid ?: return null
    val sd = showdate ?: return null
    val (secs, raw) = parseTrackTime(tracktime)
    return SetlistEntryEntity(
        uniqueid = id,
        showid = sid,
        showdate = sd,
        songid = songid,
        song = song.orEmpty(),
        slug = slug,
        nickname = nickname,
        set_name = setName.orEmpty(),
        position = position,
        transition = transition,
        trans_mark = transMark,
        footnote = footnote,
        isjam = isjam.toInt0Or1(),
        isreprise = isreprise.toInt0Or1(),
        isjamchart = isjamchart.toInt0Or1(),
        jamchart_description = jamchartDescription,
        tracktime_seconds = secs,
        tracktime_raw = raw,
        gap = gap?.toDoubleOrNull(),
        tourid = tourid,
        tourname = tourname ?: tourNameAlt,
        tourwhen = tourwhen,
        venueid = venueid,
        venue = venue,
        city = city,
        state = state,
        country = country,
        artistid = artistid,
        artist_name = artistName,
        setlistnotes = setlistNotes ?: setlistNotesAlt,
        soundcheck = soundcheck,
    )
}

fun List<SetlistEntryDto>.derivedVenues(): List<VenueEntity> {
    val map = LinkedHashMap<Long, VenueEntity>()
    for (e in this) {
        val id = e.venueid ?: continue
        if (map.containsKey(id)) continue
        map[id] = VenueEntity(
            venueid = id,
            venuename = e.venue ?: "Venue $id",
            city = e.city,
            state = e.state,
            country = e.country,
            venuenotes = null,
            alias = null,
            short_name = null,
        )
    }
    return map.values.toList()
}

fun List<SetlistEntryDto>.derivedSongs(): List<SongEntity> {
    val map = LinkedHashMap<Long, SongEntity>()
    for (e in this) {
        val id = e.songid ?: continue
        if (map.containsKey(id)) continue
        map[id] = SongEntity(
            songid = id,
            song = e.song ?: e.nickname ?: "Song $id",
            slug = e.slug,
            abbr = null,
            artist = e.artistName,
            debut = null,
            last_played = null,
            times_played = null,
            last_permalink = null,
            debut_permalink = null,
            gap = null,
        )
    }
    return map.values.toList()
}

fun List<SetlistEntryDto>.derivedShows(): List<ShowEntity> {
    val map = LinkedHashMap<Long, ShowEntity>()
    for (e in this) {
        val id = e.showid ?: continue
        if (map.containsKey(id)) continue
        val sd = e.showdate ?: continue
        val parts = sd.split("-")
        map[id] = ShowEntity(
            showid = id,
            showdate = sd,
            showyear = parts.getOrNull(0)?.toIntOrNull(),
            showmonth = parts.getOrNull(1)?.toIntOrNull(),
            showday = parts.getOrNull(2)?.toIntOrNull(),
            permalink = null,
            exclude_from_stats = 0,
            venueid = e.venueid,
            venue = e.venue,
            city = e.city,
            state = e.state,
            country = e.country,
            artistid = e.artistid,
            artist_name = e.artistName,
            tourid = e.tourid,
            tour_name = e.tourname ?: e.tourNameAlt,
            setlist_notes = e.setlistNotes ?: e.setlistNotesAlt,
            soundcheck = e.soundcheck,
            created_at = null,
            updated_at = null,
        )
    }
    return map.values.toList()
}
