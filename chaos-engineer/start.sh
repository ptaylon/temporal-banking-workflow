#!/bin/bash

# 🌪️ Chaos Engineering Dashboard - Startup Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "============================================================"
echo "🌪️  CHAOS ENGINEERING DASHBOARD"
echo "============================================================"
echo ""

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "⚠️  Virtual environment not found!"
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
    echo "✅ Virtual environment created!"
fi

# Activate virtual environment
echo "🔌 Activating virtual environment..."
source venv/bin/activate

# Install dependencies
echo "📦 Checking dependencies..."
pip install -q -r requirements.txt

# Check if Docker is running
if ! docker ps > /dev/null 2>&1; then
    echo "⚠️  Docker is not running!"
    echo "⚠️  Some chaos features may not work without Docker."
    echo ""
fi

# Check if infrastructure is running
echo "🔍 Checking infrastructure status..."
POSTGRES_RUNNING=$(docker inspect -f '{{.State.Status}}' banking-postgres 2>/dev/null || echo "not found")
KAFKA_RUNNING=$(docker inspect -f '{{.State.Status}}' banking-kafka 2>/dev/null || echo "not found")
TEMPORAL_RUNNING=$(docker inspect -f '{{.State.Status}}' banking-temporal 2>/dev/null || echo "not found")

echo "  PostgreSQL: $POSTGRES_RUNNING"
echo "  Kafka: $KAFKA_RUNNING"
echo "  Temporal: $TEMPORAL_RUNNING"
echo ""

if [[ "$POSTGRES_RUNNING" != "running" ]] || [[ "$KAFKA_RUNNING" != "running" ]] || [[ "$TEMPORAL_RUNNING" != "running" ]]; then
    echo "⚠️  Some infrastructure components are not running!"
    echo "💡 To start infrastructure, run:"
    echo "   cd .. && docker-compose up -d"
    echo ""
fi

# Start the dashboard
echo "🚀 Starting Chaos Dashboard..."
echo ""
echo "============================================================"
echo "📡 Dashboard URL: http://localhost:5000"
echo "📡 API URL: http://localhost:5000/api"
echo "============================================================"
echo ""
echo "💡 Press Ctrl+C to stop the dashboard"
echo ""

python chaos_api.py
