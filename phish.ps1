# Run phish database commands with the project virtual environment.
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Python = Join-Path $ProjectRoot ".venv\Scripts\python.exe"

if (-not (Test-Path $Python)) {
    Write-Error "Virtual environment not found. Run: py -3.13 -m venv .venv; .\.venv\Scripts\python.exe -m pip install -r requirements.txt"
    exit 1
}

& $Python (Join-Path $ProjectRoot "main.py") @args
exit $LASTEXITCODE