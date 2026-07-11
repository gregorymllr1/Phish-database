@echo off
setlocal
set "ROOT=%~dp0"
set "PY=%ROOT%.venv\Scripts\python.exe"
if not exist "%PY%" (
  echo Virtual environment not found.
  echo Run: py -3.13 -m venv .venv
  echo Then: .venv\Scripts\python.exe -m pip install -r requirements.txt
  exit /b 1
)
"%PY%" "%ROOT%main.py" %*
exit /b %ERRORLEVEL%