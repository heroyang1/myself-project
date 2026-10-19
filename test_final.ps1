# test_final.ps1 - Correct URLs (/pc/ prefix) + proper auth headers
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$pass = 0; $fail = 0; $results = @()

function DoTest($name, $method, $url, $body, $token) {
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Encoding = [System.Text.Encoding]::UTF8
        $wc.Headers.Add('Content-Type', 'application/json')
        if ($token) { $wc.Headers.Add('Authorization', "Bearer $token") }
        if ($method -eq 'GET') {
            $r = $wc.DownloadString($url)
        } else {
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
            $r = $wc.UploadString($url, $method, $body)
        }
        $json = $r | ConvertFrom-Json
        if ($json.code -eq 200) {
            $script:pass++; $script:results += "[PASS] $name"
            return $json
        } else {
            $script:fail++; $script:results += "[FAIL] $name - code=$($json.code) msg=$($json.msg)"
            return $null
        }
    } catch {
        $msg = $_.Exception.Message
        if ($msg.Length -gt 100) { $msg = $msg.Substring(0, 100) + '...' }
        $script:fail++; $script:results += "[FAIL] $name - $msg"
        return $null
    }
}

function DoTestRaw($name, $url) {
    try {
        $wc = New-Object System.Net.WebClient
        $r = $wc.DownloadString($url)
        $script:pass++; $script:results += "[PASS] $name"
        return $r
    } catch {
        $script:fail++; $script:results += "[FAIL] $name - $($_.Exception.Message)"
        return $null
    }
}

# ===== Step 1: Middleware =====
Write-Host '===== Step 1: Middleware =====' -ForegroundColor Cyan
@(3306,6379,9200,9092,8888) | ForEach-Object {
    $p = $_
    try { $t = New-Object System.Net.Sockets.TcpClient('127.0.0.1',$p); $t.Close(); Write-Host "  Port ${p}: UP" -ForegroundColor Green }
    catch { Write-Host "  Port ${p}: DOWN" -ForegroundColor Red }
}

# ===== Step 2: Backend =====
Write-Host "`n===== Step 2: Backend =====" -ForegroundColor Cyan
$jar = (Get-ChildItem 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\*.jar')[0].FullName
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$psi.Arguments = "-jar `"$jar`" --spring.profiles.active=local"
$psi.CreateNoWindow = $false; $psi.UseShellExecute = $false
$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host "PID: $($proc.Id)"
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    try { $t = New-Object System.Net.Sockets.TcpClient('127.0.0.1',8080); $t.Close(); $ready=$true; Write-Host "Ready at ${i}s" -ForegroundColor Green; break } catch {}
}
if (-not $ready) { Write-Host 'FAILED to start!'; exit 1 }
Start-Sleep -Seconds 8

# ===== Step 3: Auth =====
Write-Host "`n===== Step 3: Auth =====" -ForegroundColor Cyan
$captchaRaw = DoTestRaw 'Captcha' "$base/captchaImage"
$uuid = ''
if ($captchaRaw) { $cj = $captchaRaw | ConvertFrom-Json; $uuid = $cj.uuid }

$loginBody = "{`"username`":`"admin`",`"password`":`"admin123`",`"code`":`"fake`",`"uuid`":`"$uuid`"}"
$loginRes = DoTest 'Admin Login' 'POST' "$base/login" $loginBody $null
$adminToken = ''
if ($loginRes -and $loginRes.token) { $adminToken = $loginRes.token; Write-Host "  Token OK" }

# getInfo (admin auth: Authorization: Bearer <jwt>)
if ($adminToken) { DoTest 'getInfo' 'GET' "$base/getInfo" $null $adminToken }

# ===== Step 4: Course (@Anonymous, no token needed) =====
Write-Host "`n===== Step 4: Course =====" -ForegroundColor Cyan
DoTest 'Course Search' 'POST' "$base/pc/course/search" '{"pageNum":1,"pageSize":5}' $null
DoTest 'Course Detail' 'GET' "$base/pc/course/detail/1" $null $null

# ===== Step 5: Group =====
Write-Host "`n===== Step 5: Group =====" -ForegroundColor Cyan
DoTest 'Group List' 'GET' "$base/pc/group/list?groupId=1&page=1" $null $null
DoTest 'Group Course Read' 'GET' "$base/pc/group/course/read?id=1&groupId=1" $null $null

# ===== Step 6: Flashsale =====
Write-Host "`n===== Step 6: Flashsale =====" -ForegroundColor Cyan
DoTest 'Flashsale Course Read' 'GET' "$base/pc/flashsale/course/read?id=1&flashsale_id=1" $null $null
DoTest 'Flashsale List' 'GET' "$base/pc/flashsale/list" $null $adminToken

# ===== Step 7: OpenProject =====
Write-Host "`n===== Step 7: OpenProject =====" -ForegroundColor Cyan
DoTest 'OpenProject List' 'POST' "$base/pc/openproject/list" '{"pageNum":1,"pageSize":5}' $null
DoTest 'OpenProject Tags' 'GET' "$base/pc/openproject/tags" $null $null
DoTest 'OpenProject Announcements' 'GET' "$base/pc/openproject/announcements" $null $null
DoTest 'OpenProject TechComponents' 'GET' "$base/pc/openproject/tech-components" $null $null

# ===== Step 8: Column =====
Write-Host "`n===== Step 8: Column =====" -ForegroundColor Cyan
DoTest 'Column List' 'GET' "$base/pc/column/list?page=1&limit=5" $null $null
DoTest 'Column Detail' 'GET' "$base/pc/column/read?id=1" $null $null

# ===== Step 9: Book =====
Write-Host "`n===== Step 9: Book =====" -ForegroundColor Cyan
DoTest 'Book List' 'POST' "$base/pc/book/page" '{"pageNum":1,"pageSize":5}' $null

# ===== Step 10: HomePage Cards (@Anonymous) =====
Write-Host "`n===== Step 10: HomePage =====" -ForegroundColor Cyan
$hp = @(
    @('HP Course Hot', '/pc/homepage/course/hot'),
    @('HP Course Latest', '/pc/homepage/course/latest-updates?courseId=1&limit=5'),
    @('HP Group Hot', '/pc/homepage/group/hot'),
    @('HP Seckill Hot', '/pc/homepage/seckill/hot'),
    @('HP Book Hot', '/pc/homepage/book/hot'),
    @('HP OpenProject Hot', '/pc/homepage/openproject/hot'),
    @('HP Announcement Notice', '/pc/homepage/announcement/notice'),
    @('HP Announcement Dynamic', '/pc/homepage/announcement/dynamic'),
    @('HP Website Notices', '/pc/website/notices'),
    @('HP Exam Hot', '/pc/homepage/exam/hot'),
    @('HP Feedback Hot', '/pc/homepage/feedback/hot'),
    @('HP Member Plans', '/pc/homepage/member/plans'),
    @('HP Nav Modules', '/pc/homepage/nav/modules'),
    @('HP QA Hot', '/pc/homepage/qa/hot'),
    @('HP Tool Hot', '/pc/homepage/tool/hot'),
    @('HP InfoGap Hot', '/pc/homepage/infogap/hot'),
    @('HP Carousel Visible', '/pc/homepage/carousel/visible')
)
foreach ($h in $hp) { DoTest $h[0] 'GET' "$base$($h[1])" $null $null }

# ===== Step 11: Other (@Anonymous) =====
Write-Host "`n===== Step 11: Other Modules =====" -ForegroundColor Cyan
DoTest 'FriendLink List' 'GET' "$base/pc/site/friend-link/list" $null $null
DoTest 'Website Tags' 'GET' "$base/pc/website/tags" $null $null
DoTest 'BBS Summary' 'GET' "$base/pc/bbs" $null $null
DoTest 'PracticalWebsite Dynamics' 'GET' "$base/pc/website/dynamics" $null $null
DoTest 'Site List' 'POST' "$base/pc/site/list" '{"pageNum":1,"pageSize":5}' $null
DoTest 'Book Page' 'POST' "$base/pc/book/page" '{"pageNum":1,"pageSize":5}' $null

# ===== Step 12: Admin-only (with Bearer token) =====
Write-Host "`n===== Step 12: Admin Endpoints =====" -ForegroundColor Cyan
if ($adminToken) {
    DoTest 'Admin User List' 'GET' "$base/system/user/list?pageNum=1&pageSize=5" $null $adminToken
    DoTest 'Admin Role List' 'GET' "$base/system/role/list?pageNum=1&pageSize=5" $null $adminToken
    DoTest 'Admin Menu List' 'GET' "$base/system/menu/list" $null $adminToken
}

# ===== Step 13: XXL-Job =====
Write-Host "`n===== Step 13: XXL-Job =====" -ForegroundColor Cyan
try { $t = New-Object System.Net.Sockets.TcpClient('127.0.0.1',9999); $t.Close(); $pass++; $results += '[PASS] XXL-Job port 9999' }
catch { $fail++; $results += '[FAIL] XXL-Job port 9999' }

# ===== Summary =====
Write-Host "`n===== RESULTS: $pass PASS / $fail FAIL =====" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Yellow' })
foreach ($r in $results) {
    $c = if ($r.StartsWith('[PASS]')) { 'Green' } else { 'Red' }
    Write-Host $r -ForegroundColor $c
}

# Error log
Write-Host "`n===== Error Log (last 30 lines) =====" -ForegroundColor Cyan
$el = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs\sys-error.log'
if (Test-Path $el) {
    $lines = Get-Content $el -Tail 30 | Where-Object { $_ -match 'ERROR|Exception' -and $_ -notmatch '^\s+at ' }
    Write-Host "  Error lines: $($lines.Count)"
    $lines | Select-Object -First 10 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
}

Write-Host "`nBackend PID: $($proc.Id)"
