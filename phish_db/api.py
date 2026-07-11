"""Phish.net API v5 client."""

from __future__ import annotations

import time
from typing import Any
from urllib.parse import urlencode

import requests

from .config import API_BASE_URL, API_KEY


class PhishAPIError(Exception):
    pass


class PhishAPIClient:
    def __init__(self, api_key: str | None = None, delay: float = 0.35):
        self.api_key = api_key or API_KEY
        self.delay = delay
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": "phish-database/1.0"})

    def _request(self, path: str, params: dict[str, Any] | None = None) -> list[dict]:
        if not self.api_key:
            raise PhishAPIError(
                "PHISH_API_KEY is not set. Request a free key at https://phish.net/api/keys "
                "and add it to your .env file."
            )

        query = {"apikey": self.api_key}
        if params:
            query.update({k: v for k, v in params.items() if v is not None})

        url = f"{API_BASE_URL}/{path}"
        if query:
            url = f"{url}?{urlencode(query)}"

        time.sleep(self.delay)
        response = self.session.get(url, timeout=120)
        response.raise_for_status()
        payload = response.json()

        if payload.get("error"):
            raise PhishAPIError(
                f"API error {payload.get('error')}: {payload.get('error_message', 'unknown')}"
            )

        data = payload.get("data", [])
        return data if isinstance(data, list) else [data]

    def get_shows(self, **params: Any) -> list[dict]:
        return self._request("shows/artist/phish.json", params)

    def get_setlists_by_year(self, year: int, **params: Any) -> list[dict]:
        return self._request(f"setlists/showyear/{year}.json", params)

    def get_songs(self, **params: Any) -> list[dict]:
        return self._request("songs.json", params)

    def get_venues(self, **params: Any) -> list[dict]:
        return self._request("venues.json", params)

    def get_show_by_date(self, showdate: str) -> list[dict]:
        return self._request(f"shows/showdate/{showdate}.json")

    def get_setlist_by_date(self, showdate: str) -> list[dict]:
        return self._request(f"setlists/showdate/{showdate}.json")