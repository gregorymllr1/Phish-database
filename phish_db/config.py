import os
from pathlib import Path

try:
    from dotenv import load_dotenv

    load_dotenv()
except ModuleNotFoundError:
    pass

ROOT_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT_DIR / "data"
DB_PATH = DATA_DIR / "phish.db"

API_BASE_URL = "https://api.phish.net/v5"
API_KEY = os.environ.get("PHISH_API_KEY", "")

FIRST_SHOW_YEAR = 1983