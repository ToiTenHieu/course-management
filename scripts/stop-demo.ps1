$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskPidFile = Join-Path $taskRoot 'target/local-demo/server.pid'
if (-not (Test-Path -LiteralPath $taskPidFile)) { Write-Host 'No demo PID recorded'; exit 0 }
$taskDemoPid = [int](Get-Content -LiteralPath $taskPidFile)
$taskProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$taskDemoPid"
if ($taskProcess -and $taskProcess.CommandLine -like '*course_management-0.0.1-SNAPSHOT.jar*') {
    Stop-Process -Id $taskDemoPid
    Write-Host "Stopped demo PID $taskDemoPid"
} else { Write-Host 'The recorded demo process is no longer running' }
