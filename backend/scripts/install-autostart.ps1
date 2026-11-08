# Registers (or replaces) a per-user Windows task that starts the Mi Musica backend at logon.
# Run once from PowerShell:  powershell -ExecutionPolicy Bypass -File backend\scripts\install-autostart.ps1
# Remove with:               Unregister-ScheduledTask -TaskName "MiMusica Server" -Confirm:$false

$script = Join-Path $PSScriptRoot "run-server.ps1"
$action = New-ScheduledTaskAction -Execute "powershell.exe" `
    -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$script`""
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName "MiMusica Server" -Action $action -Trigger $trigger `
    -Settings $settings -Description "Mi Musica backend (uvicorn on 127.0.0.1:8000)" -Force | Out-Null
Write-Output "Task 'MiMusica Server' registered."
