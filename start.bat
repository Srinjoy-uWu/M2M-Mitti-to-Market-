@echo off
setlocal enabledelayedexpansion
REM ═══════════════════════════════════════════════════════════════
REM Mitti2Market — Start Script (Windows)
REM ═══════════════════════════════════════════════════════════════
REM Automatically starts both Backend and Frontend services.
REM Usage: double-click start.bat or run .\start.bat
REM ═══════════════════════════════════════════════════════════════

REM ─── Optional: Load backend .env if present ─────────────────────
if exist backend\.env (
    echo Loading backend\.env...
    for /f "usebackq tokens=1,* delims==" %%A in ("backend\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

REM ─── Optional: Load frontend .env if present ────────────────────
if exist frontend\.env (
    echo Loading frontend\.env...
    for /f "usebackq tokens=1,* delims==" %%A in ("frontend\.env") do (
        set "line=%%A"
        if not "!line:~0,1!"=="#" (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
)

if "%SERVER_PORT%"=="" set SERVER_PORT=8080

echo.
echo ===============================================================
echo   Mitti2Market Starting...
echo   Backend:  http://localhost:%SERVER_PORT%
echo   Frontend: http://localhost:5173
echo ===============================================================
echo.

REM ─── Ensure frontend dependencies exist ─────────────────────────
if not exist frontend\node_modules (
    echo [Frontend] Installing dependencies (first run)...
    cd frontend && call npm install && cd ..
)

REM ─── Start Backend ──────────────────────────────────────────────
echo Starting Backend...
if exist backend\mvnw.cmd (
    start "M2M-Backend" cmd /k "cd backend && mvnw.cmd spring-boot:run"
) else (
    start "M2M-Backend" cmd /k "cd backend && mvn spring-boot:run"
)

REM ─── Wait for backend ───────────────────────────────────────────
echo Waiting for backend to initialize (15 seconds)...
timeout /t 15 /nobreak >nul

REM ─── Start Frontend ─────────────────────────────────────────────
echo Starting Frontend...
start "M2M-Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo ===============================================================
echo   Both services started!
echo   Frontend: http://localhost:5173
echo   Backend:  http://localhost:%SERVER_PORT%
echo   Swagger:  http://localhost:%SERVER_PORT%/swagger-ui/index.html
echo.
echo   Keep the terminal windows open while using the application.
echo ===============================================================
pause
