# test_verify.ps1 - Full verification test after schema/i18n fixes
$ErrorActionPreference = 'Continue'
$base = 'http://localhost:8080'
$pass = 0; $fail = 0; $results = @()

function DoTest($name, $method, $url, $body, $token) {
    try {
        $wc = New-Object System.Net.WebClient
        $wc.Headers.Add('Content-Type', 'application/json')
        if ($token) { $wc.Headers.Add('token', $token) }
        if ($method -eq 'GET') {
            $r = $wc.DownloadString($url)
        } else {
            $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
            $r = [System.Text.Encoding]::UTF8.GetString($wc.UploadData($url, $method, $bytes))
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
        $code = 0
        $msg = $_.Exception.Message
        if ($_.Exception.InnerException -and $_.Exception.InnerException.Message -match '(\d{3})') { $code = $matches[1] }
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

# ===== Step 1: Check middleware =====
Write-Host '===== Step 1: Middleware Check =====' -ForegroundColor Cyan
$mw = @{
    'MySQL(3306)' = 3306; 'Redis(6379)' = 6379; 'ES(9200)' = 9200
    'Kafka(9092)' = 9092; 'XXL-Job(8888)' = 8888
}
foreach ($k in $mw.GetEnumerator()) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient('127.0.0.1', $k.Value)
        $tcp.Close()
        Write-Host "  $($k.Key): UP" -ForegroundColor Green
    } catch {
        Write-Host "  $($k.Key): DOWN" -ForegroundColor Red
    }
}

# ===== Step 2: Start backend =====
Write-Host "`n===== Step 2: Start Backend =====" -ForegroundColor Cyan
$jars = Get-ChildItem -Path 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\*.jar' -ErrorAction SilentlyContinue
if (-not $jars) { Write-Host 'JAR not found!'; exit 1 }
$jarPath = $jars[0].FullName
Write-Host "JAR: $jarPath"

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$psi.Arguments = "-jar `"$jarPath`" --spring.profiles.active=local"
$psi.CreateNoWindow = $false
$psi.UseShellExecute = $false
$proc = [System.Diagnostics.Process]::Start($psi)
Write-Host "Backend PID: $($proc.Id)"

# Wait for port
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 8080)
        $tcp.Close()
        $ready = $true
        Write-Host "Backend ready at ${i}s" -ForegroundColor Green
        break
    } catch {}
}
if (-not $ready) { Write-Host 'Backend failed to start!'; exit 1 }
Start-Sleep -Seconds 8

# ===== Step 3: Authentication =====
Write-Host "`n===== Step 3: Authentication =====" -ForegroundColor Cyan

# 3a: Captcha
$captchaResult = DoTestRaw 'Captcha GET' "$base/captchaImage"
if ($captchaResult) {
    $cj = $captchaResult | ConvertFrom-Json
    $uuid = $cj.uuid
    Write-Host "  uuid=$uuid"
}

# 3b: Login
$loginBody = "{`"username`":`"admin`",`"password`":`"admin123`",`"code`":`"fake`",`"uuid`":`"$uuid`"}"
$loginResult = DoTest 'Admin Login' 'POST' "$base/login" $loginBody $null
$token = ''
if ($loginResult -and $loginResult.token) {
    $token = $loginResult.token
    Write-Host "  Token: $($token.Substring(0, [Math]::Min(20, $token.Length)))..."
}

# 3c: getInfo
if ($token) {
    $gi = DoTest 'getInfo' 'GET' "$base/getInfo" $null $token
    if ($gi) { Write-Host "  user=$($gi.user.userName)" }
}

# ===== Step 4: Course Module =====
Write-Host "`n===== Step 4: Course Module =====" -ForegroundColor Cyan
DoTest 'Course Search (Anonymous)' 'GET' "$base/course/list?pageNum=1&pageSize=5" $null $null
DoTest 'Course Search (Auth)' 'GET' "$base/course/list?pageNum=1&pageSize=5" $null $token
DoTest 'Course Detail' 'GET' "$base/course/1" $null $null

# ===== Step 5: Group Module =====
Write-Host "`n===== Step 5: Group Module =====" -ForegroundColor Cyan
DoTest 'Group List' 'GET' "$base/group/list?pageNum=1&pageSize=5" $null $null
DoTest 'Group Activity Detail' 'GET' "$base/group/activity/1" $null $null

# ===== Step 6: Flashsale Module =====
Write-Host "`n===== Step 6: Flashsale Module =====" -ForegroundColor Cyan
DoTest 'Flashsale List' 'GET' "$base/flashsale/list?pageNum=1&pageSize=5" $null $null
DoTest 'Flashsale Course Read' 'GET' "$base/flashsale/course?flashsale_id=1" $null $null

# ===== Step 7: OpenProject Module =====
Write-Host "`n===== Step 7: OpenProject Module =====" -ForegroundColor Cyan
DoTest 'OpenProject List' 'GET' "$base/open/project/list?pageNum=1&pageSize=5" $null $null
DoTest 'OpenProject Tags' 'GET' "$base/open/project/tags" $null $null
DoTest 'OpenProject Announcements' 'GET' "$base/open/project/announcements" $null $null
DoTest 'OpenProject TechComponents' 'GET' "$base/open/project/tech-components" $null $null

# ===== Step 8: Column Module =====
Write-Host "`n===== Step 8: Column Module =====" -ForegroundColor Cyan
DoTest 'Column List' 'GET' "$base/column/list" $null $null
DoTest 'Column Detail' 'GET' "$base/column/1" $null $null

# ===== Step 9: Book Module =====
Write-Host "`n===== Step 9: Book Module =====" -ForegroundColor Cyan
DoTest 'Book List' 'GET' "$base/book/list?pageNum=1&pageSize=5" $null $null

# ===== Step 10: HomePage Cards (auth-required) =====
Write-Host "`n===== Step 10: HomePage Cards =====" -ForegroundColor Cyan
$cards = @(
    'courseCard','courseColumn','groupCard','flashSaleCard','bookCard',
    'openProjectCard','announcementCard','practicalWebsiteCard',
    'courseTagCard','examCard','flashSaleGoodsCard','memberPlanCard',
    'openProjectTagCard','recommendCourseCard','resourceCard'
)
foreach ($c in $cards) {
    DoTest "Card: $c" 'GET' "$base/homepage/$c" $null $token
}

# ===== Step 11: Other Modules (auth-required) =====
Write-Host "`n===== Step 11: Other Modules =====" -ForegroundColor Cyan
$others = @(
    @('Feedback POST', 'POST', '/feedback', "{`"content`":`"test feedback`",`"type`":1}"),
    @('PracticalWebsite List', 'GET', '/practical/website/list', $null),
    @('Book Detail', 'GET', '/book/1', $null),
    @('FriendLink List', 'GET', '/friend/link/list', $null),
    @('BBS Category List', 'GET', '/bbs/category/list', $null),
    @('Site Info', 'GET', '/site/info', $null)
)
foreach ($t in $others) {
    if ($t[3]) {
        DoTest $t[0] $t[1] "$base$($t[2])" $t[3] $token
    } else {
        DoTest $t[0] $t[1] "$base$($t[2])" $null $token
    }
}

# ===== Step 12: XXL-Job =====
Write-Host "`n===== Step 12: XXL-Job Executor =====" -ForegroundColor Cyan
try {
    $tcp = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 9999)
    $tcp.Close()
    $pass++; $results += '[PASS] XXL-Job Executor port 9999'
} catch {
    $fail++; $results += '[FAIL] XXL-Job Executor port 9999'
}

# ===== Summary =====
Write-Host "`n===== RESULTS: $pass PASS / $fail FAIL =====" -ForegroundColor $(if ($fail -eq 0) { 'Green' } else { 'Yellow' })
foreach ($r in $results) {
    $color = if ($r.StartsWith('[PASS]')) { 'Green' } else { 'Red' }
    Write-Host $r -ForegroundColor $color
}

# ===== Error log scan =====
Write-Host "`n===== Recent Error Log (last 50 lines) =====" -ForegroundColor Cyan
$errLog = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs\sys-error.log'
if (Test-Path $errLog) {
    $lines = Get-Content $errLog -Tail 50
    $errors = $lines | Where-Object { $_ -match 'Exception|Error|error' }
    Write-Host "  Found $($errors.Count) error lines in last 50"
    $errors | Select-Object -First 15 | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
} else {
    Write-Host '  No error log found'
}

# Keep backend running
Write-Host "`nBackend still running (PID: $($proc.Id)). To stop: Stop-Process -Id $($proc.Id)"
