$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskPidFile = Join-Path $taskRoot 'target/local-demo/server.pid'
if (-not (Test-Path -LiteralPath $taskPidFile)) { Write-Host 'No demo PID recorded'; return }
$taskDemoPid = 0
if (-not [int]::TryParse((Get-Content -LiteralPath $taskPidFile -Raw).Trim(), [ref]$taskDemoPid) -or $taskDemoPid -le 0) {
    Remove-Item -LiteralPath $taskPidFile
    Write-Host 'Removed an invalid demo PID record'
    return
}
$taskProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$taskDemoPid"
$taskJar = Join-Path $taskRoot 'target/course_management-0.0.1-SNAPSHOT.jar'
if ($taskProcess -and $taskProcess.CommandLine -and $taskProcess.CommandLine.IndexOf($taskJar, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
    Stop-Process -Id $taskDemoPid
    Write-Host "Stopped demo PID $taskDemoPid"
} else { Write-Host 'The recorded demo process is no longer running' }
Remove-Item -LiteralPath $taskPidFile
