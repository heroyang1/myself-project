@echo off
title OSH Local Middleware - ES + Kafka + XXL-Job
echo ========================================
echo   OSH Local Middleware Launcher
echo ========================================
echo.

:: --- Start Elasticsearch ---
echo [1/3] Starting Elasticsearch 7.17.25...
start "Elasticsearch 7.17.25" /min cmd /c "C:\middleware\elasticsearch\elasticsearch-7.17.25\bin\elasticsearch.bat"
echo       ES launched

:: --- Start Kafka (requires JDK 17) ---
echo [2/3] Starting Kafka 4.3.1...
set KAFKA_JAVA_HOME=C:\middleware\jdk17\jdk17.0.20_12
set PATH=%KAFKA_JAVA_HOME%\bin;%PATH%
start "Kafka 4.3.1" /min cmd /c "C:\middleware\kafka\run-kafka.bat"
echo       Kafka launched

:: --- Start XXL-Job Admin (uses JDK 1.8) ---
echo [3/3] Starting XXL-Job Admin 2.4.0...
start "XXL-Job Admin 2.4.0" /min cmd /c "C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe -jar C:\middleware\xxl-job\xxl-job-2.4.0\xxl-job-admin\target\xxl-job-admin-2.4.0.jar"
echo       XXL-Job Admin launched

echo.
echo All 3 services started in minimized windows.
echo Waiting 30 seconds for initialization...
timeout /t 30 /nobreak >nul

:: --- Verify ---
echo.
echo ========================================
echo   Connectivity Check
echo ========================================

:: Check ES
powershell -Command "try { $null = Invoke-WebRequest -Uri 'http://localhost:9200' -UseBasicParsing -TimeoutSec 5; Write-Host '[OK] Elasticsearch (port 9200)' } catch { Write-Host '[FAIL] Elasticsearch (port 9200)' }"

:: Check Kafka
powershell -Command "try { $t = New-Object System.Net.Sockets.TcpClient('localhost', 9092); Write-Host '[OK] Kafka (port 9092)'; $t.Close() } catch { Write-Host '[FAIL] Kafka (port 9092)' }"

:: Check XXL-Job Admin
powershell -Command "try { $null = Invoke-WebRequest -Uri 'http://localhost:8888/xxl-job-admin' -UseBasicParsing -TimeoutSec 5; Write-Host '[OK] XXL-Job Admin (port 8888)' } catch { Write-Host '[FAIL] XXL-Job Admin (port 8888)' }"

echo.
echo Press any key to close this window (services stay running)
pause >nul
