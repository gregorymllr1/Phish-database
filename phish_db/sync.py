"""Sync Phish.net data into the local SQLite database."""

from __future__ import annotations

import json
from datetime import datetime

from .api import PhishAPIClient, PhishAPIError
from .config import DB_PATH, FIRST_SHOW_YEAR
from .schema import init_db, parse_tracktime, set_meta


def _bool_int(value) -> int:
    return 1 if str(value) in {"1", "true", "True"} else 0


def upsert_venues(conn, venues: list[dict]) -> int:
    conn.executemany(
        """
        INSERT INTO venues (venueid, venuename, city, state, country, venuenotes, alias, short_name)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(venueid) DO UPDATE SET
            venuename = excluded.venuename,
            city = excluded.city,
            state = excluded.state,
            country = excluded.country,
            venuenotes = excluded.venuenotes,
            alias = excluded.alias,
            short_name = excluded.short_name
        """,
        [
            (
                v.get("venueid"),
                v.get("venuename") or v.get("venue"),
                v.get("city"),
                v.get("state"),
                v.get("country"),
                v.get("venuenotes"),
                v.get("alias"),
                v.get("short_name"),
            )
            for v in venues
        ],
    )
    return len(venues)


def upsert_songs(conn, songs: list[dict]) -> int:
    conn.executemany(
        """
        INSERT INTO songs (
            songid, song, slug, abbr, artist, debut, last_played,
            times_played, last_permalink, debut_permalink, gap
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(songid) DO UPDATE SET
            song = excluded.song,
            slug = excluded.slug,
            abbr = excluded.abbr,
            artist = excluded.artist,
            debut = excluded.debut,
            last_played = excluded.last_played,
            times_played = excluded.times_played,
            last_permalink = excluded.last_permalink,
            debut_permalink = excluded.debut_permalink,
            gap = excluded.gap
        """,
        [
            (
                s.get("songid"),
                s.get("song"),
                s.get("slug"),
                s.get("abbr"),
                s.get("artist"),
                s.get("debut"),
                s.get("last_played"),
                s.get("times_played"),
                s.get("last_permalink"),
                s.get("debut_permalink"),
                s.get("gap"),
            )
            for s in songs
        ],
    )
    return len(songs)


def upsert_shows(conn, shows: list[dict]) -> int:
    conn.executemany(
        """
        INSERT INTO shows (
            showid, showdate, showyear, showmonth, showday, permalink,
            exclude_from_stats, venueid, venue, city, state, country,
            artistid, artist_name, tourid, tour_name, setlist_notes,
            soundcheck, created_at, updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(showid) DO UPDATE SET
            showdate = excluded.showdate,
            showyear = excluded.showyear,
            showmonth = excluded.showmonth,
            showday = excluded.showday,
            permalink = excluded.permalink,
            exclude_from_stats = excluded.exclude_from_stats,
            venueid = excluded.venueid,
            venue = excluded.venue,
            city = excluded.city,
            state = excluded.state,
            country = excluded.country,
            artistid = excluded.artistid,
            artist_name = excluded.artist_name,
            tourid = excluded.tourid,
            tour_name = excluded.tour_name,
            setlist_notes = excluded.setlist_notes,
            soundcheck = excluded.soundcheck,
            created_at = excluded.created_at,
            updated_at = excluded.updated_at
        """,
        [
            (
                s.get("showid"),
                s.get("showdate"),
                s.get("showyear"),
                s.get("showmonth"),
                s.get("showday"),
                s.get("permalink"),
                _bool_int(s.get("exclude_from_stats")),
                s.get("venueid"),
                s.get("venue"),
                s.get("city"),
                s.get("state"),
                s.get("country"),
                s.get("artistid"),
                s.get("artist_name"),
                s.get("tourid"),
                s.get("tour_name") or s.get("tourname"),
                s.get("setlist_notes") or s.get("setlistnotes"),
                s.get("soundcheck"),
                s.get("created_at"),
                s.get("updated_at"),
            )
            for s in shows
        ],
    )
    return len(shows)


def upsert_setlist_entries(conn, entries: list[dict]) -> int:
    rows = []
    for entry in entries:
        seconds, raw = parse_tracktime(entry.get("tracktime"))
        rows.append(
            (
                entry.get("uniqueid"),
                entry.get("showid"),
                entry.get("showdate"),
                entry.get("songid"),
                entry.get("song"),
                entry.get("slug"),
                entry.get("nickname"),
                entry.get("set"),
                entry.get("position"),
                entry.get("transition"),
                entry.get("trans_mark"),
                entry.get("footnote"),
                _bool_int(entry.get("isjam")),
                _bool_int(entry.get("isreprise")),
                _bool_int(entry.get("isjamchart")),
                entry.get("jamchart_description"),
                seconds,
                raw,
                entry.get("gap"),
                entry.get("tourid"),
                entry.get("tourname"),
                entry.get("tourwhen"),
                entry.get("venueid"),
                entry.get("venue"),
                entry.get("city"),
                entry.get("state"),
                entry.get("country"),
                entry.get("artistid"),
                entry.get("artist_name"),
                entry.get("setlistnotes"),
                entry.get("soundcheck"),
            )
        )

    conn.executemany(
        """
        INSERT INTO setlist_entries (
            uniqueid, showid, showdate, songid, song, slug, nickname, set_name,
            position, transition, trans_mark, footnote, isjam, isreprise,
            isjamchart, jamchart_description, tracktime_seconds, tracktime_raw,
            gap, tourid, tourname, tourwhen, venueid, venue, city, state,
            country, artistid, artist_name, setlistnotes, soundcheck
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(uniqueid) DO UPDATE SET
            showid = excluded.showid,
            showdate = excluded.showdate,
            songid = excluded.songid,
            song = excluded.song,
            slug = excluded.slug,
            nickname = excluded.nickname,
            set_name = excluded.set_name,
            position = excluded.position,
            transition = excluded.transition,
            trans_mark = excluded.trans_mark,
            footnote = excluded.footnote,
            isjam = excluded.isjam,
            isreprise = excluded.isreprise,
            isjamchart = excluded.isjamchart,
            jamchart_description = excluded.jamchart_description,
            tracktime_seconds = excluded.tracktime_seconds,
            tracktime_raw = excluded.tracktime_raw,
            gap = excluded.gap,
            tourid = excluded.tourid,
            tourname = excluded.tourname,
            tourwhen = excluded.tourwhen,
            venueid = excluded.venueid,
            venue = excluded.venue,
            city = excluded.city,
            state = excluded.state,
            country = excluded.country,
            artistid = excluded.artistid,
            artist_name = excluded.artist_name,
            setlistnotes = excluded.setlistnotes,
            soundcheck = excluded.soundcheck
        """,
        rows,
    )
    return len(rows)


def rebuild_search_index(conn) -> None:
    conn.execute("DELETE FROM search_index")

    conn.execute(
        """
        INSERT INTO search_index (entity_type, entity_id, showdate, title, body)
        SELECT
            'show',
            CAST(showid AS TEXT),
            showdate,
            venue || ' — ' || city || ', ' || COALESCE(state, '') || COALESCE(', ' || country, ''),
            COALESCE(setlist_notes, '') || ' ' || COALESCE(soundcheck, '') || ' ' ||
            COALESCE(tour_name, '') || ' ' || COALESCE(artist_name, '')
        FROM shows
        """
    )

    conn.execute(
        """
        INSERT INTO search_index (entity_type, entity_id, showdate, title, body)
        SELECT
            'song_performance',
            CAST(uniqueid AS TEXT),
            showdate,
            song || ' @ ' || venue,
            COALESCE(footnote, '') || ' ' || COALESCE(jamchart_description, '') || ' ' ||
            COALESCE(setlistnotes, '') || ' ' || COALESCE(soundcheck, '') || ' set:' || set_name
        FROM setlist_entries
        """
    )

    conn.execute(
        """
        INSERT INTO search_index (entity_type, entity_id, showdate, title, body)
        SELECT
            'song',
            CAST(songid AS TEXT),
            NULL,
            song,
            COALESCE(slug, '') || ' ' || COALESCE(artist, '') || ' played ' ||
            COALESCE(CAST(times_played AS TEXT), '') || ' times'
        FROM songs
        """
    )


def venues_from_setlists(entries: list[dict]) -> list[dict]:
    by_venue: dict[int, dict] = {}
    for entry in entries:
        venueid = entry.get("venueid")
        if venueid is None:
            continue
        if venueid not in by_venue:
            by_venue[venueid] = {
                "venueid": venueid,
                "venuename": entry.get("venue") or f"Venue {venueid}",
                "city": entry.get("city"),
                "state": entry.get("state"),
                "country": entry.get("country"),
            }
    return list(by_venue.values())


def songs_from_setlists(entries: list[dict]) -> list[dict]:
    by_song: dict[int, dict] = {}
    for entry in entries:
        songid = entry.get("songid")
        if songid is None:
            continue
        if songid not in by_song:
            by_song[songid] = {
                "songid": songid,
                "song": entry.get("song") or entry.get("nickname") or f"Song {songid}",
                "slug": entry.get("slug"),
            }
    return list(by_song.values())


def shows_from_setlists(entries: list[dict]) -> list[dict]:
    by_show: dict[int, dict] = {}
    for entry in entries:
        showid = entry.get("showid")
        if showid is None:
            continue
        if showid not in by_show:
            showdate = entry.get("showdate")
            showyear = entry.get("showyear")
            showmonth = None
            showday = None
            if showdate and len(showdate) >= 10:
                parts = showdate.split("-")
                if len(parts) == 3:
                    showyear = showyear or int(parts[0])
                    showmonth = int(parts[1])
                    showday = int(parts[2])
            by_show[showid] = {
                "showid": showid,
                "showdate": showdate,
                "showyear": showyear,
                "showmonth": showmonth,
                "showday": showday,
                "permalink": entry.get("permalink"),
                "venueid": entry.get("venueid"),
                "venue": entry.get("venue"),
                "city": entry.get("city"),
                "state": entry.get("state"),
                "country": entry.get("country"),
                "artistid": entry.get("artistid"),
                "artist_name": entry.get("artist_name"),
                "tourid": entry.get("tourid"),
                "tour_name": entry.get("tourname"),
                "setlist_notes": entry.get("setlistnotes"),
                "soundcheck": entry.get("soundcheck"),
            }
    return list(by_show.values())


def sync_setlist_batch(conn, entries: list[dict]) -> int:
    """Upsert venues, songs, shows, then setlist rows for a batch of API entries."""
    if not entries:
        return 0
    upsert_venues(conn, venues_from_setlists(entries))
    upsert_songs(conn, songs_from_setlists(entries))
    upsert_shows(conn, shows_from_setlists(entries))
    return upsert_setlist_entries(conn, entries)


def sync_all(
    api: PhishAPIClient | None = None,
    db_path=DB_PATH,
    start_year: int = FIRST_SHOW_YEAR,
    end_year: int | None = None,
    progress_callback=None,
) -> dict:
    api = api or PhishAPIClient()
    end_year = end_year or datetime.now().year
    conn = init_db(db_path)

    stats = {
        "venues": 0,
        "songs": 0,
        "shows": 0,
        "setlist_entries": 0,
        "years_synced": [],
    }

    def report(message: str) -> None:
        if progress_callback:
            progress_callback(message)

    try:
        report("Fetching venues...")
        stats["venues"] = upsert_venues(conn, api.get_venues())
        conn.commit()

        report("Fetching songs...")
        stats["songs"] = upsert_songs(conn, api.get_songs())
        conn.commit()

        report("Fetching all Phish shows...")
        shows = api.get_shows(order_by="showdate", direction="asc")
        stats["shows"] = upsert_shows(conn, shows)
        conn.commit()

        for year in range(start_year, end_year + 1):
            report(f"Fetching setlists for {year}...")
            entries = api.get_setlists_by_year(year, order_by="showdate", direction="asc")
            if entries:
                count = sync_setlist_batch(conn, entries)
                stats["setlist_entries"] += count
                stats["years_synced"].append(year)
                conn.commit()

        report("Building search index...")
        rebuild_search_index(conn)
        set_meta(conn, "last_sync", datetime.now().isoformat())
        set_meta(conn, "sync_stats", json.dumps(stats))
        conn.commit()
        report("Sync complete.")
        return stats
    except PhishAPIError:
        conn.rollback()
        raise
    finally:
        conn.close()


def sync_year(year: int, api: PhishAPIClient | None = None, db_path=DB_PATH) -> int:
    api = api or PhishAPIClient()
    conn = init_db(db_path)
    try:
        entries = api.get_setlists_by_year(year, order_by="showdate", direction="asc")
        if not entries:
            return 0
        count = sync_setlist_batch(conn, entries)
        rebuild_search_index(conn)
        set_meta(conn, f"year_sync_{year}", datetime.now().isoformat())
        conn.commit()
        return count
    finally:
        conn.close()