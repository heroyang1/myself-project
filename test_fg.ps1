# Start backend in foreground to capture full output
$java = 'C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe'
$jar = 'C:\Users\Admin1\IdeaProjects\osh-backend\backstage-admin\target\backstage-admin.jar'

# Clear old logs
$logDir = 'C:\Users\Admin1\IdeaProjects\osh-backend\logs'
if (Test-Path "$logDir\sys-info.log") { Remove-Item "$logDir\sys-info.log" -Force }
if (Test-Path "$logDir\sys-error.log") { Remove-Item "$logDir\sys-error.log" -Force }

Write-Host 'Starting backend (foreground, 90s timeout)...'
Write-Host ''

$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = $java
$psi.Arguments = '-Dname=backstage-admin.jar -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -jar ' + $jar + ' --spring.profiles.active=local'
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $false
$psi.RedirectStandardError = $false
$psi.CreateNoWindow = $false

$proc = [System.Diagnostics.Process]::Start($psi)

# Wait up to 90 seconds, checking for port 8080
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$portUp = $false
while ($stopwatch.ElapsedMilliseconds -lt 90000) {
    if ($proc.HasExited) {
        Write-Host ''
        Write-Host ('[CRASH] Process exited with code: ' + $proc.ExitCode)
        break
    }
    try {
        $t = New-Object System.Net.Sockets.TcpClient('localhost', 8080)
        $t.Close()
        if (-not $portUp) {
            $portUp = $true
            Write-Host ('[OK] Port 8080 UP at ' + [math]::Round($stopwatch.Elapsed.TotalSeconds, 0) + 's')
            # Keep waiting for "Started" message in log
            $startWait = 0
            while ($startWait -lt 30) {
                Start-Sleep -Seconds 2
                $startWait += 2
                if (Test-Path "$logDir\sys-info.log") {
                    $started = Select-String -Path "$logDir\sys-info.log" -Pattern 'Started RuoYiApplication' -SimpleMatch -ErrorAction SilentlyContinue
                    if ($started) {
                        Write-Host ''
                        Write-Host ('[OK] ' + $started.Line)
                        Write-Host '[OK] Backend fully started!'
                        exit 0
                    }
                }
                if ($proc.HasExited) {
                    Write-Host ''
                    Write-Host ('[CRASH] Process died after port bind, exit code: ' + $proc.ExitCode)
                    break
                }
            }
            if ($proc.HasExited) { break }
            Write-Host '[OK] Backend running (no Started line yet but port stable)'
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}

if (-not $proc.HasExited -and -not $portUp) {
    Write-Host '[TIMEOUT] Backend did not start in 90s'
    $proc.Kill()
}
