#!/bin/bash
# ═══════════════════════════════════════════════════════════════
# Mitti2Market — Start Script (Linux/Mac)
# ═══════════════════════════════════════════════════════════════
# Loads backend/.env and frontend/.env, then starts both services.
# Usage: ./start.sh
# ═══════════════════════════════════════════════════════════════

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_ENV="$SCRIPT_DIR/backend/.env"
FRONTEND_ENV="$SCRIPT_DIR/frontend/.env"

# ─── Optional: Load backend .env ──────────────────────────────
if [ -f "$BACKEND_ENV" ]; then
    echo "📂 Loading backend/.env..."
    while IFS='=' read -r key value; do
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        value="${value#\"}"; value="${value%\"}"; value="${value#\'}"; value="${value%\'}"
        export "$key=$value"
    done < "$BACKEND_ENV"
fi

# ─── Optional: Load frontend .env ─────────────────────────────
if [ -f "$FRONTEND_ENV" ]; then
    echo "📂 Loading frontend/.env..."
    while IFS='=' read -r key value; do
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)
        value="${value#\"}"; value="${value%\"}"; value="${value#\'}"; value="${value%\'}"
        export "$key=$value"
    done < "$FRONTEND_ENV"
fi

echo "✅ Environment configured"
echo "   Backend:  http://localhost:${SERVER_PORT:-8080}"
echo "   Frontend: http://localhost:5173"
echo ""

# ─── Ensure frontend dependencies exist ───────────────────────
if [ ! -d "$SCRIPT_DIR/frontend/node_modules" ]; then
    echo "📦 Installing frontend dependencies..."
    cd "$SCRIPT_DIR/frontend" && npm install
fi

# ─── Start Backend ───────────────────────────────────────────
echo "🚀 Starting Backend..."
cd "$SCRIPT_DIR/backend"
if [ -x "./mvnw" ]; then
    ./mvnw spring-boot:run &
elif [ -f "./mvnw" ]; then
    sh ./mvnw spring-boot:run &
else
    mvn spring-boot:run &
fi
BACKEND_PID=$!

# ─── Wait for backend to be ready ────────────────────────────
echo "⏳ Waiting for backend..."
for i in $(seq 1 30); do
    if curl -s "http://localhost:${SERVER_PORT:-8080}/api/health" > /dev/null 2>&1; then
        echo "✅ Backend is ready!"
        break
    fi
    sleep 1
done

# ─── Start Frontend ─────────────────────────────────────────
echo "🚀 Starting Frontend..."
cd "$SCRIPT_DIR/frontend"
npm run dev &
FRONTEND_PID=$!

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "  Mitti2Market is running!"
echo ""
echo "  Frontend:  http://localhost:5173"
echo "  Backend:   http://localhost:${SERVER_PORT:-8080}"
echo ""
echo "  Press Ctrl+C to stop both services"
echo "═══════════════════════════════════════════════════════════"

# ─── Cleanup on exit ────────────────────────────────────────
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    kill $BACKEND_PID 2>/dev/null || true
    kill $FRONTEND_PID 2>/dev/null || true
    echo "✅ Done"
}
trap cleanup EXIT INT TERM

wait
