# Quick status check
Write-Host '=== Port 8080 ==='
try {
    $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
    Write-Host '[UP] Port 8080'
    $t.Close()
} catch {
    Write-Host '[DOWN] Port 8080'
}

Write-Host ''
Write-Host '=== Backend Process ==='
$found = $false
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object {
    $id = $_.Id
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$id"
    if ($proc.CommandLine -match 'backstage-admin') {
        $ws = [math]::Round($_.WorkingSet64 / 1MB, 0)
        Write-Host ('PID=' + $id + ' WS=' + $ws + 'MB (backstage-admin)')
        $found = $true
    }
}
if (-not $found) { Write-Host 'Backend process NOT found!' }

Write-Host ''
Write-Host '=== Error Log (last 20 lines) ==='
$errLog = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs\sys-error.log'
if (Test-Path $errLog) {
    Get-Content $errLog -Tail 20
} else {
    Write-Host 'No error log'
}

Write-Host ''
Write-Host '=== Info Log (startup line + last 10) ==='
$infoLog = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs\sys-info.log'
if (Test-Path $infoLog) {
    $started = Select-String -Path $infoLog -Pattern 'Started RuoYiApplication' -SimpleMatch
    if ($started) { Write-Host ('STARTUP: ' + $started.Line) } else { Write-Host 'No startup line found!' }
    Write-Host '--- Last 10 lines ---'
    Get-Content $infoLog -Tail 10
} else {
    Write-Host 'No info log'
}
