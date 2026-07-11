"""Search the local Phish database."""

from __future__ import annotations

from dataclasses import dataclass, field

from .config import DB_PATH
from .export import fetch_export_rows
from .filters import (
    PERFORMANCE_SELECT,
    SHOW_SELECT,
    SONG_SELECT,
    SearchFilters,
    build_performance_where,
    build_show_where,
    build_song_where,
)
from .schema import format_duration, get_connection


SET_LABELS = {
    "1": "Set 1",
    "2": "Set 2",
    "3": "Set 3",
    "4": "Set 4",
    "e": "Encore",
    "e2": "Encore 2",
    "e3": "Encore 3",
}


@dataclass
class SearchResult:
    result_type: str
    title: str
    subtitle: str = ""
    showdate: str | None = None
    entity_id: str | None = None
    details: dict = field(default_factory=dict)


def _fts_query(query: str) -> str:
    terms = []
    for token in query.split():
        token = token.strip()
        if not token:
            continue
        cleaned = "".join(ch for ch in token if ch.isalnum() or ch in {"-", "_"})
        if cleaned:
            terms.append(f'"{cleaned}"*')
    return " AND ".join(terms) if terms else '""'


def _performance_result(row: dict) -> SearchResult:
    duration = format_duration(row.get("tracktime_seconds")) or row.get("tracktime_raw")
    subtitle = f"{row['venue']}, {row['city']}, {row['state']}"
    if duration:
        subtitle += f" · {duration}"
    if row.get("tour_name"):
        subtitle += f" · {row['tour_name']}"
    return SearchResult(
        result_type="song_performance",
        title=f"{row['song']} — {row['showdate']}",
        subtitle=subtitle,
        showdate=row["showdate"],
        entity_id=str(row["uniqueid"]),
        details=row,
    )


def _show_result(row: dict) -> SearchResult:
    return SearchResult(
        result_type="show",
        title=f"{row['showdate']} — {row['venue']}",
        subtitle=f"{row['city']}, {row['state']}, {row['country']}",
        showdate=row["showdate"],
        entity_id=str(row["showid"]),
        details=row,
    )


def _song_result(row: dict) -> SearchResult:
    return SearchResult(
        result_type="song",
        title=row["song"],
        subtitle=f"Played {row['times_played']} times · debut {row['debut']}",
        entity_id=str(row["songid"]),
        details=row,
    )


def search_structured(filters: SearchFilters, db_path=DB_PATH) -> list[SearchResult]:
    conn = get_connection(db_path)
    results: list[SearchResult] = []
    try:
        result_type = filters.result_type

        if result_type in (None, "song_performance"):
            where, params = build_performance_where(filters)
            sql = PERFORMANCE_SELECT.format(where=where)
            params.append(filters.limit)
            for row in conn.execute(sql, params).fetchall():
                results.append(_performance_result(dict(row)))

        if result_type in (None, "show") and not filters.song:
            where, params = build_show_where(filters)
            sql = SHOW_SELECT.format(where=where)
            params.append(filters.limit)
            for row in conn.execute(sql, params).fetchall():
                results.append(_show_result(dict(row)))

        if result_type in (None, "song") and not any(
            [filters.venue, filters.tour, filters.date, filters.date_from, filters.date_to, filters.year, filters.state, filters.city]
        ):
            where, params = build_song_where(filters)
            sql = SONG_SELECT.format(where=where)
            params.append(filters.limit)
            for row in conn.execute(sql, params).fetchall():
                results.append(_song_result(dict(row)))

        if result_type is None:
            seen = set()
            deduped = []
            for r in results:
                key = (r.result_type, r.entity_id)
                if key not in seen:
                    seen.add(key)
                    deduped.append(r)
            return deduped[: filters.limit]

        return results[: filters.limit]
    finally:
        conn.close()


def get_song_stats(
    song_query: str,
    *,
    filters: SearchFilters | None = None,
    db_path=DB_PATH,
) -> dict:
    """Return aggregated analytics for a song with optional location/date filters."""
    f = filters or SearchFilters()
    f.song = f.song or song_query

    if not f.song:
        return {"query": "", "summary": {}, "top_venues": [], "top_locations": [], "yearly": [], "longest": []}

    conn = get_connection(db_path)
    try:
        where, params = build_performance_where(f)

        summary = dict(
            conn.execute(
                f"""
                SELECT
                    COALESCE(MAX(se.song), '') AS song,
                    COUNT(*) AS performances,
                    COUNT(DISTINCT se.showdate) AS shows,
                    COUNT(DISTINCT COALESCE(s.venue, se.venue)) AS venues,
                    COUNT(DISTINCT
                        COALESCE(s.city, se.city) || '|' ||
                        COALESCE(s.state, se.state) || '|' ||
                        COALESCE(s.country, se.country)
                    ) AS locations,
                    MIN(se.tracktime_seconds) AS min_length_seconds,
                    MAX(se.tracktime_seconds) AS max_length_seconds,
                    ROUND(AVG(se.tracktime_seconds), 1) AS avg_length_seconds,
                    MIN(se.showdate) AS first_played,
                    MAX(se.showdate) AS last_played
                FROM setlist_entries se
                LEFT JOIN shows s ON s.showid = se.showid
                WHERE {where}
                """,
                params,
            ).fetchone()
        )

        if not summary.get("performances"):
            return {
                "query": f.song,
                "summary": {},
                "top_venues": [],
                "top_locations": [],
                "yearly": [],
                "longest": [],
            }

        top_venues = [
            dict(row)
            for row in conn.execute(
                f"""
                SELECT COALESCE(s.venue, se.venue) AS venue, COUNT(*) AS performances
                FROM setlist_entries se
                LEFT JOIN shows s ON s.showid = se.showid
                WHERE {where}
                GROUP BY COALESCE(s.venue, se.venue)
                ORDER BY performances DESC, venue
                LIMIT 15
                """,
                params,
            ).fetchall()
        ]

        top_locations = [
            dict(row)
            for row in conn.execute(
                f"""
                SELECT
                    COALESCE(s.city, se.city) AS city,
                    COALESCE(s.state, se.state) AS state,
                    COALESCE(s.country, se.country) AS country,
                    COUNT(*) AS performances
                FROM setlist_entries se
                LEFT JOIN shows s ON s.showid = se.showid
                WHERE {where}
                GROUP BY
                    COALESCE(s.city, se.city),
                    COALESCE(s.state, se.state),
                    COALESCE(s.country, se.country)
                ORDER BY performances DESC, city, state
                LIMIT 15
                """,
                params,
            ).fetchall()
        ]

        yearly = [
            dict(row)
            for row in conn.execute(
                f"""
                SELECT SUBSTR(se.showdate, 1, 4) AS year, COUNT(*) AS performances
                FROM setlist_entries se
                LEFT JOIN shows s ON s.showid = se.showid
                WHERE {where}
                GROUP BY SUBSTR(se.showdate, 1, 4)
                ORDER BY year
                """,
                params,
            ).fetchall()
        ]

        longest = [
            {
                **dict(row),
                "tracktime": format_duration(row["tracktime_seconds"]) if row["tracktime_seconds"] is not None else row["tracktime_raw"],
            }
            for row in conn.execute(
                f"""
                SELECT
                    se.showdate,
                    COALESCE(s.venue, se.venue) AS venue,
                    COALESCE(s.city, se.city) AS city,
                    COALESCE(s.state, se.state) AS state,
                    se.tracktime_seconds,
                    se.tracktime_raw
                FROM setlist_entries se
                LEFT JOIN shows s ON s.showid = se.showid
                WHERE {where} AND se.tracktime_seconds IS NOT NULL
                ORDER BY se.tracktime_seconds DESC, se.showdate DESC
                LIMIT 10
                """,
                params,
            ).fetchall()
        ]

        summary["min_length"] = format_duration(summary.get("min_length_seconds"))
        summary["max_length"] = format_duration(summary.get("max_length_seconds"))
        summary["avg_length"] = format_duration(int(round(summary["avg_length_seconds"]))) if summary.get("avg_length_seconds") is not None else ""

        return {
            "query": f.song,
            "summary": summary,
            "top_venues": top_venues,
            "top_locations": top_locations,
            "yearly": yearly,
            "longest": longest,
        }
    finally:
        conn.close()

def search(
    query: str = "",
    *,
    result_type: str | None = None,
    song: str | None = None,
    tour: str | None = None,
    venue: str | None = None,
    date: str | None = None,
    date_from: str | None = None,
    date_to: str | None = None,
    year: int | None = None,
    state: str | None = None,
    city: str | None = None,
    limit: int = 50,
    db_path=DB_PATH,
) -> list[SearchResult]:
    filters = SearchFilters(
        query=query or None,
        result_type=result_type,
        song=song,
        tour=tour,
        venue=venue,
        date=date,
        date_from=date_from,
        date_to=date_to,
        year=year,
        state=state,
        city=city,
        limit=limit,
    )

    if filters.has_structured_filters() or not query:
        if filters.has_any_criteria():
            return search_structured(filters, db_path)
        return []

    conn = get_connection(db_path)
    try:
        fts = _fts_query(query)
        sql = """
            SELECT entity_type, entity_id, showdate, title, body,
                   bm25(search_index) AS rank
            FROM search_index
            WHERE search_index MATCH ?
        """
        params: list = [fts]

        if result_type:
            sql += " AND entity_type = ?"
            params.append(result_type)
        if year:
            sql += " AND showdate LIKE ?"
            params.append(f"{year}-%")
        if date:
            sql += " AND showdate = ?"
            params.append(date)
        if date_from:
            sql += " AND showdate >= ?"
            params.append(date_from)
        if date_to:
            sql += " AND showdate <= ?"
            params.append(date_to)

        sql += " ORDER BY rank LIMIT ?"
        params.append(limit)

        rows = conn.execute(sql, params).fetchall()
        results: list[SearchResult] = []

        for row in rows:
            entity_type = row["entity_type"]
            entity_id = row["entity_id"]

            if entity_type == "show":
                show = conn.execute("SELECT * FROM shows WHERE showid = ?", (entity_id,)).fetchone()
                if not show:
                    continue
                show_dict = dict(show)
                if tour and (show_dict.get("tour_name") or "").lower().find(tour.lower()) < 0:
                    continue
                if venue and (show_dict.get("venue") or "").lower().find(venue.lower()) < 0:
                    continue
                if state and (show_dict.get("state") or "").lower() != state.lower():
                    continue
                if city and (show_dict.get("city") or "").lower().find(city.lower()) < 0:
                    continue
                if song:
                    has_song = conn.execute(
                        "SELECT 1 FROM setlist_entries WHERE showid = ? AND (song LIKE ? OR slug LIKE ?)",
                        (entity_id, f"%{song}%", f"%{song}%"),
                    ).fetchone()
                    if not has_song:
                        continue
                results.append(_show_result(show_dict))

            elif entity_type == "song_performance":
                perf = conn.execute(
                    "SELECT * FROM setlist_entries WHERE uniqueid = ?", (entity_id,)
                ).fetchone()
                if not perf:
                    continue
                perf_dict = dict(perf)
                if song and song.lower() not in (perf_dict.get("song") or "").lower():
                    continue
                if tour and (perf_dict.get("tourname") or "").lower().find(tour.lower()) < 0:
                    continue
                if venue and (perf_dict.get("venue") or "").lower().find(venue.lower()) < 0:
                    continue
                if state and (perf_dict.get("state") or "").lower() != state.lower():
                    continue
                if city and (perf_dict.get("city") or "").lower().find(city.lower()) < 0:
                    continue
                results.append(_performance_result(perf_dict))

            elif entity_type == "song":
                if song or tour or venue or date or date_from or date_to:
                    continue
                song_row = conn.execute("SELECT * FROM songs WHERE songid = ?", (entity_id,)).fetchone()
                if song_row:
                    results.append(_song_result(dict(song_row)))

        return results
    finally:
        conn.close()


def get_show(showdate: str, db_path=DB_PATH) -> dict | None:
    conn = get_connection(db_path)
    try:
        show = conn.execute("SELECT * FROM shows WHERE showdate = ?", (showdate,)).fetchone()
        if not show:
            return None

        entries = conn.execute(
            """
            SELECT * FROM setlist_entries
            WHERE showdate = ?
            ORDER BY
                CASE set_name
                    WHEN '1' THEN 1 WHEN '2' THEN 2 WHEN '3' THEN 3 WHEN '4' THEN 4
                    WHEN 'e' THEN 5 WHEN 'e2' THEN 6 WHEN 'e3' THEN 7
                    ELSE 99
                END,
                position
            """,
            (showdate,),
        ).fetchall()

        sets: dict[str, list[dict]] = {}
        for entry in entries:
            set_key = entry["set_name"] or "?"
            sets.setdefault(set_key, []).append(
                {
                    "song": entry["song"],
                    "slug": entry["slug"],
                    "position": entry["position"],
                    "transition": entry["trans_mark"],
                    "footnote": entry["footnote"],
                    "tracktime": format_duration(entry["tracktime_seconds"])
                    or entry["tracktime_raw"],
                    "gap": entry["gap"],
                    "is_jam": bool(entry["isjam"]),
                    "is_jamchart": bool(entry["isjamchart"]),
                    "jamchart_description": entry["jamchart_description"],
                }
            )

        return {
            "show": dict(show),
            "sets": [
                {
                    "label": SET_LABELS.get(key, f"Set {key}"),
                    "key": key,
                    "songs": songs,
                }
                for key, songs in sets.items()
            ],
        }
    finally:
        conn.close()


def get_song_history(
    song_query: str,
    limit: int = 100,
    filters: SearchFilters | None = None,
    db_path=DB_PATH,
) -> list[dict]:
    f = filters or SearchFilters()
    f.song = f.song or song_query
    f.limit = limit
    rows = fetch_export_rows("performances", f, db_path)
    return rows


def get_stats(db_path=DB_PATH) -> dict:
    conn = get_connection(db_path)
    try:
        return {
            "shows": conn.execute("SELECT COUNT(*) AS c FROM shows").fetchone()["c"],
            "songs": conn.execute("SELECT COUNT(*) AS c FROM songs").fetchone()["c"],
            "performances": conn.execute("SELECT COUNT(*) AS c FROM setlist_entries").fetchone()["c"],
            "venues": conn.execute("SELECT COUNT(*) AS c FROM venues").fetchone()["c"],
            "date_range": dict(
                conn.execute(
                    "SELECT MIN(showdate) AS first, MAX(showdate) AS last FROM shows"
                ).fetchone()
            ),
        }
    finally:
        conn.close()


def filters_from_request_args(args: dict) -> SearchFilters:
    def _get(key: str) -> str | None:
        val = args.get(key)
        if val is None:
            return None
        val = str(val).strip()
        return val or None

    year_raw = args.get("year")
    year = int(year_raw) if year_raw not in (None, "") else None

    limit_raw = args.get("limit", 50)
    limit = int(limit_raw) if limit_raw not in (None, "") else 50

    return SearchFilters(
        query=_get("q") or _get("query"),
        result_type=_get("type"),
        song=_get("song"),
        tour=_get("tour"),
        venue=_get("venue"),
        date=_get("date"),
        date_from=_get("date_from"),
        date_to=_get("date_to"),
        year=year,
        state=_get("state"),
        city=_get("city"),
        limit=limit,
    )