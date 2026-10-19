# Retest after DB fix - starts backend in same session
$java = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$jar = 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\backstage-admin.jar'
$base = 'http://localhost:8080'
$logDir = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs'

# Clear old logs
if (Test-Path "$logDir\sys-error.log") { Remove-Item "$logDir\sys-error.log" -Force }
if (Test-Path "$logDir\sys-info.log") { Remove-Item "$logDir\sys-info.log" -Force }

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
    if ($proc.HasExited) { Write-Host ('[CRASH] Exit code: ' + $proc.ExitCode); exit 1 }
    try { $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080); $t.Close(); Write-Host ('[OK] Port 8080 UP at ' + (($i+1)*3) + 's'); break } catch { if (($i+1) % 5 -eq 0) { Write-Host ('  Waiting... ' + (($i+1)*3) + 's') } }
}
Start-Sleep -Seconds 8
if ($proc.HasExited) { Write-Host '[CRASH] Died after port bind'; exit 1 }

Write-Host ''; Write-Host '=== Running API Tests ==='

# Test helper
$pass = 0; $fail = 0
function DoTest($name, $method, $url, $body, $headers) {
    try {
        $p = @{ Uri=($base+$url); Method=$method; UseBasicParsing=$true; TimeoutSec=15; ContentType='application/json' }
        if ($body) { $p['Body'] = ($body | ConvertTo-Json -Depth 5 -Compress) }
        if ($headers) { $p['Headers'] = $headers }
        $r = Invoke-WebRequest @p
        $d = $r.Content | ConvertFrom-Json
        $code = if ($d.code -ne $null) { $d.code } else { $r.StatusCode }
        $msg = if ($d.msg) { $d.msg } else { 'OK' }
        $rows = 0
        if ($d.rows) { $rows = $d.rows.Count }
        elseif ($d.data -and $d.data.list) { $rows = $d.data.list.Count }
        elseif ($d.data -and $d.data -is [array]) { $rows = $d.data.Count }
        elseif ($d.data -and $d.data.total) { $rows = $d.data.total }
        $script:pass++
        Write-Host ('[PASS] ' + $name + ' | code=' + $code + ' | rows=' + $rows + ' | ' + $msg)
        return $d
    } catch {
        $sc = 0; $eb = ''
        if ($_.Exception.Response) {
            $sc = [int]$_.Exception.Response.StatusCode
            try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $eb = $sr.ReadToEnd(); $sr.Close() } catch {}
        }
        $em = $_.Exception.Message
        if ($em.Length -gt 100) { $em = $em.Substring(0, 100) }
        $script:fail++
        Write-Host ('[FAIL] ' + $name + ' | HTTP=' + $sc + ' | ' + $em)
        if ($eb -and $eb.Length -gt 0 -and $eb.Length -le 300) { Write-Host ('  Body: ' + $eb) }
        return $null
    }
}

# ---- 1. Authentication ----
Write-Host ''; Write-Host '--- 1. Authentication ---'
$cap = $null
try {
    $cap = Invoke-RestMethod -Uri ($base + '/captchaImage') -Method Get -TimeoutSec 10
    $script:pass++
    Write-Host ('[PASS] Captcha | enabled=' + $cap.captchaEnabled + ' | uuid=' + $cap.uuid.Substring(0,16) + '...')
} catch {
    $script:fail++
    Write-Host ('[FAIL] Captcha: ' + $_.Exception.Message.Substring(0,60))
}

$token = $null
try {
    $lb = '{"username":"admin","password":"admin123","code":"test","uuid":"test"}'
    $lr = Invoke-WebRequest -Uri ($base + '/login') -Method Post -Body $lb -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $ld = $lr.Content | ConvertFrom-Json
    if ($ld.token) {
        $token = $ld.token
        $script:pass++
        Write-Host ('[PASS] Admin Login | token=' + $token.Substring(0,20) + '...')
        $authHeaders = @{Authorization='Bearer '+$token}
        $gi = Invoke-RestMethod -Uri ($base + '/getInfo') -Method Get -Headers $authHeaders -TimeoutSec 10
        $script:pass++
        Write-Host ('[PASS] GetInfo | user=' + $gi.user.userName + ' | roles=' + ($gi.roles -join ','))
    } else {
        $script:fail++
        Write-Host ('[FAIL] Admin Login | code=' + $ld.code + ' | msg=' + $ld.msg)
    }
} catch {
    $script:fail++
    $em = $_.Exception.Message; if ($em.Length -gt 80) { $em = $em.Substring(0,80) }
    Write-Host ('[FAIL] Admin Login: ' + $em)
}

# ---- 2. Course Module ----
Write-Host ''; Write-Host '--- 2. Course Module ---'
DoTest 'Course Search (MySQL)' POST '/pc/course/search' @{pageNum=1;pageSize=3} $null | Out-Null
DoTest 'Course ES Sync' POST '/pc/course/esSync/all' $null $null | Out-Null

# ---- 3. Group Module ----
Write-Host ''; Write-Host '--- 3. Group Module ---'
DoTest 'Group List' GET '/pc/group/list?groupId=1&page=1' $null $null | Out-Null

# ---- 4. Flashsale Module ----
Write-Host ''; Write-Host '--- 4. Flashsale Module ---'
DoTest 'Flashsale Course Read' GET '/pc/flashsale/course/read?flashsale_id=1' $null $null | Out-Null
DoTest 'Flashsale List' GET '/pc/flashsale/list?pageNum=1&pageSize=3' $null $null | Out-Null

# ---- 5. OpenProject Module ----
Write-Host ''; Write-Host '--- 5. OpenProject Module ---'
DoTest 'OpenProject List' POST '/pc/openproject/list' @{pageNum=1;pageSize=3} $null | Out-Null
DoTest 'OpenProject Tags' GET '/pc/openproject/tags' $null $null | Out-Null
DoTest 'OpenProject Announcements' GET '/pc/openproject/announcements' $null $null | Out-Null
DoTest 'OpenProject TechComponents' GET '/pc/openproject/tech-components' $null $null | Out-Null

# ---- 6. Book & Column ----
Write-Host ''; Write-Host '--- 6. Book & Column ---'
DoTest 'Book List' GET '/pc/book/list?pageNum=1&pageSize=3' $null $null | Out-Null
DoTest 'Column List' GET '/pc/column/list?page=1&size=3' $null $null | Out-Null

# ---- 7. HomePage Cards (need auth) ----
Write-Host ''; Write-Host '--- 7. HomePage Cards ---'
$hpAuth = if ($token) { @{Authorization='Bearer '+$token} } else { $null }
$hpModules = @('course','group','seckill','tool','book','website','infoGap','exam','feedback','announcement','carousel','nav','qa','openproject','memberPlan')
foreach ($m in $hpModules) {
    DoTest ('HomePage ' + $m) GET ('/pc/homepage/' + $m + '/list?pageNum=1&pageSize=3') $null $hpAuth | Out-Null
}

# ---- 8. Other Modules ----
Write-Host ''; Write-Host '--- 8. Other Modules ---'
DoTest 'Friend Links' GET '/pc/friendlink/list' $null $null | Out-Null
DoTest 'BBS Categories' GET '/pc/bbs/category/list' $null $null | Out-Null
DoTest 'BBS Posts' GET '/pc/bbs/post/list?pageNum=1&pageSize=3' $null $null | Out-Null
DoTest 'Assistant Info' GET '/pc/assistant/info' $null $null | Out-Null
DoTest 'InfoGap List' GET '/pc/infogap/list?pageNum=1&pageSize=3' $null $null | Out-Null
DoTest 'Feedback Types' GET '/pc/assistant/feedback/public/types' $null $null | Out-Null

# ---- 9. XXL-Job Check ----
Write-Host ''; Write-Host '--- 9. XXL-Job ---'
if ($proc.HasExited) {
    Write-Host '[FAIL] Backend died!'
    $script:fail++
} else {
    try { $t = New-Object System.Net.Sockets.TcpClient('localhost', 9999); Write-Host '[PASS] XXL-Job Executor (port 9999)'; $t.Close(); $script:pass++ } catch { Write-Host '[WARN] XXL-Job Executor not listening'; $script:fail++ }
}

# ---- 10. Error Log ----
Write-Host ''; Write-Host '--- 10. Error Log Check ---'
Start-Sleep -Seconds 3
$errLog = "$logDir\sys-error.log"
if (Test-Path $errLog) {
    $lines = Get-Content $errLog -Encoding UTF8
    $mwErrors = @($lines | Where-Object { $_ -match 'kafka|Kafka|elasticsearch|Connection refused|SocketTimeout|session\.timeout|SQLSyntax|doesn''t exist|Unknown column' })
    if ($mwErrors.Count -gt 0) {
        Write-Host ('[WARN] ' + $mwErrors.Count + ' errors found')
        foreach ($e in ($mwErrors | Select-Object -First 5)) {
            $el = if ($e.Length -gt 140) { $e.Substring(0,140) } else { $e }
            Write-Host ('  ' + $el)
        }
    } else {
        Write-Host ('[PASS] No middleware/SQL errors (total: ' + $lines.Count + ' lines)')
    }
} else {
    Write-Host '[PASS] No error log generated'
}

Write-Host ''
Write-Host ('=== SUMMARY: ' + $script:pass + ' PASS / ' + $script:fail + ' FAIL ===')
Write-Host '=== Test Complete ==='
