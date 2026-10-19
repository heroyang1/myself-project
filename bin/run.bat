@echo off
REM ============================================================
REM run.bat — 本地开发启动脚本（Windows）
REM
REM [WARNING] 本脚本仅用于本地开发环境，禁止在生产/测试服务器使用。
REM 生产环境统一使用 Docker 容器管理，由 CI/CD 流水线
REM (deploy-qa.yml / deploy-prod.yml) 远程执行 docker 命令。
REM ============================================================
echo.
echo [信息] 使用Jar命令运行Web工程。
echo.

cd %~dp0
cd ../backstage-admin/target

set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java -jar %JAVA_OPTS% backstage-admin.jar

cd bin
pause