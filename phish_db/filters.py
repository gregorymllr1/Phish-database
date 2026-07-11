"""Structured search filters and SQL query builders."""

from __future__ import annotations

from dataclasses import dataclass, fields


@dataclass
class SearchFilters:
    query: str | None = None
    result_type: str | None = None
    song: str | None = None
    tour: str | None = None
    venue: str | None = None
    date: str | None = None
    date_from: str | None = None
    date_to: str | None = None
    year: int | None = None
    state: str | None = None
    city: str | None = None
    limit: int = 500

    def has_structured_filters(self) -> bool:
        return any(
            getattr(self, f.name)
            for f in fields(self)
            if f.name not in {"query", "result_type", "limit"}
        )

    def has_any_criteria(self) -> bool:
        return bool(self.query) or self.has_structured_filters()


def _like(value: str) -> str:
    return f"%{value}%"


def build_performance_where(filters: SearchFilters) -> tuple[str, list]:
    clauses: list[str] = []
    params: list = []

    if filters.song:
        clauses.append("(se.song LIKE ? OR se.slug LIKE ?)")
        params.extend([_like(filters.song), _like(filters.song)])

    if filters.tour:
        clauses.append("(COALESCE(s.tour_name, se.tourname) LIKE ?)")
        params.append(_like(filters.tour))

    if filters.venue:
        clauses.append("(COALESCE(s.venue, se.venue) LIKE ?)")
        params.append(_like(filters.venue))

    if filters.date:
        clauses.append("se.showdate = ?")
        params.append(filters.date)

    if filters.date_from:
        clauses.append("se.showdate >= ?")
        params.append(filters.date_from)

    if filters.date_to:
        clauses.append("se.showdate <= ?")
        params.append(filters.date_to)

    if filters.year:
        clauses.append("se.showdate LIKE ?")
        params.append(f"{filters.year}-%")

    if filters.state:
        clauses.append("LOWER(COALESCE(s.state, se.state)) = LOWER(?)")
        params.append(filters.state)

    if filters.city:
        clauses.append("LOWER(COALESCE(s.city, se.city)) LIKE LOWER(?)")
        params.append(_like(filters.city))

    if filters.query:
        clauses.append(
            """(
                se.song LIKE ? OR se.slug LIKE ? OR
                COALESCE(s.venue, se.venue) LIKE ? OR
                COALESCE(s.city, se.city) LIKE ? OR
                COALESCE(s.tour_name, se.tourname) LIKE ? OR
                COALESCE(se.footnote, '') LIKE ? OR
                COALESCE(s.setlist_notes, se.setlistnotes, '') LIKE ? OR
                COALESCE(s.soundcheck, se.soundcheck, '') LIKE ?
            )"""
        )
        q = _like(filters.query)
        params.extend([q, q, q, q, q, q, q, q])

    where = " AND ".join(clauses) if clauses else "1=1"
    return where, params


PERFORMANCE_SELECT = """
    SELECT
        se.showdate,
        se.song,
        se.slug,
        se.set_name,
        se.position,
        se.trans_mark AS transition,
        se.footnote,
        se.tracktime_seconds,
        se.tracktime_raw,
        se.gap,
        se.isjam,
        se.isjamchart,
        se.jamchart_description,
        COALESCE(s.venue, se.venue) AS venue,
        COALESCE(s.city, se.city) AS city,
        COALESCE(s.state, se.state) AS state,
        COALESCE(s.country, se.country) AS country,
        COALESCE(s.tour_name, se.tourname) AS tour_name,
        COALESCE(s.soundcheck, se.soundcheck) AS soundcheck,
        COALESCE(s.setlist_notes, se.setlistnotes) AS setlist_notes,
        se.showid,
        se.uniqueid
    FROM setlist_entries se
    LEFT JOIN shows s ON s.showid = se.showid
    WHERE {where}
    ORDER BY se.showdate DESC, se.showid, se.set_name, se.position
    LIMIT ?
"""


def build_show_where(filters: SearchFilters) -> tuple[str, list]:
    clauses: list[str] = []
    params: list = []

    if filters.tour:
        clauses.append("s.tour_name LIKE ?")
        params.append(_like(filters.tour))

    if filters.venue:
        clauses.append("s.venue LIKE ?")
        params.append(_like(filters.venue))

    if filters.date:
        clauses.append("s.showdate = ?")
        params.append(filters.date)

    if filters.date_from:
        clauses.append("s.showdate >= ?")
        params.append(filters.date_from)

    if filters.date_to:
        clauses.append("s.showdate <= ?")
        params.append(filters.date_to)

    if filters.year:
        clauses.append("s.showdate LIKE ?")
        params.append(f"{filters.year}-%")

    if filters.state:
        clauses.append("LOWER(s.state) = LOWER(?)")
        params.append(filters.state)

    if filters.city:
        clauses.append("LOWER(s.city) LIKE LOWER(?)")
        params.append(_like(filters.city))

    if filters.song:
        clauses.append(
            """EXISTS (
                SELECT 1 FROM setlist_entries se
                WHERE se.showid = s.showid
                AND (se.song LIKE ? OR se.slug LIKE ?)
            )"""
        )
        params.extend([_like(filters.song), _like(filters.song)])

    if filters.query:
        clauses.append(
            """(
                s.venue LIKE ? OR s.city LIKE ? OR s.tour_name LIKE ? OR
                COALESCE(s.setlist_notes, '') LIKE ? OR COALESCE(s.soundcheck, '') LIKE ?
            )"""
        )
        q = _like(filters.query)
        params.extend([q, q, q, q, q])

    where = " AND ".join(clauses) if clauses else "1=1"
    return where, params


SHOW_SELECT = """
    SELECT
        s.showdate,
        s.venue,
        s.city,
        s.state,
        s.country,
        s.tour_name,
        s.soundcheck,
        s.setlist_notes,
        s.showid,
        s.permalink
    FROM shows s
    WHERE {where}
    ORDER BY s.showdate DESC
    LIMIT ?
"""


def build_song_where(filters: SearchFilters) -> tuple[str, list]:
    clauses: list[str] = []
    params: list = []

    if filters.song:
        clauses.append("(song LIKE ? OR slug LIKE ?)")
        params.extend([_like(filters.song), _like(filters.song)])

    if filters.query:
        clauses.append("(song LIKE ? OR slug LIKE ?)")
        q = _like(filters.query)
        params.extend([q, q])

    where = " AND ".join(clauses) if clauses else "1=1"
    return where, params


SONG_SELECT = """
    SELECT song, slug, times_played, debut, last_played, gap, songid
    FROM songs
    WHERE {where}
    ORDER BY song
    LIMIT ?
"""