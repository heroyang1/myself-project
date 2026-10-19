# Start backend and run tests in same session
$java = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$jar = 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\backstage-admin.jar'
$base = 'http://localhost:8080'

# Clear old logs
$logDir = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs'
if (Test-Path "$logDir\sys-info.log") { Remove-Item "$logDir\sys-info.log" -Force }
if (Test-Path "$logDir\sys-error.log") { Remove-Item "$logDir\sys-error.log" -Force }

Write-Host '=== Starting backend ==='
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $java
$psi.Arguments = '-Dname=backstage-admin.jar -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -jar ' + $jar + ' --spring.profiles.active=local'
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $false
$psi.RedirectStandardError = $false
$psi.CreateNoWindow = $false
$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host ('Started PID: ' + $proc.Id)

# Wait for startup
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 3
    if ($proc.HasExited) {
        Write-Host ('[CRASH] Exit code: ' + $proc.ExitCode)
        exit 1
    }
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
        $t.Close()
        Write-Host ('[OK] Port 8080 UP at ' + (($i+1)*3) + 's')
        break
    } catch {
        if (($i+1) % 5 -eq 0) { Write-Host ('  Waiting... ' + (($i+1)*3) + 's') }
    }
}

# Wait for full startup
Start-Sleep -Seconds 8
if ($proc.HasExited) {
    Write-Host ('[CRASH] Process died after port bind')
    exit 1
}

Write-Host ''
Write-Host '=== Running API Tests ==='

# Test helper
function DoTest($name, $method, $url, $body) {
    try {
        $p = @{ Uri=($base+$url); Method=$method; UseBasicParsing=$true; TimeoutSec=15; ContentType='application/json' }
        if ($body) { $p['Body'] = ($body | ConvertTo-Json -Depth 5 -Compress) }
        $r = Invoke-WebRequest @p
        $d = $r.Content | ConvertFrom-Json
        $code = if ($d.code) { $d.code } else { $r.StatusCode }
        $msg = if ($d.msg) { $d.msg } else { 'OK' }
        $rows = 0
        if ($d.rows) { $rows = $d.rows.Count }
        elseif ($d.data -and $d.data.list) { $rows = $d.data.list.Count }
        elseif ($d.data -and $d.data -is [array]) { $rows = $d.data.Count }
        Write-Host ('[PASS] ' + $name + ' | code=' + $code + ' | rows=' + $rows + ' | ' + $msg)
        return $true
    } catch {
        $sc = 0
        $eb = ''
        if ($_.Exception.Response) {
            $sc = [int]$_.Exception.Response.StatusCode
            try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $eb = $sr.ReadToEnd(); $sr.Close() } catch {}
        }
        $em = $_.Exception.Message
        if ($em.Length -gt 80) { $em = $em.Substring(0, 80) }
        Write-Host ('[FAIL] ' + $name + ' | HTTP=' + $sc + ' | ' + $em)
        if ($eb -and $eb.Length -gt 0 -and $eb.Length -le 300) { Write-Host ('  Body: ' + $eb) }
        return $false
    }
}

# 1. Captcha
Write-Host ''
Write-Host '--- 1. Authentication ---'
$cap = $null
try {
    $cap = Invoke-RestMethod -Uri ($base + '/captchaImage') -Method Get -TimeoutSec 10
    Write-Host ('[PASS] Captcha | enabled=' + $cap.captchaEnabled + ' | uuid=' + $cap.uuid.Substring(0,16) + '...')
} catch {
    Write-Host ('[FAIL] Captcha: ' + $_.Exception.Message.Substring(0,60))
}

# Admin login
try {
    $lb = '{"username":"admin","password":"admin123","code":"test","uuid":"test"}'
    $lr = Invoke-WebRequest -Uri ($base + '/login') -Method Post -Body $lb -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $ld = $lr.Content | ConvertFrom-Json
    if ($ld.token) {
        $tk = $ld.token
        Write-Host ('[PASS] Admin Login | token=' + $tk.Substring(0,20) + '...')
        # Test getInfo
        $gi = Invoke-RestMethod -Uri ($base + '/getInfo') -Method Get -Headers @{Authorization='Bearer '+$tk} -TimeoutSec 10
        Write-Host ('[PASS] GetInfo | user=' + $gi.user.userName + ' | roles=' + ($gi.roles -join ','))
    } else {
        Write-Host ('[FAIL] Admin Login | code=' + $ld.code + ' | msg=' + $ld.msg)
    }
} catch {
    $em = $_.Exception.Message
    if ($em.Length -gt 80) { $em = $em.Substring(0,80) }
    Write-Host ('[FAIL] Admin Login: ' + $em)
}

# 2. Public APIs
Write-Host ''
Write-Host '--- 2. Course Module ---'
DoTest 'Course Search' POST '/pc/course/search' @{pageNum=1;pageSize=3} | Out-Null
DoTest 'Course ES Sync All' POST '/pc/course/esSync/all' $null | Out-Null

Write-Host ''
Write-Host '--- 3. Group (Group-Buy) Module ---'
DoTest 'Group List' GET '/pc/group/list?groupId=1&page=1' $null | Out-Null

Write-Host ''
Write-Host '--- 4. Flashsale Module ---'
DoTest 'Flashsale Course Read' GET '/pc/flashsale/course/read?id=1' $null | Out-Null
DoTest 'Flashsale List' GET '/pc/flashsale/list?pageNum=1&pageSize=3' $null | Out-Null

Write-Host ''
Write-Host '--- 5. OpenProject Module ---'
DoTest 'OpenProject List' POST '/pc/openproject/list' @{pageNum=1;pageSize=3} | Out-Null
DoTest 'OpenProject Tags' GET '/pc/openproject/tags' $null | Out-Null
DoTest 'OpenProject Announcements' GET '/pc/openproject/announcements' $null | Out-Null
DoTest 'OpenProject TechComponents' GET '/pc/openproject/tech-components' $null | Out-Null

Write-Host ''
Write-Host '--- 6. Book Module ---'
DoTest 'Book List' GET '/pc/book/list?pageNum=1&pageSize=3' $null | Out-Null

Write-Host ''
Write-Host '--- 7. Column Module ---'
DoTest 'Column List' GET '/pc/column/list?pageNum=1&pageSize=3' $null | Out-Null

Write-Host ''
Write-Host '--- 8. HomePage Cards ---'
$hpModules = @('course','group','seckill','tool','book','website','infoGap','exam','feedback','announcement','carousel','nav','qa','openproject','memberPlan')
foreach ($m in $hpModules) {
    DoTest ('HomePage ' + $m) GET ('/pc/homepage/' + $m + '/list?pageNum=1&pageSize=3') $null | Out-Null
}

Write-Host ''
Write-Host '--- 9. Other Modules ---'
DoTest 'Friend Links' GET '/pc/friendlink/list' $null | Out-Null
DoTest 'BBS Categories' GET '/pc/bbs/category/list' $null | Out-Null
DoTest 'BBS Posts' GET '/pc/bbs/post/list?pageNum=1&pageSize=3' $null | Out-Null
DoTest 'Assistant Info' GET '/pc/assistant/info' $null | Out-Null
DoTest 'InfoGap List' GET '/pc/infogap/list?pageNum=1&pageSize=3' $null | Out-Null
DoTest 'Feedback Types' GET '/pc/assistant/feedback/public/types' $null | Out-Null

Write-Host ''
Write-Host '--- 10. XXL-Job Executor Check ---'
if ($proc.HasExited) {
    Write-Host '[FAIL] Backend process died!'
} else {
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', 9999)
        Write-Host '[PASS] XXL-Job Executor (port 9999) still UP'
        $t.Close()
    } catch {
        Write-Host '[WARN] XXL-Job Executor (port 9999) not listening'
    }
}

Write-Host ''
Write-Host '--- 11. Error Log Check ---'
$errLog = "$logDir\sys-error.log"
if (Test-Path $errLog) {
    $lines = Get-Content $errLog
    $mwErrors = $lines | Where-Object { $_ -match 'kafka|Kafka|elasticsearch|Connection refused|Connection reset|SocketTimeout|session\.timeout' }
    if ($mwErrors -and $mwErrors.Count -gt 0) {
        Write-Host ('[WARN] ' + $mwErrors.Count + ' middleware errors found')
        foreach ($e in ($mwErrors | Select-Object -First 3)) { Write-Host ('  ' + $e.Substring(0, [Math]::Min(120, $e.Length))) }
    } else {
        Write-Host ('[PASS] No middleware errors (total: ' + $lines.Count + ' lines)')
    }
} else {
    Write-Host '[PASS] No error log'
}

Write-Host ''
Write-Host '=== Test Complete ==='
