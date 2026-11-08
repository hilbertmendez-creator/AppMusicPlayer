# Keeps the Mi Musica backend running on 127.0.0.1:8000 (exposed via Tailscale Funnel).
# Restarts uvicorn if it exits and appends its output to data\server.log.
# Registered as a Windows logon task; see scripts\install-autostart.ps1.

$ErrorActionPreference = "Continue"
$backend = Split-Path -Parent $PSScriptRoot
Set-Location $backend
New-Item -ItemType Directory -Force -Path "$backend\data" | Out-Null
$log = "$backend\data\server.log"
$uvicorn = "$backend\.venv\Scripts\uvicorn.exe"

while ($true) {
    Add-Content -Path $log -Value "[$(Get-Date -Format s)] starting uvicorn"
    # cmd.exe appends uvicorn's raw (UTF-8) output; PowerShell 5 redirection would write UTF-16.
    cmd.exe /c "`"$uvicorn`" app.main:app --app-dir src --host 127.0.0.1 --port 8000 >> `"$log`" 2>&1"
    Add-Content -Path $log -Value "[$(Get-Date -Format s)] uvicorn exited with code $LASTEXITCODE; restarting in 5s"
    Start-Sleep -Seconds 5
}
