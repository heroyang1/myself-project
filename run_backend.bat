@echo off
title OSH Backend
cd /d C:\Users\Admin1\IdeaProjects\osh-backend
"C:\Users\Admin1\.jdks\corretto-1.8.0_492\bin\java.exe" -Dname=backstage-admin.jar -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -jar backstage-admin\target\backstage-admin.jar --spring.profiles.active=local
pause
