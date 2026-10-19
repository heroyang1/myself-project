$base = 'http://localhost:8080'
$results = @()

function Test-Api {
    param($Name, $Method, $Url, $Body, $Token, $Header)
    try {
        $params = @{
            Uri = ($base + $Url)
            Method = $Method
            UseBasicParsing = $true
            TimeoutSec = 15
            ContentType = 'application/json'
        }
        if ($Token) {
            $params['Headers'] = @{ 'Authorization' = ('Bearer ' + $Token) }
        }
        if ($Header) {
            if (-not $params['Headers']) { $params['Headers'] = @{} }
            foreach ($k in $Header.Keys) { $params['Headers'][$k] = $Header[$k] }
        }
        if ($Body) { $params['Body'] = ($Body | ConvertTo-Json -Depth 5 -Compress) }
        $resp = Invoke-WebRequest @params
        $data = $resp.Content | ConvertFrom-Json
        $code = if ($data.code) { $data.code } else { $resp.StatusCode }
        $msg = if ($data.msg) { $data.msg } else { 'OK' }
        $rows = 0
        if ($data.rows) { $rows = $data.rows.Count }
        elseif ($data.data -and $data.data.list) { $rows = $data.data.list.Count }
        elseif ($data.data -and $data.data -is [array]) { $rows = $data.data.Count }
        Write-Host ('[PASS] ' + $Name + ' | code=' + $code + ' | msg=' + $msg + ' | rows=' + $rows)
        return @{ Name=$Name; Status='PASS'; Code=$code; Msg=$msg; Rows=$rows }
    } catch {
        $statusCode = 0
        $errBody = ''
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $errBody = $reader.ReadToEnd()
                $reader.Close()
            } catch {}
        }
        $shortErr = $_.Exception.Message
        if ($shortErr.Length -gt 120) { $shortErr = $shortErr.Substring(0, 120) }
        Write-Host ('[FAIL] ' + $Name + ' | HTTP=' + $statusCode + ' | ' + $shortErr)
        if ($errBody.Length -gt 0) {
            $shortBody = $errBody
            if ($shortBody.Length -gt 200) { $shortBody = $shortBody.Substring(0, 200) + '...' }
            Write-Host ('  Body: ' + $shortBody)
        }
        return @{ Name=$Name; Status='FAIL'; Code=$statusCode; Msg=$shortErr; Body=$errBody }
    }
}

Write-Host '============================================'
Write-Host '  OSH Backend Integration Test Suite'
Write-Host '============================================'
Write-Host ''

# ====== 1. Captcha + Login ======
Write-Host '--- 1. Authentication ---'
$captchaResp = $null
try {
    $captchaResp = Invoke-RestMethod -Uri ($base + '/captchaImage') -Method Get -TimeoutSec 10
    Write-Host ('[INFO] Captcha UUID: ' + $captchaResp.uuid.Substring(0, [Math]::Min(20, $captchaResp.uuid.Length)) + '...')
    Write-Host ('[INFO] Captcha enabled: ' + $captchaResp.captchaEnabled)
} catch {
    Write-Host ('[WARN] Captcha fetch failed: ' + $_.Exception.Message)
}

# Try login with admin/admin123 (may fail due to captcha)
$loginBody = @{ username='admin'; password='admin123'; code='0000'; uuid='test' }
$loginResult = $null
try {
    $loginResp = Invoke-WebRequest -Uri ($base + '/login') -Method Post -Body ($loginBody | ConvertTo-Json) -ContentType 'application/json' -UseBasicParsing -TimeoutSec 10
    $loginData = $loginResp.Content | ConvertFrom-Json
    if ($loginData.token) {
        $adminToken = $loginData.token
        Write-Host ('[PASS] Admin Login | token=' + $adminToken.Substring(0, 20) + '...')
        $loginResult = 'PASS'
    } else {
        Write-Host ('[FAIL] Admin Login | code=' + $loginData.code + ' | msg=' + $loginData.msg)
        $loginResult = 'FAIL:' + $loginData.msg
    }
} catch {
    $errCode = 0
    if ($_.Exception.Response) { $errCode = [int]$_.Exception.Response.StatusCode }
    Write-Host ('[FAIL] Admin Login | HTTP=' + $errCode + ' (captcha required - expected)')
    $loginResult = 'FAIL:captcha_required'
}

# C-User login (OshUser)
$oshLoginBody = @{ username='admin'; password='admin123' }
try {
    $oshLoginResp = Invoke-RestMethod -Uri ($base + '/api/oshUser/login') -Method Post -Body ($oshLoginBody | ConvertTo-Json) -ContentType 'application/json' -TimeoutSec 10
    if ($oshLoginResp.data) {
        Write-Host ('[PASS] OshUser Login | data=' + ($oshLoginResp.data | ConvertTo-Json -Compress -Depth 2).Substring(0, [Math]::Min(100, ($oshLoginResp.data | ConvertTo-Json -Compress -Depth 2).Length)) + '...')
    } else {
        Write-Host ('[INFO] OshUser Login | code=' + $oshLoginResp.code + ' | msg=' + $oshLoginResp.msg)
    }
} catch {
    Write-Host ('[INFO] OshUser Login endpoint: ' + $_.Exception.Message.Substring(0, [Math]::Min(100, $_.Exception.Message.Length)))
}

Write-Host ''

# ====== 2. Anonymous/Public APIs ======
Write-Host '--- 2. Public API Tests (@Anonymous) ---'

# Course search
$r = Test-Api -Name 'Course Search' -Method POST -Url '/pc/course/search' -Body @{ pageNum=1; pageSize=5 }
$results += $r

# Course detail (try id=1)
$r = Test-Api -Name 'Course Detail (id=1)' -Method GET -Url '/pc/course/detail/1'
$results += $r

# Group course
$r = Test-Api -Name 'Group Course (id=1,groupId=1)' -Method GET -Url '/pc/group/course/read?id=1&groupId=1'
$results += $r

# Group list
$r = Test-Api -Name 'Group List (groupId=1)' -Method GET -Url '/pc/group/list?groupId=1&page=1'
$results += $r

# Flashsale course read
$r = Test-Api -Name 'Flashsale Course Read' -Method GET -Url '/pc/flashsale/course/read?id=1'
$results += $r

# Open Project list
$r = Test-Api -Name 'OpenProject List' -Method POST -Url '/pc/openproject/list' -Body @{ pageNum=1; pageSize=5 }
$results += $r

# Open Project tags
$r = Test-Api -Name 'OpenProject Tags' -Method GET -Url '/pc/openproject/tags'
$results += $r

# Open Project announcements
$r = Test-Api -Name 'OpenProject Announcements' -Method GET -Url '/pc/openproject/announcements'
$results += $r

# Book list
$r = Test-Api -Name 'Book List' -Method GET -Url '/pc/book/list?pageNum=1&pageSize=5'
$results += $r

# Column list
$r = Test-Api -Name 'Column List' -Method GET -Url '/pc/column/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (course)
$r = Test-Api -Name 'HomePage Course' -Method GET -Url '/pc/homepage/course/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (group)
$r = Test-Api -Name 'HomePage Group' -Method GET -Url '/pc/homepage/group/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (seckill)
$r = Test-Api -Name 'HomePage Seckill' -Method GET -Url '/pc/homepage/seckill/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (tool)
$r = Test-Api -Name 'HomePage Tool' -Method GET -Url '/pc/homepage/tool/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (book)
$r = Test-Api -Name 'HomePage Book' -Method GET -Url '/pc/homepage/book/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (website)
$r = Test-Api -Name 'HomePage Website' -Method GET -Url '/pc/homepage/website/list?pageNum=1&pageSize=5'
$results += $r

# Homepage card (info_gap)
$r = Test-Api -Name 'HomePage InfoGap' -Method GET -Url '/pc/homepage/infoGap/list?pageNum=1&pageSize=5'
$results += $r

# Friend links
$r = Test-Api -Name 'Friend Links' -Method GET -Url '/pc/friendlink/list'
$results += $r

# BBS categories
$r = Test-Api -Name 'BBS Categories' -Method GET -Url '/pc/bbs/category/list'
$results += $r

# BBS posts
$r = Test-Api -Name 'BBS Posts' -Method GET -Url '/pc/bbs/post/list?pageNum=1&pageSize=5'
$results += $r

# Assistant
$r = Test-Api -Name 'Assistant Info' -Method GET -Url '/pc/assistant/info'
$results += $r

# InfoGap list
$r = Test-Api -Name 'InfoGap List' -Method GET -Url '/pc/infogap/list?pageNum=1&pageSize=5'
$results += $r

# Feedback public submit test (GET to check endpoint exists)
$r = Test-Api -Name 'Feedback Public Endpoint' -Method GET -Url '/pc/assistant/feedback/public/types'
$results += $r

# ES health check
$r = Test-Api -Name 'ES Health (direct)' -Method GET -Url '/pc/course/esSync/all'
$results += $r

Write-Host ''
Write-Host '--- 3. XXL-Job Admin Check ---'
try {
    $loginResp = Invoke-WebRequest -Uri 'http://localhost:8888/xxl-job-admin/login' -Method Post -Body 'userName=admin&password=123456' -ContentType 'application/x-www-form-urlencoded' -SessionVariable session -UseBasicParsing -TimeoutSec 10
    Write-Host ('[PASS] XXL-Job Admin Login | status=' + $loginResp.StatusCode)
    $execResp = Invoke-WebRequest -Uri 'http://localhost:8888/xxl-job-admin/jobgroup/pageList' -Method Post -Body 'start=0&length=10' -WebSession $session -UseBasicParsing -TimeoutSec 10
    $execData = $execResp.Content | ConvertFrom-Json
    foreach ($item in $execData.data) {
        $regList = if ($item.registryList) { $item.registryList -join ', ' } else { '(empty)' }
        Write-Host ('[INFO] Executor: ' + $item.appname + ' | registry=' + $regList)
    }
} catch {
    Write-Host ('[FAIL] XXL-Job Admin: ' + $_.Exception.Message.Substring(0, [Math]::Min(100, $_.Exception.Message.Length)))
}

Write-Host ''
Write-Host '--- 4. Error Log Check ---'
$errLog = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs\sys-error.log'
if (Test-Path $errLog) {
    $lines = Get-Content $errLog
    $mwErrors = $lines | Where-Object { $_ -match 'kafka|Kafka|elasticsearch|Connection refused|Connection reset|session\.timeout|SocketTimeout' }
    if ($mwErrors -and $mwErrors.Count -gt 0) {
        Write-Host ('[WARN] Found ' + $mwErrors.Count + ' middleware-related errors')
        foreach ($e in $mwErrors | Select-Object -First 3) { Write-Host ('  ' + $e) }
    } else {
        Write-Host ('[PASS] No middleware connection errors (total error lines: ' + $lines.Count + ')')
    }
} else {
    Write-Host '[PASS] No error log (clean start)'
}

Write-Host ''
Write-Host '============================================'
Write-Host '  TEST SUMMARY'
Write-Host '============================================'
$pass = ($results | Where-Object { $_.Status -eq 'PASS' }).Count
$fail = ($results | Where-Object { $_.Status -eq 'FAIL' }).Count
$total = $results.Count
Write-Host ('Total: ' + $total + ' | PASS: ' + $pass + ' | FAIL: ' + $fail)
Write-Host ''
if ($fail -gt 0) {
    Write-Host 'Failed tests:'
    $results | Where-Object { $_.Status -eq 'FAIL' } | ForEach-Object {
        Write-Host ('  [FAIL] ' + $_.Name + ' | HTTP=' + $_.Code + ' | ' + $_.Msg.Substring(0, [Math]::Min(80, $_.Msg.Length)))
    }
}
