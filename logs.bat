@echo off
cd /d D:\Apps\docker-dev

echo Opening Docker logs in separate windows...

start "JAVA LOGS" cmd /k "docker compose logs -f java"
start "ANGULAR LOGS" cmd /k "docker compose logs -f angular"
start "PYTHON LOGS" cmd /k "docker compose logs -f python"
start "KAFKA LOGS" cmd /k "docker compose logs -f kafka"

echo Done!
pause