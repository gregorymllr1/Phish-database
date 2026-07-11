"""SQLite schema and helpers."""

from __future__ import annotations

import re
import sqlite3
from pathlib import Path

from .config import DB_PATH, DATA_DIR


SCHEMA_SQL = """
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS sync_meta (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS venues (
    venueid INTEGER PRIMARY KEY,
    venuename TEXT NOT NULL,
    city TEXT,
    state TEXT,
    country TEXT,
    venuenotes TEXT,
    alias TEXT,
    short_name TEXT
);

CREATE TABLE IF NOT EXISTS songs (
    songid INTEGER PRIMARY KEY,
    song TEXT NOT NULL,
    slug TEXT,
    abbr TEXT,
    artist TEXT,
    debut TEXT,
    last_played TEXT,
    times_played INTEGER,
    last_permalink TEXT,
    debut_permalink TEXT,
    gap REAL
);

CREATE TABLE IF NOT EXISTS shows (
    showid INTEGER PRIMARY KEY,
    showdate TEXT NOT NULL,
    showyear INTEGER,
    showmonth INTEGER,
    showday INTEGER,
    permalink TEXT,
    exclude_from_stats INTEGER DEFAULT 0,
    venueid INTEGER,
    venue TEXT,
    city TEXT,
    state TEXT,
    country TEXT,
    artistid INTEGER,
    artist_name TEXT,
    tourid INTEGER,
    tour_name TEXT,
    setlist_notes TEXT,
    soundcheck TEXT,
    created_at TEXT,
    updated_at TEXT,
    FOREIGN KEY (venueid) REFERENCES venues(venueid)
);

CREATE TABLE IF NOT EXISTS setlist_entries (
    uniqueid INTEGER PRIMARY KEY,
    showid INTEGER NOT NULL,
    showdate TEXT NOT NULL,
    songid INTEGER,
    song TEXT NOT NULL,
    slug TEXT,
    nickname TEXT,
    set_name TEXT NOT NULL,
    position INTEGER,
    transition INTEGER,
    trans_mark TEXT,
    footnote TEXT,
    isjam INTEGER DEFAULT 0,
    isreprise INTEGER DEFAULT 0,
    isjamchart INTEGER DEFAULT 0,
    jamchart_description TEXT,
    tracktime_seconds INTEGER,
    tracktime_raw TEXT,
    gap REAL,
    tourid INTEGER,
    tourname TEXT,
    tourwhen TEXT,
    venueid INTEGER,
    venue TEXT,
    city TEXT,
    state TEXT,
    country TEXT,
    artistid INTEGER,
    artist_name TEXT,
    setlistnotes TEXT,
    soundcheck TEXT,
    FOREIGN KEY (showid) REFERENCES shows(showid),
    FOREIGN KEY (songid) REFERENCES songs(songid)
);

CREATE INDEX IF NOT EXISTS idx_shows_date ON shows(showdate);
CREATE INDEX IF NOT EXISTS idx_shows_year ON shows(showyear);
CREATE INDEX IF NOT EXISTS idx_shows_venue ON shows(venueid);
CREATE INDEX IF NOT EXISTS idx_shows_city ON shows(city);
CREATE INDEX IF NOT EXISTS idx_shows_state ON shows(state);
CREATE INDEX IF NOT EXISTS idx_setlist_show ON setlist_entries(showid);
CREATE INDEX IF NOT EXISTS idx_setlist_date ON setlist_entries(showdate);
CREATE INDEX IF NOT EXISTS idx_setlist_song ON setlist_entries(song);
CREATE INDEX IF NOT EXISTS idx_setlist_slug ON setlist_entries(slug);

CREATE VIRTUAL TABLE IF NOT EXISTS search_index USING fts5(
    entity_type,
    entity_id UNINDEXED,
    showdate UNINDEXED,
    title,
    body,
    tokenize = 'porter unicode61'
);
"""


def parse_tracktime(value: str | int | float | None) -> tuple[int | None, str | None]:
    if value is None or value == "":
        return None, None

    raw = str(value).strip()
    if not raw:
        return None, None

    if re.fullmatch(r"\d+", raw):
        return int(raw), raw

    match = re.fullmatch(r"(\d+):(\d{2})(?::(\d{2}))?", raw)
    if match:
        hours = 0
        if match.group(3) is not None:
            hours, minutes, seconds = int(match.group(1)), int(match.group(2)), int(match.group(3))
        else:
            minutes, seconds = int(match.group(1)), int(match.group(2))
        return hours * 3600 + minutes * 60 + seconds, raw

    try:
        return int(float(raw)), raw
    except ValueError:
        return None, raw


def format_duration(seconds: int | None) -> str:
    if seconds is None:
        return ""
    minutes, secs = divmod(seconds, 60)
    if minutes >= 60:
        hours, minutes = divmod(minutes, 60)
        return f"{hours}:{minutes:02d}:{secs:02d}"
    return f"{minutes}:{secs:02d}"


def get_connection(db_path: Path | None = None) -> sqlite3.Connection:
    path = db_path or DB_PATH
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    return conn


def init_db(db_path: Path | None = None) -> sqlite3.Connection:
    conn = get_connection(db_path)
    conn.executescript(SCHEMA_SQL)
    conn.commit()
    return conn


def set_meta(conn: sqlite3.Connection, key: str, value: str) -> None:
    conn.execute(
        """
        INSERT INTO sync_meta (key, value, updated_at)
        VALUES (?, ?, datetime('now'))
        ON CONFLICT(key) DO UPDATE SET
            value = excluded.value,
            updated_at = datetime('now')
        """,
        (key, value),
    )


def get_meta(conn: sqlite3.Connection, key: str) -> str | None:
    row = conn.execute("SELECT value FROM sync_meta WHERE key = ?", (key,)).fetchone()
    return row["value"] if row else None