"""Build the database that ships inside the Android app.

Creates the tables exactly as the app's Room schema defines them (so Room accepts the
file without a migration), copies rows from the desktop database, and leaves out the
desktop-only full-text search index.
"""

from __future__ import annotations

import json
import sqlite3
from pathlib import Path

from .config import DB_PATH, ROOT_DIR

ANDROID_APP_DIR = ROOT_DIR / "android" / "app"
ROOM_SCHEMA_DIR = ANDROID_APP_DIR / "schemas" / "com.phishdb.app.data.db.PhishDatabase"
ANDROID_DB_PATH = ANDROID_APP_DIR / "src" / "main" / "assets" / "databases" / "phish.db"

_NOT_NULL_FALLBACK = {"TEXT": "''", "INTEGER": "0", "REAL": "0"}


def _latest_schema() -> dict:
    files = sorted(ROOM_SCHEMA_DIR.glob("*.json"), key=lambda p: int(p.stem))
    if not files:
        raise FileNotFoundError(
            f"No Room schema found in {ROOM_SCHEMA_DIR}. Build the Android app once to generate it."
        )
    return json.loads(files[-1].read_text(encoding="utf-8"))["database"]


def build_android_db(source: Path = DB_PATH, output: Path = ANDROID_DB_PATH) -> dict:
    if not Path(source).exists():
        raise FileNotFoundError(f"Desktop database not found at {source}. Run a sync first.")

    schema = _latest_schema()
    output = Path(output)
    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = output.with_suffix(".tmp")
    tmp.unlink(missing_ok=True)

    conn = sqlite3.connect(tmp)
    counts: dict[str, int] = {}
    try:
        conn.execute("ATTACH DATABASE ? AS src", (str(source),))
        for entity in schema["entities"]:
            table = entity["tableName"]
            conn.execute(entity["createSql"].replace("${TABLE_NAME}", table))

            columns = [f["columnName"] for f in entity["fields"]]
            selects = [
                f"COALESCE(`{f['columnName']}`, {_NOT_NULL_FALLBACK[f['affinity']]})"
                if f.get("notNull")
                else f"`{f['columnName']}`"
                for f in entity["fields"]
            ]
            col_list = ", ".join(f"`{c}`" for c in columns)
            conn.execute(f"INSERT INTO `{table}` ({col_list}) SELECT {', '.join(selects)} FROM src.`{table}`")
            counts[table] = conn.execute(f"SELECT COUNT(*) FROM `{table}`").fetchone()[0]

            for index in entity.get("indices", []):
                conn.execute(index["createSql"].replace("${TABLE_NAME}", table))

        for query in schema.get("setupQueries", []):
            conn.execute(query)
        conn.execute(f"PRAGMA user_version = {schema['version']}")
        conn.commit()
        conn.execute("DETACH DATABASE src")
        conn.execute("VACUUM")
    finally:
        conn.close()

    tmp.replace(output)
    counts["size_mb"] = round(output.stat().st_size / 1_048_576, 1)
    return counts
