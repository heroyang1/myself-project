# retest3.ps1 - Quick retest of 3 previously failing endpoints
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'

# Start backend
Write-Host 'Starting backend...' -ForegroundColor Cyan
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$psi.Arguments = "-jar `"C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\backstage-admin.jar`" --spring.profiles.active=local"
$psi.CreateNoWindow = $false; $psi.UseShellExecute = $false
$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host "PID: $($proc.Id)"

# Wait for ready
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    try { $t = New-Object System.Net.Sockets.TcpClient('127.0.0.1',8080); $t.Close(); $ready=$true; Write-Host "Ready at ${i}s" -ForegroundColor Green; break } catch {}
}
if (-not $ready) { Write-Host 'FAILED to start!'; exit 1 }
Start-Sleep -Seconds 5

function DoTest($name, $url) {
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Encoding = [System.Text.Encoding]::UTF8
        $wc.Headers.Add('Content-Type', 'application/json')
        $r = $wc.DownloadString($url)
        $json = $r | ConvertFrom-Json
        if ($json.code -eq 200) {
            Write-Host "[PASS] $name" -ForegroundColor Green
            return $true
        } else {
            $shortMsg = $json.msg
            if ($shortMsg -and $shortMsg.Length -gt 120) { $shortMsg = $shortMsg.Substring(0,120) + '...' }
            Write-Host "[FAIL] $name - code=$($json.code) msg=$shortMsg" -ForegroundColor Red
            return $false
        }
    } catch {
        $msg = $_.Exception.Message
        if ($msg.Length -gt 120) { $msg = $msg.Substring(0,120) + '...' }
        Write-Host "[FAIL] $name - $msg" -ForegroundColor Red
        return $false
    }
}

Write-Host "`n===== Retest 3 previously failing endpoints =====" -ForegroundColor Cyan
$p = 0; $f = 0
if (DoTest 'HP Seckill Hot' "$base/pc/homepage/seckill/hot") { $p++ } else { $f++ }
if (DoTest 'HP Book Hot' "$base/pc/homepage/book/hot") { $p++ } else { $f++ }
if (DoTest 'BBS Summary' "$base/pc/bbs") { $p++ } else { $f++ }
Write-Host "`nResult: $p PASS / $f FAIL" -ForegroundColor $(if ($f -eq 0) { 'Green' } else { 'Yellow' })

Write-Host "`nBackend PID: $($proc.Id) (stop with Stop-Process -Id $($proc.Id))"
