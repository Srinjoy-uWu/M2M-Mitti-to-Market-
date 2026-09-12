@echo off
title Mitti2Market Launcher
cd /d "%~dp0"

echo ===============================================================
echo   Mitti2Market Launcher
echo ===============================================================
echo.

where java >nul 2>nul
if errorlevel 1 goto no_java

where node >nul 2>nul
if errorlevel 1 goto no_node

if exist frontend\node_modules goto skip_npm
echo [Setup] Installing frontend dependencies...
cd frontend
call npm install
cd ..
echo.

:skip_npm
echo [1/2] Starting Backend service on http://localhost:8080 ...
if exist "backend\mvnw.cmd" (
    start "Mitti2Market Backend" /d "%~dp0backend" cmd /k "mvnw.cmd spring-boot:run"
) else (
    start "Mitti2Market Backend" /d "%~dp0backend" cmd /k "mvn spring-boot:run"
)

echo Waiting for Backend service to initialize...
ping -n 12 127.0.0.1 >nul

echo [2/2] Starting Frontend service on http://localhost:5173 ...
start "Mitti2Market Frontend" /d "%~dp0frontend" cmd /k "npm run dev"

echo.
echo ===============================================================
echo   Mitti2Market services are running!
echo.
echo   Frontend App: http://localhost:5173
echo   Backend API:  http://localhost:8080
echo   Swagger Docs: http://localhost:8080/swagger-ui/index.html
echo.
echo   Keep the Backend and Frontend terminal windows open.
echo ===============================================================
echo.
echo Opening browser in 3 seconds...
ping -n 4 127.0.0.1 >nul
start http://localhost:5173

echo.
echo Launcher finished. You may keep this window open or close it.
pause
exit /b 0

:no_java
echo [ERROR] Java is not installed or not in your system PATH.
echo Please install Java 17 or higher from https://adoptium.net/
pause
exit /b 1

:no_node
echo [ERROR] Node.js is not installed or not in your system PATH.
echo Please install Node.js 18 or higher from https://nodejs.org/
pause
exit /b 1



