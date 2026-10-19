try {
    $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
    $t.Close()
    Write-Host '[UP] Backend :8080'
} catch {
    Write-Host '[DOWN] Backend :8080'
}
