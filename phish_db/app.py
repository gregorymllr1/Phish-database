"""Web search interface."""

from __future__ import annotations

from urllib.parse import urlencode

from flask import Flask, abort, jsonify, render_template_string, request, send_file

from .config import ROOT_DIR
from .export import (
    default_export_path,
    export_csv,
    export_xlsx,
    fetch_export_rows,
    get_filter_options,
)
from .search import (
  filters_from_request_args,
  get_show,
  get_song_history,
  get_song_stats,
  get_stats,
  search,
)
from .sync import sync_all

app = Flask(__name__)

HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Phish Show Database</title>
  <style>
    :root {
      --bg: #0f1419;
      --surface: #1a2332;
      --border: #2d3a4f;
      --text: #e8edf4;
      --muted: #8b9cb3;
      --accent: #c45c26;
      --accent-hover: #e07030;
      --jam: #e05252;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: "Segoe UI", system-ui, sans-serif;
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
      line-height: 1.5;
    }
    header {
      background: linear-gradient(135deg, #1a2332 0%, #0f1419 100%);
      border-bottom: 1px solid var(--border);
      padding: 2rem 1.5rem 1.5rem;
    }
    h1 { font-size: 1.75rem; font-weight: 600; letter-spacing: -0.02em; }
    .tagline { color: var(--muted); margin-top: 0.25rem; font-size: 0.95rem; }
    .stats { margin-top: 0.75rem; font-size: 0.85rem; color: var(--muted); }
    main { max-width: 1100px; margin: 0 auto; padding: 1.5rem; }
    .search-panel {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 1.1rem;
      margin-bottom: 1.25rem;
    }
    .search-box {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 0.55rem;
      margin-bottom: 0.75rem;
    }
    .search-box .wide { grid-column: 1 / -1; }
    label { font-size: 0.75rem; color: var(--muted); display: block; margin-bottom: 0.2rem; }
    input, select, button {
      font: inherit; border-radius: 8px; border: 1px solid var(--border);
      width: 100%;
    }
    input, select {
      padding: 0.55rem 0.75rem;
      background: var(--bg);
      color: var(--text);
    }
    .actions {
      display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center;
    }
    button {
      padding: 0.6rem 1rem; background: var(--accent); color: #fff;
      border: none; cursor: pointer; font-weight: 500; width: auto;
    }
    button:hover { background: var(--accent-hover); }
    button.secondary { background: var(--bg); border: 1px solid var(--border); color: var(--text); }
    .results { display: grid; gap: 0.75rem; }
    .card {
      background: var(--surface); border: 1px solid var(--border);
      border-radius: 10px; padding: 1rem 1.1rem;
    }
    .card h3 { font-size: 1rem; font-weight: 600; }
    .card h3 a { color: var(--text); text-decoration: none; }
    .card h3 a:hover { color: var(--accent-hover); }
    .meta { color: var(--muted); font-size: 0.88rem; margin-top: 0.2rem; }
    .badge {
      display: inline-block; font-size: 0.7rem; text-transform: uppercase;
      letter-spacing: 0.04em; padding: 0.15rem 0.45rem; border-radius: 4px;
      background: var(--border); color: var(--muted); margin-right: 0.4rem;
    }
    .setlist { margin-top: 1rem; }
    .set-block { margin-bottom: 1rem; }
    .set-block h4 { color: var(--accent); font-size: 0.9rem; margin-bottom: 0.35rem; }
    .set-block ol { margin-left: 1.25rem; }
    .set-block li { margin-bottom: 0.2rem; }
    .jamchart { color: var(--jam); }
    .footnote { color: var(--muted); font-size: 0.85rem; }
    .empty { color: var(--muted); text-align: center; padding: 3rem 1rem; }
    .result-count { color: var(--muted); font-size: 0.9rem; margin-bottom: 0.75rem; }
    .song-stats { margin-bottom: 0.9rem; }
    .song-stats h3 { margin-bottom: 0.25rem; }
    .song-stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 0.5rem;
      margin-top: 0.8rem;
      margin-bottom: 0.8rem;
    }
    .stat-chip {
      background: var(--bg);
      border: 1px solid var(--border);
      border-radius: 8px;
      padding: 0.6rem 0.7rem;
    }
    .stat-chip .k { display: block; color: var(--muted); font-size: 0.74rem; }
    .stat-chip .v { display: block; color: var(--text); font-weight: 600; font-size: 1rem; }
    .stat-cols {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 0.75rem;
      margin-top: 0.8rem;
    }
    .stat-list { list-style: none; padding-left: 0; margin: 0.35rem 0 0; }
    .stat-list li {
      display: flex;
      justify-content: space-between;
      gap: 0.6rem;
      border-bottom: 1px dashed var(--border);
      padding: 0.3rem 0;
      font-size: 0.88rem;
    }
    .longest-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 0.5rem;
      font-size: 0.86rem;
    }
    .longest-table th, .longest-table td {
      border-bottom: 1px solid var(--border);
      text-align: left;
      padding: 0.35rem 0.4rem;
    }
    .longest-table th { color: var(--muted); font-weight: 600; }
    .bar-charts {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 1.25rem;
      margin-top: 1rem;
    }
    .bar-chart h4 { margin-bottom: 0.6rem; }
    .bar-row {
      display: grid;
      grid-template-columns: minmax(70px, 130px) 1fr auto;
      align-items: center;
      gap: 0.55rem;
      margin-bottom: 0.4rem;
    }
    .bar-label {
      font-size: 0.82rem;
      color: var(--muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .bar-track {
      background: var(--bg);
      border: 1px solid var(--border);
      border-radius: 6px;
      height: 22px;
      overflow: hidden;
    }
    .bar-fill {
      height: 100%;
      min-width: 3px;
      border-radius: 5px 0 0 5px;
      background-color: #3b4a9e;
      background-image: url("/assets/fishman-donuts.jpg");
      background-size: 44px 44px;
      background-repeat: repeat;
      box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.25);
      transition: width 0.4s ease;
    }
    .bar-value {
      font-size: 0.82rem;
      font-weight: 600;
      color: var(--text);
      min-width: 2ch;
      text-align: right;
    }
    footer {
      text-align: center; padding: 2rem; color: var(--muted); font-size: 0.8rem;
    }
    footer a { color: var(--accent); }
  </style>
</head>
<body>
  <header>
    <h1>Phish Show Database</h1>
    <p class="tagline">Searchable setlists, song lengths, venues, guests, and show notes</p>
    {% if stats %}
    <p class="stats">
      {{ stats.shows | default(0) }} shows ·
      {{ stats.performances | default(0) }} performances ·
      {{ stats.songs | default(0) }} songs ·
      {{ stats.date_range.first | default('?') }} – {{ stats.date_range.last | default('?') }}
    </p>
    {% endif %}
  </header>
  <main>
    <form class="search-panel" method="get" action="/">
      <div class="search-box">
        <div class="wide">
          <label for="q">Search</label>
          <input type="search" id="q" name="q" value="{{ f.query or '' }}" placeholder="Free text: guests, notes, anything...">
        </div>
        <div>
          <label for="song">Song</label>
          <input type="text" id="song" name="song" value="{{ f.song or '' }}" list="song-list" placeholder="e.g. Ghost">
        </div>
        <div>
          <label for="tour">Tour</label>
          <input type="text" id="tour" name="tour" value="{{ f.tour or '' }}" list="tour-list" placeholder="e.g. Summer">
        </div>
        <div>
          <label for="venue">Venue</label>
          <input type="text" id="venue" name="venue" value="{{ f.venue or '' }}" list="venue-list" placeholder="e.g. MSG">
        </div>
        <div>
          <label for="date">Date</label>
          <input type="date" id="date" name="date" value="{{ f.date or '' }}">
        </div>
        <div>
          <label for="date_from">From</label>
          <input type="date" id="date_from" name="date_from" value="{{ f.date_from or '' }}">
        </div>
        <div>
          <label for="date_to">To</label>
          <input type="date" id="date_to" name="date_to" value="{{ f.date_to or '' }}">
        </div>
        <div>
          <label for="year">Year</label>
          <input type="number" id="year" name="year" value="{{ f.year or '' }}" min="1983" max="2030" placeholder="1997">
        </div>
        <div>
          <label for="state">State</label>
          <input type="text" id="state" name="state" value="{{ f.state or '' }}" placeholder="NY">
        </div>
        <div>
          <label for="city">City</label>
          <input type="text" id="city" name="city" value="{{ f.city or '' }}" placeholder="NYC">
        </div>
        <div>
          <label for="type">Result type</label>
          <select id="type" name="type">
            <option value="">All types</option>
            <option value="show" {{ 'selected' if f.result_type == 'show' }}>Shows</option>
            <option value="song" {{ 'selected' if f.result_type == 'song' }}>Songs</option>
            <option value="song_performance" {{ 'selected' if f.result_type == 'song_performance' }}>Performances</option>
          </select>
        </div>
      </div>
      <datalist id="song-list">
        {% for s in options.songs[:200] %}<option value="{{ s }}">{% endfor %}
      </datalist>
      <datalist id="tour-list">
        {% for t in options.tours[:200] %}<option value="{{ t }}">{% endfor %}
      </datalist>
      <datalist id="venue-list">
        {% for v in options.venues[:200] %}<option value="{{ v }}">{% endfor %}
      </datalist>
      <div class="actions">
        <button type="submit">Search</button>
        <a href="/export?{{ export_query }}&format=csv&export_type=performances">
          <button type="button" class="secondary">Export CSV</button>
        </a>
        <a href="/export?{{ export_query }}&format=xlsx&export_type=performances">
          <button type="button" class="secondary">Export Excel</button>
        </a>
      </div>
    </form>

    {% if show_detail %}
    <div class="card setlist">
      <h3>{{ show_detail.show.showdate }} — {{ show_detail.show.venue }}</h3>
      <p class="meta">{{ show_detail.show.city }}, {{ show_detail.show.state }}, {{ show_detail.show.country }}</p>
      {% if show_detail.show.tour_name %}<p class="meta">Tour: {{ show_detail.show.tour_name }}</p>{% endif %}
      {% if show_detail.show.soundcheck %}<p class="meta">Soundcheck: {{ show_detail.show.soundcheck }}</p>{% endif %}
      {% if show_detail.show.setlist_notes %}<p class="meta">Notes: {{ show_detail.show.setlist_notes }}</p>{% endif %}
      {% for block in show_detail.sets %}
      <div class="set-block">
        <h4>{{ block.label }}</h4>
        <ol>
          {% for song in block.songs %}
          <li class="{{ 'jamchart' if song.is_jamchart }}">
            {{ song.song }}
            {% if song.tracktime %}<span class="footnote">({{ song.tracktime }})</span>{% endif %}
            {% if song.transition %}{{ song.transition }}{% endif %}
            {% if song.footnote %}<span class="footnote"> — {{ song.footnote }}</span>{% endif %}
          </li>
          {% endfor %}
        </ol>
      </div>
      {% endfor %}
      <p style="margin-top:1rem"><a href="/">← Back to search</a></p>
    </div>
    {% elif has_search %}
      {% if song_stats and song_stats.summary %}
      <div class="card song-stats">
        <h3>Song Analytics: {{ song_stats.summary.song or song_stats.query }}</h3>
        <p class="meta">
          {% if f.venue or f.city or f.state or f.year or f.date_from or f.date_to %}
          Filtered by:
          {% if f.venue %} venue contains "{{ f.venue }}"{% endif %}
          {% if f.city %} city contains "{{ f.city }}"{% endif %}
          {% if f.state %} state "{{ f.state }}"{% endif %}
          {% if f.year %} year {{ f.year }}{% endif %}
          {% if f.date_from %} from {{ f.date_from }}{% endif %}
          {% if f.date_to %} to {{ f.date_to }}{% endif %}
          {% else %}
          Across all available shows
          {% endif %}
        </p>

        <div class="song-stats-grid">
          <div class="stat-chip"><span class="k">Performances</span><span class="v">{{ song_stats.summary.performances }}</span></div>
          <div class="stat-chip"><span class="k">Unique Shows</span><span class="v">{{ song_stats.summary.shows }}</span></div>
          <div class="stat-chip"><span class="k">Venues</span><span class="v">{{ song_stats.summary.venues }}</span></div>
          <div class="stat-chip"><span class="k">Locations</span><span class="v">{{ song_stats.summary.locations }}</span></div>
          <div class="stat-chip"><span class="k">First Played</span><span class="v">{{ song_stats.summary.first_played or '-' }}</span></div>
          <div class="stat-chip"><span class="k">Last Played</span><span class="v">{{ song_stats.summary.last_played or '-' }}</span></div>
          <div class="stat-chip"><span class="k">Shortest (known)</span><span class="v">{{ song_stats.summary.min_length or '-' }}</span></div>
          <div class="stat-chip"><span class="k">Average (known)</span><span class="v">{{ song_stats.summary.avg_length or '-' }}</span></div>
          <div class="stat-chip"><span class="k">Longest (known)</span><span class="v">{{ song_stats.summary.max_length or '-' }}</span></div>
        </div>

        <div class="stat-cols">
          <div>
            <h4>Top Venues</h4>
            <ul class="stat-list">
              {% for item in song_stats.top_venues %}
              <li><span>{{ item.venue or 'Unknown venue' }}</span><strong>{{ item.performances }}</strong></li>
              {% endfor %}
            </ul>
          </div>
          <div>
            <h4>Top Locations</h4>
            <ul class="stat-list">
              {% for item in song_stats.top_locations %}
              <li>
                <span>{{ item.city or '?' }}, {{ item.state or '?' }}{% if item.country %} ({{ item.country }}){% endif %}</span>
                <strong>{{ item.performances }}</strong>
              </li>
              {% endfor %}
            </ul>
          </div>
          <div>
            <h4>By Year</h4>
            <ul class="stat-list">
              {% for item in song_stats.yearly %}
              <li><span>{{ item.year }}</span><strong>{{ item.performances }}</strong></li>
              {% endfor %}
            </ul>
          </div>
        </div>

        <div class="bar-charts">
          {% if song_stats.yearly %}
          {% set year_max = song_stats.yearly | map(attribute='performances') | max %}
          <div class="bar-chart">
            <h4>Plays by Year</h4>
            {% for item in song_stats.yearly %}
            <div class="bar-row">
              <span class="bar-label">{{ item.year }}</span>
              <div class="bar-track">
                <div class="bar-fill" style="width: {{ (item.performances / year_max * 100) | round(1) }}%"></div>
              </div>
              <span class="bar-value">{{ item.performances }}</span>
            </div>
            {% endfor %}
          </div>
          {% endif %}

          {% if song_stats.top_venues %}
          {% set venue_max = song_stats.top_venues | map(attribute='performances') | max %}
          <div class="bar-chart">
            <h4>Top Venues</h4>
            {% for item in song_stats.top_venues %}
            <div class="bar-row">
              <span class="bar-label" title="{{ item.venue or 'Unknown venue' }}">{{ item.venue or 'Unknown venue' }}</span>
              <div class="bar-track">
                <div class="bar-fill" style="width: {{ (item.performances / venue_max * 100) | round(1) }}%"></div>
              </div>
              <span class="bar-value">{{ item.performances }}</span>
            </div>
            {% endfor %}
          </div>
          {% endif %}

          {% if song_stats.top_locations %}
          {% set loc_max = song_stats.top_locations | map(attribute='performances') | max %}
          <div class="bar-chart">
            <h4>Top Locations</h4>
            {% for item in song_stats.top_locations %}
            <div class="bar-row">
              <span class="bar-label" title="{{ item.city or '?' }}, {{ item.state or '?' }}">{{ item.city or '?' }}, {{ item.state or '?' }}</span>
              <div class="bar-track">
                <div class="bar-fill" style="width: {{ (item.performances / loc_max * 100) | round(1) }}%"></div>
              </div>
              <span class="bar-value">{{ item.performances }}</span>
            </div>
            {% endfor %}
          </div>
          {% endif %}
        </div>

        {% if song_stats.longest %}
        <h4 style="margin-top:0.9rem;">Longest Recorded Versions</h4>
        <table class="longest-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Venue</th>
              <th>Location</th>
              <th>Length</th>
            </tr>
          </thead>
          <tbody>
            {% for perf in song_stats.longest %}
            <tr>
              <td><a href="/?date={{ perf.showdate }}">{{ perf.showdate }}</a></td>
              <td>{{ perf.venue }}</td>
              <td>{{ perf.city }}, {{ perf.state }}</td>
              <td>{{ perf.tracktime or '-' }}</td>
            </tr>
            {% endfor %}
          </tbody>
        </table>
        {% endif %}
      </div>
      {% endif %}

      {% if results %}
      <p class="result-count">{{ results|length }} result{{ 's' if results|length != 1 else '' }}</p>
      <div class="results">
        {% for r in results %}
        <div class="card">
          <span class="badge">{{ r.result_type }}</span>
          <h3>
            {% if r.showdate %}
            <a href="/?date={{ r.showdate }}">{{ r.title }}</a>
            {% elif r.result_type == 'song' %}
            <a href="/?song={{ r.title }}">{{ r.title }}</a>
            {% else %}
            {{ r.title }}
            {% endif %}
          </h3>
          {% if r.subtitle %}<p class="meta">{{ r.subtitle }}</p>{% endif %}
        </div>
        {% endfor %}
      </div>
      {% else %}
      <p class="empty">No results match your filters.</p>
      {% endif %}
    {% else %}
    <p class="empty">Use the filters above to search {{ stats.shows | default(0) }} shows of Phish history.<br>
    Try song <em>Ghost</em>, tour <em>Summer</em>, venue <em>Madison Square Garden</em>, or date range 1997-01-01 to 1997-12-31.</p>
    {% endif %}
  </main>
  <footer>Data from <a href="https://phish.net">Phish.net</a> · Mockingbird Foundation</footer>
</body>
</html>
"""


def _search_from_filters(f):
    return search(
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


@app.route("/")
def index():
    showdate = request.args.get("date", "").strip()
    f = filters_from_request_args(request.args)
    f.limit = 100

    stats = get_stats()
    options = get_filter_options()
    show_detail = get_show(showdate) if showdate else None
    results = []
    song_stats = None
    has_search = f.has_any_criteria() and not showdate

    if has_search:
        results = _search_from_filters(f)
        if f.song:
            song_stats = get_song_stats(f.song, filters=f)

    export_params = {
        k: v
        for k, v in request.args.items()
        if k in {
            "q", "song", "tour", "venue", "date", "date_from", "date_to",
            "year", "state", "city", "type",
        }
        and v
    }
    export_query = urlencode(export_params)

    return render_template_string(
        HTML,
        f=f,
        options=options,
        results=results,
        song_stats=song_stats,
        has_search=has_search,
        show_detail=show_detail,
        stats=stats,
        export_query=export_query,
    )


@app.route("/export")
def export_download():
    fmt = request.args.get("format", "csv")
    export_type = request.args.get("export_type", "performances")
    f = filters_from_request_args(request.args)
    f.limit = request.args.get("export_limit", 10000, type=int)

    if fmt == "xlsx":
        path = export_xlsx(export_type, f, default_export_path(export_type, "xlsx"))
        return send_file(
            path,
            as_attachment=True,
            download_name=path.name,
            mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        )

    path = export_csv(export_type, f, default_export_path(export_type, "csv"))
    return send_file(path, as_attachment=True, download_name=path.name, mimetype="text/csv")


@app.route("/api/search")
def api_search():
    f = filters_from_request_args(request.args)
    return jsonify(
        [
            {
                "type": r.result_type,
                "title": r.title,
                "subtitle": r.subtitle,
                "showdate": r.showdate,
                "entity_id": r.entity_id,
            }
            for r in _search_from_filters(f)
        ]
    )


@app.route("/api/show/<showdate>")
def api_show(showdate: str):
    data = get_show(showdate)
    if not data:
        return jsonify({"error": "not found"}), 404
    return jsonify(data)


@app.route("/api/song/<path:name>")
def api_song(name: str):
    f = filters_from_request_args(request.args)
    f.song = f.song or name
    return jsonify(get_song_history(name, limit=f.limit, filters=f))


@app.route("/api/song_stats/<path:name>")
def api_song_stats(name: str):
    f = filters_from_request_args(request.args)
    f.song = f.song or name
    return jsonify(get_song_stats(name, filters=f))


@app.route("/api/stats")
def api_stats():
    return jsonify(get_stats())


@app.route("/api/filters")
def api_filters():
    return jsonify(get_filter_options())


@app.route("/api/export/<export_type>")
def api_export(export_type: str):
    fmt = request.args.get("format", "json")
    f = filters_from_request_args(request.args)
    f.limit = request.args.get("limit", 10000, type=int)

    rows = fetch_export_rows(export_type, f)
    if fmt == "json":
        return jsonify(rows)
    if fmt == "csv":
        path = export_csv(export_type, f, default_export_path(export_type, "csv"))
        return send_file(path, as_attachment=True, download_name=path.name)
    path = export_xlsx(export_type, f, default_export_path(export_type, "xlsx"))
    return send_file(
        path,
        as_attachment=True,
        download_name=path.name,
        mimetype="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    )


@app.route("/api/sync", methods=["POST"])
def api_sync():
    try:
        stats = sync_all(progress_callback=lambda m: None)
        return jsonify({"ok": True, "stats": stats})
    except Exception as exc:
        return jsonify({"ok": False, "error": str(exc)}), 400


@app.route("/assets/fishman-donuts.jpg")
def fishman_donuts():
    path = ROOT_DIR / "fishman donuts.jpg"
    if not path.exists():
        abort(404)
    return send_file(path, mimetype="image/jpeg")


def run(host: str = "127.0.0.1", port: int = 5000, debug: bool = False) -> None:
    app.run(host=host, port=port, debug=debug)