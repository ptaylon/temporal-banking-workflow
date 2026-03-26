.PHONY: install run dev clean help test docker-build docker-run lint

# Default target
all: install run

# Install dependencies
install:
	@echo "📦 Installing dependencies..."
	pip install -r requirements.txt
	@echo "✅ Dependencies installed!"

# Run the chaos dashboard
run:
	@echo "🌪️  Starting Chaos Engineering Dashboard..."
	@echo "📡 Dashboard URL: http://localhost:5000"
	@echo "📡 API URL: http://localhost:5000/api"
	@echo "📊 Metrics URL: http://localhost:5000/api/metrics"
	@echo ""
	python chaos_api.py

# Run in development mode with auto-reload
dev:
	@echo "🔧 Starting in development mode..."
	FLASK_ENV=development FLASK_DEBUG=1 python chaos_api.py

# Run with WebSocket support
run-ws:
	@echo "🌪️  Starting Chaos Dashboard with WebSocket..."
	WEBSOCKET_ENABLED=true python chaos_api.py

# Run with real network delay (Linux only, requires sudo)
run-real-network:
	@echo "🌪️  Starting Chaos Dashboard with real network delay..."
	@echo "⚠️  Requires sudo privileges for tc commands"
	USE_REAL_NETWORK_DELAY=true python chaos_api.py

# Create virtual environment
venv:
	@echo "🐍 Creating virtual environment..."
	python3 -m venv venv
	@echo "✅ Virtual environment created!"
	@echo "📝 Activate with: source venv/bin/activate"

# Run tests
test:
	@echo "🧪 Running tests..."
	pytest tests/ -v --cov=chaos_api --cov=network_chaos --cov-report=term-missing

# Run tests with coverage report
coverage:
	@echo "🧪 Running tests with coverage..."
	pytest tests/ -v --cov=chaos_api --cov=network_chaos --cov-report=html --cov-report=term-missing
	@echo "📊 Coverage report generated in htmlcov/index.html"

# Lint code
lint:
	@echo "🔍 Linting code..."
	@if command -v flake8 > /dev/null 2>&1; then \
		flake8 chaos_api.py network_chaos.py tests/; \
	else \
		echo "⚠️  flake8 not installed, skipping lint"; \
	fi

# Build Docker image
docker-build:
	@echo "🐳 Building Docker image..."
	docker build -t chaos-dashboard:latest .
	@echo "✅ Docker image built!"

# Run Docker container
docker-run:
	@echo "🐳 Running Docker container..."
	docker run -d \
		--name chaos-dashboard \
		--network host \
		-v /var/run/docker.sock:/var/run/docker.sock \
		-e FLASK_PORT=5000 \
		-e WEBSOCKET_ENABLED=false \
		chaos-dashboard:latest
	@echo "✅ Container started!"
	@echo "📊 Dashboard: http://localhost:5000"

# Stop Docker container
docker-stop:
	@echo "🛑 Stopping Docker container..."
	docker stop chaos-dashboard 2>/dev/null || true
	docker rm chaos-dashboard 2>/dev/null || true

# Docker Compose (for full stack)
docker-compose-up:
	@echo "🚀 Starting full stack with Docker Compose..."
	cd .. && docker-compose up -d

docker-compose-down:
	@echo "🛑 Stopping full stack..."
	cd .. && docker-compose down

# Clean up
clean:
	@echo "🧹 Cleaning up..."
	rm -rf __pycache__
	rm -rf *.pyc
	rm -rf .pytest_cache
	rm -rf htmlcov/
	rm -rf .coverage
	rm -rf logs/
	rm -rf pids/
	@echo "✅ Cleaned!"

# Check if docker is running
check-docker:
	@echo "🐳 Checking Docker..."
	@docker ps > /dev/null 2>&1 && echo "✅ Docker is running" || (echo "❌ Docker is not running" && exit 1)

# Start infrastructure for chaos testing
start-infra: check-docker
	@echo "🚀 Starting infrastructure..."
	cd .. && docker-compose up -d
	@echo "⏳ Waiting for services to be ready (30s)..."
	sleep 30
	@echo "✅ Infrastructure started!"

# Stop infrastructure
stop-infra:
	@echo "🛑 Stopping infrastructure..."
	cd .. && docker-compose stop
	@echo "✅ Infrastructure stopped!"

# Restart infrastructure
restart-infra: stop-infra start-infra

# Setup CDC connector
setup-cdc:
	@echo "🔄 Setting up CDC connector..."
	@curl -X POST http://localhost:8083/connectors \
		-H "Content-Type: application/json" \
		-d @../config/debezium-connector.json || echo "⚠️  CDC setup failed - make sure infrastructure is running"
	@echo "✅ CDC setup complete!"

# Full setup (first time use)
setup: venv install start-infra setup-cdc
	@echo ""
	@echo "🎉 Setup complete!"
	@echo "📝 Run 'make run' to start the Chaos Dashboard"

# Health check
health:
	@echo "🏥 Running health checks..."
	@echo -n "Docker: " && (docker ps > /dev/null 2>&1 && echo "✅" || echo "❌")
	@echo -n "Dashboard API: " && (curl -s http://localhost:5000/api/status > /dev/null 2>&1 && echo "✅" || echo "❌")
	@echo -n "Dashboard Health: " && (curl -s http://localhost:5000/api/health/detailed > /dev/null 2>&1 && echo "✅" || echo "❌")
	@echo -n "Metrics: " && (curl -s http://localhost:5000/api/metrics > /dev/null 2>&1 && echo "✅" || echo "❌")
	@echo -n "PostgreSQL: " && (docker inspect -f '{{.State.Status}}' banking-postgres 2>/dev/null | grep -q running && echo "✅" || echo "❌")
	@echo -n "Kafka: " && (docker inspect -f '{{.State.Status}}' banking-kafka 2>/dev/null | grep -q running && echo "✅" || echo "❌")
	@echo -n "Temporal: " && (docker inspect -f '{{.State.Status}}' banking-temporal 2>/dev/null | grep -q running && echo "✅" || echo "❌")

# Show help
help:
	@echo "🌪️  Chaos Engineering Dashboard - Makefile"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  install          Install Python dependencies"
	@echo "  run              Start the Chaos Dashboard"
	@echo "  dev              Start in development mode with auto-reload"
	@echo "  run-ws           Start with WebSocket support"
	@echo "  run-real-network Start with real network delay (Linux)"
	@echo "  venv             Create virtual environment"
	@echo "  test             Run tests"
	@echo "  coverage         Run tests with coverage report"
	@echo "  lint             Lint code"
	@echo "  docker-build     Build Docker image"
	@echo "  docker-run       Run Docker container"
	@echo "  docker-stop      Stop Docker container"
	@echo "  clean            Clean up temporary files"
	@echo "  check-docker     Check if Docker is running"
	@echo "  start-infra      Start Docker infrastructure"
	@echo "  stop-infra       Stop Docker infrastructure"
	@echo "  restart-infra    Restart Docker infrastructure"
	@echo "  setup-cdc        Setup Debezium CDC connector"
	@echo "  setup            Full setup (venv + install + infra + cdc)"
	@echo "  health           Run health checks on all components"
	@echo "  help             Show this help message"
	@echo ""
	@echo "Quick Start:"
	@echo "  1. make setup        # First time only"
	@echo "  2. make run          # Start dashboard"
	@echo "  3. Open http://localhost:5000"
	@echo ""
	@echo "Advanced:"
	@echo "  make run-ws          # With WebSocket for real-time updates"
	@echo "  make docker-build    # Build Docker image"
	@echo "  make test            # Run tests"
	@echo ""
