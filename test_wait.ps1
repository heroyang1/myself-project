# Wait for backend and verify
Write-Host 'Waiting for backend to start...'
$up = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 3
    $secs = ($i + 1) * 3
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
        $t.Close()
        Write-Host ('[OK] Port 8080 UP after ' + $secs + 's')
        $up = $true
        break
    } catch {
        if ($secs % 15 -eq 0) { Write-Host ('  Waiting... ' + $secs + 's') }
    }
}

if ($up) {
    # Wait extra 5 seconds for full init
    Start-Sleep -Seconds 5
    
    # Quick test: captcha endpoint
    try {
        $resp = Invoke-WebRequest -Uri 'http://localhost:8080/captchaImage' -Method Get -UseBasicParsing -TimeoutSec 10
        $data = $resp.Content | ConvertFrom-Json
        Write-Host ('[PASS] Captcha endpoint | code=' + $data.code + ' | captchaEnabled=' + $data.captchaEnabled)
    } catch {
        Write-Host ('[FAIL] Captcha endpoint: ' + $_.Exception.Message.Substring(0, [Math]::Min(80, $_.Exception.Message.Length)))
    }
    
    # Quick test: course search
    try {
        $body = '{"pageNum":1,"pageSize":3}'
        $resp = Invoke-WebRequest -Uri 'http://localhost:8080/pc/course/search' -Method Post -Body $body -ContentType 'application/json' -UseBasicParsing -TimeoutSec 15
        $data = $resp.Content | ConvertFrom-Json
        $count = 0
        if ($data.data -and $data.data.list) { $count = $data.data.list.Count }
        Write-Host ('[PASS] Course Search | code=' + $data.code + ' | results=' + $count)
    } catch {
        Write-Host ('[FAIL] Course Search: ' + $_.Exception.Message.Substring(0, [Math]::Min(100, $_.Exception.Message.Length)))
    }
} else {
    Write-Host '[FAIL] Backend did not start'
}

# Check process still alive
Write-Host ''
$found = $false
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object {
    $id = $_.Id
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$id"
    if ($proc.CommandLine -match 'backstage-admin') {
        Write-Host ('[OK] Backend process PID=' + $id + ' still running')
        $found = $true
    }
}
if (-not $found) { Write-Host '[WARN] Backend process not found' }
