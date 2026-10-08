param(
    [switch]$SkipBuild,
    [ValidateRange(1, 65535)][int]$Port = 8080,
    [ValidateRange(5, 300)][int]$StartupTimeoutSeconds = 90
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $taskRoot
$taskRunDir = Join-Path $taskRoot 'target/local-demo'
New-Item -ItemType Directory -Path $taskRunDir -Force | Out-Null
$taskPidFile = Join-Path $taskRunDir 'server.pid'
$taskLocalConfig = Join-Path $taskRunDir 'application-local.properties'
$taskJar = Join-Path $taskRoot 'target/course_management-0.0.1-SNAPSHOT.jar'
if (-not $PSBoundParameters.ContainsKey('Port')) {
    if ($env:PORT) { $Port = [int]$env:PORT }
    elseif (Test-Path -LiteralPath $taskLocalConfig) {
        $taskPortLine = Get-Content -LiteralPath $taskLocalConfig | Where-Object { $_ -match '^\s*server\.port\s*=\s*\d+\s*$' } | Select-Object -Last 1
        if ($taskPortLine) { $Port = [int]($taskPortLine -replace '^.*=\s*', '') }
    }
}
if ($Port -lt 1 -or $Port -gt 65535) { throw 'PORT must be between 1 and 65535.' }
$taskUrl = "http://127.0.0.1:$Port"
function Test-DemoReady {
    try {
        $taskResponse = Invoke-RestMethod "$taskUrl/api/auth/config" -TimeoutSec 2
        return $taskResponse.success -and $taskResponse.data.demo -eq $true
    } catch { return $false }
}
if (Test-Path -LiteralPath $taskPidFile) {
    $taskPreviousPid = 0
    if ([int]::TryParse((Get-Content -LiteralPath $taskPidFile -Raw).Trim(), [ref]$taskPreviousPid) -and $taskPreviousPid -gt 0) {
        $taskPrevious = Get-CimInstance Win32_Process -Filter "ProcessId=$taskPreviousPid"
        if ($taskPrevious -and $taskPrevious.CommandLine -and $taskPrevious.CommandLine.IndexOf($taskJar, [StringComparison]::OrdinalIgnoreCase) -ge 0) {
            if (Test-DemoReady) { Write-Host "Demo is already ready: $taskUrl"; return }
            throw 'The recorded demo process is still starting or uses another port. Check its logs, or stop it before restarting.'
        }
    }
    Remove-Item -LiteralPath $taskPidFile
}
$taskClient = New-Object System.Net.Sockets.TcpClient
try {
    $taskConnect = $taskClient.ConnectAsync('127.0.0.1', $Port)
    try { $null = $taskConnect.Wait(500) } catch { }
    if ($taskClient.Connected) { throw "Port $Port is already in use. Choose -Port or stop that application's server." }
} finally { $taskClient.Dispose() }
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath $taskJava)) { throw 'JAVA_HOME does not point to a working JDK. See README.md.' }
if (-not (Test-Path -LiteralPath $taskLocalConfig) -and -not $env:DB_PASSWORD) {
    throw 'Set DB_PASSWORD before starting. See README.md.'
}
if (-not $SkipBuild) {
    & (Join-Path $taskRoot 'mvnw.cmd') -B -ntp -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed' }
}
if (-not (Test-Path -LiteralPath $taskJar)) { throw 'Build the project first' }
$taskArguments = @('-jar', ('"' + $taskJar + '"'), '--spring.profiles.active=demo', "--server.port=$Port")
if (Test-Path -LiteralPath $taskLocalConfig) {
    $taskConfigUrl = ([uri]$taskLocalConfig).AbsoluteUri
    $taskArguments += ('--spring.config.additional-location=' + $taskConfigUrl)
}
$taskProcess = Start-Process -FilePath $taskJava -ArgumentList $taskArguments -WorkingDirectory $taskRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $taskRunDir 'server.log') -RedirectStandardError (Join-Path $taskRunDir 'server-error.log')
$taskProcess.Id | Set-Content -LiteralPath $taskPidFile
Write-Host "Starting demo (PID $($taskProcess.Id)), waiting for readiness..."
$taskDeadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
while ((Get-Date) -lt $taskDeadline) {
    $taskProcess.Refresh()
    if ($taskProcess.HasExited) {
        Remove-Item -LiteralPath $taskPidFile -ErrorAction SilentlyContinue
        throw "Demo exited before becoming ready. Check $taskRunDir/server.log and server-error.log."
    }
    if (Test-DemoReady) {
        Write-Host "Demo ready (PID $($taskProcess.Id)): $taskUrl"
        Write-Host "Logs: $taskRunDir/server.log"
        return
    }
    Start-Sleep -Milliseconds 500
}
Stop-Process -Id $taskProcess.Id -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $taskPidFile -ErrorAction SilentlyContinue
throw "Demo did not become ready within $StartupTimeoutSeconds seconds. Check $taskRunDir/server.log and server-error.log."
