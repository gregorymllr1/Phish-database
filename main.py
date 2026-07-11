#!/usr/bin/env python3
"""Entry point: python main.py [command] or python main.py web"""

from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VENV_PYTHON = ROOT / ".venv" / "Scripts" / "python.exe"


def _running_in_venv() -> bool:
    current = Path(sys.executable).resolve()
    try:
        return current == VENV_PYTHON.resolve()
    except OSError:
        return False


def _relaunch_with_venv() -> None:
    if not VENV_PYTHON.exists():
        return
    if _running_in_venv():
        return
    env = os.environ.copy()
    env.pop("PYTHONHOME", None)
    env.pop("PYTHONPATH", None)
    result = subprocess.run([str(VENV_PYTHON), str(Path(__file__).resolve()), *sys.argv[1:]], env=env)
    raise SystemExit(result.returncode)


_relaunch_with_venv()


def _missing_dependency_help(error: ModuleNotFoundError) -> None:
    print(f"Missing dependency: {error.name}", file=sys.stderr)
    print(file=sys.stderr)
    if not VENV_PYTHON.exists():
        print("Set up the project environment:", file=sys.stderr)
        print("  py -3.13 -m venv .venv", file=sys.stderr)
        print("  .\\.venv\\Scripts\\python.exe -m pip install -r requirements.txt", file=sys.stderr)
    else:
        print("Try:", file=sys.stderr)
        print("  .\\phish.ps1 sync --year 1997", file=sys.stderr)
        print("  .\\.venv\\Scripts\\python.exe main.py sync --year 1997", file=sys.stderr)


try:
    from phish_db.cli import main as cli_main
except ModuleNotFoundError as exc:
    _missing_dependency_help(exc)
    raise SystemExit(1) from exc


def main() -> int:
    if len(sys.argv) > 1 and sys.argv[1] == "web":
        from phish_db.app import run

        port = 5000
        if len(sys.argv) > 2:
            port = int(sys.argv[2])
        run(port=port)
        return 0
    return cli_main()


if __name__ == "__main__":
    raise SystemExit(main())