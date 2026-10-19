# quick3.ps1
$wc = New-Object System.Net.WebClient
$wc.Encoding = [System.Text.Encoding]::UTF8

$endpoints = @(
    @('HP Seckill Hot', 'http://localhost:8080/pc/homepage/seckill/hot'),
    @('HP Book Hot', 'http://localhost:8080/pc/homepage/book/hot'),
    @('BBS Summary', 'http://localhost:8080/pc/bbs')
)

foreach ($ep in $endpoints) {
    try {
        $r = $wc.DownloadString($ep[1])
        $j = $r | ConvertFrom-Json
        if ($j.code -eq 200) {
            Write-Host "[PASS] $($ep[0])" -ForegroundColor Green
        } else {
            $msg = $j.msg
            if ($msg -and $msg.Length -gt 100) { $msg = $msg.Substring(0,100) + '...' }
            Write-Host "[FAIL] $($ep[0]) - code=$($j.code) msg=$msg" -ForegroundColor Red
        }
    } catch {
        $msg = $_.Exception.Message
        if ($msg.Length -gt 100) { $msg = $msg.Substring(0,100) + '...' }
        Write-Host "[FAIL] $($ep[0]) - $msg" -ForegroundColor Red
    }
}
