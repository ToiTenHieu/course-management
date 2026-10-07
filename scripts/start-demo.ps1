param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$taskRunDir = Join-Path $taskRoot 'target/local-demo'
New-Item -ItemType Directory -Path $taskRunDir -Force | Out-Null
$taskPidFile = Join-Path $taskRunDir 'server.pid'
if (Test-Path -LiteralPath $taskPidFile) {
    $taskPreviousPid = [int](Get-Content -LiteralPath $taskPidFile)
    $taskPrevious = Get-CimInstance Win32_Process -Filter "ProcessId=$taskPreviousPid"
    if ($taskPrevious -and $taskPrevious.CommandLine -like '*course_management-0.0.1-SNAPSHOT.jar*') {
        Write-Host "Demo is already running: http://127.0.0.1:8080"
        exit 0
    }
}
if (-not $SkipBuild) {
    & (Join-Path $taskRoot 'mvnw.cmd') -B -ntp -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
}
$taskJar = Join-Path $taskRoot 'target/course_management-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Build the project first' }
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java).Source }
$taskArguments = @('-jar', ('"' + $taskJar + '"'), '--spring.profiles.active=demo')
$taskLocalConfig = Join-Path $taskRunDir 'application-local.properties'
if (Test-Path -LiteralPath $taskLocalConfig) {
    $taskConfigUrl = ([uri]$taskLocalConfig).AbsoluteUri
    $taskArguments += ('--spring.config.additional-location=' + $taskConfigUrl)
} elseif (-not $env:DB_PASSWORD) {
    throw 'Set DB_PASSWORD before starting. See README.md.'
}
$taskProcess = Start-Process -FilePath $taskJava -ArgumentList $taskArguments -WorkingDirectory $taskRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $taskRunDir 'server.log') -RedirectStandardError (Join-Path $taskRunDir 'server-error.log')
$taskProcess.Id | Set-Content -LiteralPath $taskPidFile
Write-Host "Demo started (PID $($taskProcess.Id)): http://127.0.0.1:8080"
Write-Host "Logs: $taskRunDir/server.log"
