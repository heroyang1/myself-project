# Check all ports and backend status
$ports = @(3306, 6379, 9200, 9092, 8888, 8080, 9999)
$names = @('MySQL', 'Redis', 'Elasticsearch', 'Kafka', 'XXL-Job Admin', 'RuoYi Backend', 'XXL-Job Executor')
Write-Host '========== PORT STATUS =========='
for ($i = 0; $i -lt $ports.Count; $i++) {
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', $ports[$i])
        Write-Host ('[UP]   ' + $names[$i] + ' (' + $ports[$i] + ')')
        $t.Close()
    } catch {
        Write-Host ('[DOWN] ' + $names[$i] + ' (' + $ports[$i] + ')')
    }
}

Write-Host ''
Write-Host '========== JAVA PROCESSES =========='
Get-Process -Name java -ErrorAction SilentlyContinue | ForEach-Object {
    $id = $_.Id
    $ws = [math]::Round($_.WorkingSet64 / 1MB, 0)
    $proc = Get-CimInstance Win32_Process -Filter "ProcessId=$id"
    $cmd = $proc.CommandLine
    if ($cmd -and $cmd.Length -gt 120) { $cmd = $cmd.Substring(0, 120) + '...' }
    Write-Host ('PID=' + $id + ' WS=' + $ws + 'MB CMD=' + $cmd)
}
