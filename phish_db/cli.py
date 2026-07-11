"""Command-line interface for sync and search."""

from __future__ import annotations

import argparse
import json
import sys

from .config import DB_PATH, FIRST_SHOW_YEAR
from .export import default_export_path, export_csv, export_xlsx
from .filters import SearchFilters
from .schema import get_meta, init_db
from .search import get_show, get_song_history, get_stats, search
from .sync import sync_all, sync_year


def _add_filter_args(parser: argparse.ArgumentParser, *, include_limit: bool = True, limit_default: int = 50) -> None:
    parser.add_argument("query", nargs="?", default="", help="Free-text search")
    parser.add_argument("--type", choices=["show", "song", "song_performance"], dest="result_type")
    parser.add_argument("--song", help="Filter by song name or slug")
    parser.add_argument("--tour", help="Filter by tour name")
    parser.add_argument("--venue", help="Filter by venue name")
    parser.add_argument("--date", help="Exact show date (YYYY-MM-DD)")
    parser.add_argument("--date-from", dest="date_from", help="Start of date range (YYYY-MM-DD)")
    parser.add_argument("--date-to", dest="date_to", help="End of date range (YYYY-MM-DD)")
    parser.add_argument("--year", type=int, help="Filter by year")
    parser.add_argument("--state", help="Filter by state")
    parser.add_argument("--city", help="Filter by city")
    if include_limit:
        parser.add_argument("--limit", type=int, default=limit_default)


def _filters_from_args(args: argparse.Namespace) -> SearchFilters:
    return SearchFilters(
        query=args.query or None,
        result_type=getattr(args, "result_type", None),
        song=getattr(args, "song", None),
        tour=getattr(args, "tour", None),
        venue=getattr(args, "venue", None),
        date=getattr(args, "date", None),
        date_from=getattr(args, "date_from", None),
        date_to=getattr(args, "date_to", None),
        year=getattr(args, "year", None),
        state=getattr(args, "state", None),
        city=getattr(args, "city", None),
        limit=getattr(args, "limit", 50),
    )


def cmd_sync(args: argparse.Namespace) -> int:
    from .api import PhishAPIError

    def progress(msg: str) -> None:
        print(msg)

    try:
        if args.year:
            count = sync_year(args.year)
            print(f"Synced {count} setlist entries for {args.year}.")
        else:
            stats = sync_all(
                start_year=args.start_year,
                end_year=args.end_year,
                progress_callback=progress,
            )
            print(json.dumps(stats, indent=2))
        return 0
    except PhishAPIError as exc:
        print(f"Error: {exc}", file=sys.stderr)
        return 1


def cmd_search(args: argparse.Namespace) -> int:
    f = _filters_from_args(args)
    results = search(
        query=f.query or "",
        result_type=f.result_type,
        song=f.song,
        tour=f.tour,
        venue=f.venue,
        date=f.date,
        date_from=f.date_from,
        date_to=f.date_to,
        year=f.year,
        state=f.state,
        city=f.city,
        limit=f.limit,
    )
    if args.json:
        print(json.dumps([r.__dict__ for r in results], indent=2, default=str))
        return 0

    if not results:
        print("No results found.")
        return 0

    for i, result in enumerate(results, 1):
        print(f"{i}. [{result.result_type}] {result.title}")
        if result.subtitle:
            print(f"   {result.subtitle}")
    return 0


def cmd_export(args: argparse.Namespace) -> int:
    f = _filters_from_args(args)
    f.limit = args.limit

    output = args.output
    if not output:
        output = default_export_path(args.export_type, args.format)

    try:
        if args.format == "csv":
            path = export_csv(args.export_type, f, output)
        else:
            path = export_xlsx(args.export_type, f, output)
        print(f"Exported {args.export_type} to {path}")
        return 0
    except Exception as exc:
        print(f"Export failed: {exc}", file=sys.stderr)
        return 1


def cmd_show(args: argparse.Namespace) -> int:
    data = get_show(args.date)
    if not data:
        print(f"No show found for {args.date}.")
        return 1

    show = data["show"]
    print(f"{show['showdate']} — {show['venue']}")
    print(f"{show['city']}, {show['state']}, {show['country']}")
    if show.get("tour_name"):
        print(f"Tour: {show['tour_name']}")
    if show.get("soundcheck"):
        print(f"Soundcheck: {show['soundcheck']}")
    if show.get("setlist_notes"):
        print(f"Notes: {show['setlist_notes']}")
    print()

    for set_block in data["sets"]:
        print(f"** {set_block['label']} **")
        for song in set_block["songs"]:
            line = f"  {song['position'] or '-'}. {song['song']}"
            if song.get("tracktime"):
                line += f" ({song['tracktime']})"
            if song.get("transition"):
                line += f" {song['transition']}"
            if song.get("footnote"):
                line += f" [{song['footnote']}]"
            if song.get("is_jamchart"):
                line += " *jamchart*"
            print(line)
        print()
    return 0


def cmd_song(args: argparse.Namespace) -> int:
    f = SearchFilters(song=args.name, limit=args.limit)
    if args.tour:
        f.tour = args.tour
    if args.venue:
        f.venue = args.venue
    if args.date_from:
        f.date_from = args.date_from
    if args.date_to:
        f.date_to = args.date_to
    if args.year:
        f.year = args.year

    history = get_song_history(args.name, limit=args.limit, filters=f)
    if not history:
        print(f"No performances found for '{args.name}'.")
        return 1

    print(f"Performances of '{history[0]['song']}' ({len(history)} shown):\n")
    for row in history:
        extras = []
        if row.get("tracktime"):
            extras.append(row["tracktime"])
        if row.get("footnote"):
            extras.append(row["footnote"])
        extra = f" — {'; '.join(extras)}" if extras else ""
        print(
            f"{row['showdate']}  {row['venue']}, {row['city']}, {row['state']}  "
            f"Set {row['set_name']} #{row['position']}{extra}"
        )
    return 0


def cmd_stats(_: argparse.Namespace) -> int:
    stats = get_stats()
    dr = stats["date_range"]
    print("Phish Database Stats")
    print(f"  Shows:        {stats['shows']:,}")
    print(f"  Songs:        {stats['songs']:,}")
    print(f"  Performances: {stats['performances']:,}")
    print(f"  Venues:       {stats['venues']:,}")
    if dr and dr.get("first"):
        print(f"  Date range:   {dr['first']} to {dr['last']}")

    conn = init_db()
    last_sync = get_meta(conn, "last_sync")
    conn.close()
    if last_sync:
        print(f"  Last sync:    {last_sync}")
    return 0


def cmd_init(_: argparse.Namespace) -> int:
    init_db()
    print(f"Database initialized at {DB_PATH}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Searchable Phish.net show database",
    )
    sub = parser.add_subparsers(dest="command", required=True)

    init_cmd = sub.add_parser("init", help="Create the local database schema")
    init_cmd.set_defaults(func=cmd_init)

    sync_cmd = sub.add_parser("sync", help="Download data from Phish.net API")
    sync_cmd.add_argument("--year", type=int, help="Sync a single year of setlists")
    sync_cmd.add_argument("--start-year", type=int, default=FIRST_SHOW_YEAR)
    sync_cmd.add_argument("--end-year", type=int)
    sync_cmd.set_defaults(func=cmd_sync)

    search_cmd = sub.add_parser("search", help="Search with filters")
    _add_filter_args(search_cmd)
    search_cmd.add_argument("--json", action="store_true")
    search_cmd.set_defaults(func=cmd_search)

    export_cmd = sub.add_parser("export", help="Export filtered data to CSV or Excel")
    export_cmd.add_argument(
        "export_type",
        choices=["performances", "shows", "songs"],
        help="What to export",
    )
    _add_filter_args(export_cmd, include_limit=True, limit_default=10000)
    export_cmd.add_argument(
        "--format",
        choices=["csv", "xlsx"],
        default="csv",
        help="Output format (default: csv)",
    )
    export_cmd.add_argument("-o", "--output", help="Output file path")
    export_cmd.set_defaults(func=cmd_export)

    show_cmd = sub.add_parser("show", help="Display a full setlist by date")
    show_cmd.add_argument("date", help="Show date YYYY-MM-DD")
    show_cmd.set_defaults(func=cmd_show)

    song_cmd = sub.add_parser("song", help="Show performance history for a song")
    song_cmd.add_argument("name", help="Song name or slug fragment")
    song_cmd.add_argument("--tour")
    song_cmd.add_argument("--venue")
    song_cmd.add_argument("--date-from", dest="date_from")
    song_cmd.add_argument("--date-to", dest="date_to")
    song_cmd.add_argument("--year", type=int)
    song_cmd.add_argument("--limit", type=int, default=50)
    song_cmd.set_defaults(func=cmd_song)

    stats_cmd = sub.add_parser("stats", help="Database statistics")
    stats_cmd.set_defaults(func=cmd_stats)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())