# Weekly scheduled sync: refreshes venues, songs, shows, and setlists for last year + this year.
# Output is appended to data\sync.log.
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$Python = Join-Path $ProjectRoot ".venv\Scripts\python.exe"
$LogDir = Join-Path $ProjectRoot "data"
$Log = Join-Path $LogDir "sync.log"

Set-Location $ProjectRoot
New-Item -ItemType Directory -Force $LogDir | Out-Null

$StartYear = (Get-Date).Year - 1
"===== $(Get-Date -Format s) scheduled sync (from $StartYear) =====" | Out-File -Append -Encoding utf8 $Log

& $Python (Join-Path $ProjectRoot "main.py") sync --start-year $StartYear *>&1 | Out-File -Append -Encoding utf8 $Log
$Code = $LASTEXITCODE

"===== finished with exit code $Code =====" | Out-File -Append -Encoding utf8 $Log
exit $Code
