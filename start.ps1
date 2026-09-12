Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Mitti2Market — Automated Startup (PowerShell)" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$RootDir = $PSScriptRoot
if (-not $RootDir) { $RootDir = (Get-Location).Path }

# 1. Check Java prerequisite
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Java is not installed or not found in system PATH." -ForegroundColor Red
    Write-Host "Please install Java 17 or higher from: https://adoptium.net/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# 2. Check Node prerequisite
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Node.js is not installed or not found in system PATH." -ForegroundColor Red
    Write-Host "Please install Node.js 18 or higher from: https://nodejs.org/" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# 3. Ensure frontend dependencies are installed
if (-not (Test-Path "$RootDir\frontend\node_modules")) {
    Write-Host "[Setup] Installing frontend dependencies..." -ForegroundColor Yellow
    Start-Process -FilePath "npm" -ArgumentList "install" -WorkingDirectory "$RootDir\frontend" -Wait -NoNewWindow
}

# 4. Launch Backend
Write-Host "[1/2] Launching Backend service (Spring Boot on Port 8080)..." -ForegroundColor Green
$mvnCmd = if (Test-Path "$RootDir\backend\mvnw.cmd") { "mvnw.cmd" } else { "mvn" }
Start-Process -FilePath "cmd.exe" -ArgumentList "/k $mvnCmd spring-boot:run" -WorkingDirectory "$RootDir\backend"

Write-Host "Waiting 12 seconds for Backend initialization..." -ForegroundColor Gray
Start-Sleep -Seconds 12

# 5. Launch Frontend
Write-Host "[2/2] Launching Frontend service (Vite / React on Port 5173)..." -ForegroundColor Green
Start-Process -FilePath "cmd.exe" -ArgumentList "/k npm run dev" -WorkingDirectory "$RootDir\frontend"

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "  Mitti2Market services are running!" -ForegroundColor Green
Write-Host "  Frontend App: http://localhost:5173" -ForegroundColor White
Write-Host "  Backend API:  http://localhost:8080" -ForegroundColor White
Write-Host "  Swagger Docs: http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Opening application in browser..." -ForegroundColor Gray
Start-Sleep -Seconds 2
Start-Process "http://localhost:5173"
