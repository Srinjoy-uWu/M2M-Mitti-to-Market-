@echo off
REM ═══════════════════════════════════════════════════════════════
REM Mitti2Market — Automated Startup (Windows)
REM ═══════════════════════════════════════════════════════════════

REM Always ensure execution starts from the script's root directory
cd /d "%~dp0"

echo ===============================================================
echo   Mitti2Market — Starting System
echo ===============================================================
echo.

REM 1. Check Java prerequisite
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not found in your system PATH.
    echo Please install Java 17 or higher: https://adoptium.net/
    pause
    exit /b 1
)

REM 2. Check Node.js prerequisite
where node >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Node.js is not found in your system PATH.
    echo Please install Node.js 18 or higher: https://nodejs.org/
    pause
    exit /b 1
)

REM 3. Ensure frontend dependencies are installed
if not exist "frontend\node_modules" (
    echo [Setup] Installing frontend dependencies (first-time setup)...
    pushd frontend
    call npm install
    popd
    echo.
)

REM 4. Launch Backend Service
echo [1/2] Launching Backend service (Spring Boot)...
if exist "backend\mvnw.cmd" (
    start "Mitti2Market - Backend Server (Port 8080)" cmd /k "cd /d ""%~dp0backend"" && title Mitti2Market Backend && mvnw.cmd spring-boot:run"
) else (
    start "Mitti2Market - Backend Server (Port 8080)" cmd /k "cd /d ""%~dp0backend"" && title Mitti2Market Backend && mvn spring-boot:run"
)

REM 5. Wait for Backend initialization
echo Waiting for Backend service to initialize...
ping -n 12 127.0.0.1 >nul

REM 6. Launch Frontend Service
echo [2/2] Launching Frontend service (Vite / React)...
start "Mitti2Market - Frontend Server (Port 5173)" cmd /k "cd /d ""%~dp0frontend"" && title Mitti2Market Frontend && npm run dev"

echo.
echo ===============================================================
echo   Mitti2Market services are running!
echo.
echo   Frontend App: http://localhost:5173
echo   Backend API:  http://localhost:8080
echo   Swagger Docs: http://localhost:8080/swagger-ui/index.html
echo.
echo   Two terminal windows have been opened for the services.
echo   Keep them open while testing the application.
echo ===============================================================
echo.
echo Opening the web application in your default browser...
ping -n 3 127.0.0.1 >nul
start http://localhost:5173

