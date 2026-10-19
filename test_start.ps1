# Start backend
$java = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$jar = 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\backstage-admin.jar'

Write-Host 'Starting backend...'
Start-Process -FilePath $java -ArgumentList @(
    '-Dname=backstage-admin.jar',
    '-Duser.timezone=Asia/Shanghai',
    '-Xms512m', '-Xmx1024m',
    '-XX:MetaspaceSize=128m', '-XX:MaxMetaspaceSize=512m',
    '-XX:+HeapDumpOnOutOfMemoryError',
    '-jar', $jar,
    '--spring.profiles.active=local'
) -WindowStyle Minimized

# Wait for startup
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 3
    $secs = ($i + 1) * 3
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
        $t.Close()
        Write-Host ('[OK] Backend UP after ' + $secs + 's')
        exit 0
    } catch {
        if ($secs % 15 -eq 0) { Write-Host ('  Waiting... ' + $secs + 's') }
    }
}
Write-Host '[FAIL] Backend did not start'
exit 1
