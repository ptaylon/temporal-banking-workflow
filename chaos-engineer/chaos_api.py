#!/usr/bin/env python3
"""
Chaos Engineering API for Temporal Banking Workflow
Controls chaos experiments for testing Temporal's resilience

Enhanced version with:
- Real network delay using tc (Linux) or pfctl (macOS)
- Better health checks with infrastructure validation
- WebSocket support for real-time updates
- Prometheus metrics export
- Improved error handling
"""

import os
import sys
import subprocess
import threading
import time
import json
import socket
import logging
from datetime import datetime
from functools import wraps
from flask import Flask, jsonify, request, send_from_directory, Response
from flask_cors import CORS
from flask_socketio import SocketIO, emit
import requests
import psutil

# Load environment variables
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # python-dotenv not installed

# Import Prometheus
try:
    from prometheus_client import Counter, Gauge, Histogram, generate_latest, CONTENT_TYPE_LATEST
    PROMETHEUS_AVAILABLE = True
except ImportError:
    PROMETHEUS_AVAILABLE = False

# Import network chaos module
from network_chaos import get_network_chaos, NetworkChaos

# Configure logging
logging.basicConfig(
    level=os.getenv('LOG_LEVEL', 'INFO'),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__, static_folder='static')
CORS(app)

# WebSocket configuration
WEBSOCKET_ENABLED = os.getenv('WEBSOCKET_ENABLED', 'false').lower() == 'true'
if WEBSOCKET_ENABLED:
    socketio = SocketIO(app, cors_allowed_origins="*", async_mode='threading')
else:
    socketio = None

# Global state for chaos experiments
CHAOS_STATE = {
    'database_down': False,
    'kafka_down': False,
    'cdc_down': False,
    'temporal_down': False,
    'network_delay_ms': 0,
    'packet_loss_percent': 0,
    'services_stopped': [],
    'cpu_stress': False,
    'memory_stress': False,
    'disk_io_stress': False,
    'chaos_mode_active': False,
    'experiments_log': [],
    'docker_available': False,
    'real_network_delay_available': False
}

# Service ports configuration
SERVICES = {
    'account-service': {'port': 8081, 'container': 'banking-postgres'},
    'transfer-service': {'port': 8082, 'container': 'banking-postgres'},
    'validation-service': {'port': 8087, 'container': None},
    'notification-service': {'port': 8086, 'container': 'banking-kafka'},
    'audit-service': {'port': 8085, 'container': 'banking-audit-postgres'},
}

INFRASTRUCTURE = {
    'postgres': {'container': 'banking-postgres', 'port': 5432, 'host': 'localhost'},
    'audit-postgres': {'container': 'banking-audit-postgres', 'port': 5433, 'host': 'localhost'},
    'kafka': {'container': 'banking-kafka', 'port': 9092, 'host': 'localhost'},
    'zookeeper': {'container': 'banking-zookeeper', 'port': 2181, 'host': 'localhost'},
    'temporal': {'container': 'banking-temporal', 'port': 7233, 'host': 'localhost'},
    'temporal-ui': {'container': 'banking-temporal-ui', 'port': 8088, 'host': 'localhost'},
    'debezium': {'container': 'banking-debezium-connect', 'port': 8083, 'host': 'localhost'},
    'opensearch': {'container': 'banking-opensearch', 'port': 9200, 'host': 'localhost'},
    'kafka-ui': {'container': 'banking-kafka-ui', 'port': 8090, 'host': 'localhost'},
}

# ============== Prometheus Metrics ==============
if PROMETHEUS_AVAILABLE:
    EXPERIMENT_COUNTER = Counter(
        'chaos_experiments_total',
        'Total number of chaos experiments',
        ['type', 'target', 'status']
    )
    
    ACTIVE_CHAOS_GAUGE = Gauge(
        'chaos_active_count',
        'Number of active chaos experiments'
    )
    
    NETWORK_DELAY_GAUGE = Gauge(
        'chaos_network_delay_ms',
        'Current network delay in milliseconds'
    )
    
    PACKET_LOSS_GAUGE = Gauge(
        'chaos_packet_loss_percent',
        'Current packet loss percentage'
    )
    
    SERVICE_HEALTH_GAUGE = Gauge(
        'chaos_service_health',
        'Service health status',
        ['service']
    )
    
    INFRASTRUCTURE_HEALTH_GAUGE = Gauge(
        'chaos_infrastructure_health',
        'Infrastructure health status',
        ['component']
    )
    
    EXPERIMENT_DURATION_HISTOGRAM = Histogram(
        'chaos_experiment_duration_seconds',
        'Duration of chaos experiments',
        buckets=(0.1, 0.5, 1.0, 5.0, 10.0, 30.0, 60.0)
    )

# ============== Helper Functions ==============

def log_experiment(action, target, status, details=None):
    """Log chaos experiment and emit WebSocket event"""
    entry = {
        'timestamp': datetime.now().isoformat(),
        'action': action,
        'target': target,
        'status': status,
        'details': details or {}
    }
    CHAOS_STATE['experiments_log'].append(entry)
    
    # Keep only last 100 experiments
    if len(CHAOS_STATE['experiments_log']) > 100:
        CHAOS_STATE['experiments_log'] = CHAOS_STATE['experiments_log'][-100:]
    
    # Update Prometheus metrics
    if PROMETHEUS_AVAILABLE:
        EXPERIMENT_COUNTER.labels(type=action, target=target, status=status).inc()
        ACTIVE_CHAOS_GAUGE.set(sum([
            CHAOS_STATE['database_down'],
            CHAOS_STATE['kafka_down'],
            CHAOS_STATE['cdc_down'],
            CHAOS_STATE['temporal_down']
        ]))
        NETWORK_DELAY_GAUGE.set(CHAOS_STATE['network_delay_ms'])
        PACKET_LOSS_GAUGE.set(CHAOS_STATE['packet_loss_percent'])
    
    # Emit WebSocket event
    if socketio and WEBSOCKET_ENABLED:
        socketio.emit('experiment_log', entry)
    
    return entry

def run_docker_command(command, timeout=30):
    """Run docker command and return result with improved error handling"""
    try:
        result = subprocess.run(
            command,
            shell=True,
            capture_output=True,
            text=True,
            timeout=timeout
        )
        return {
            'success': result.returncode == 0,
            'stdout': result.stdout,
            'stderr': result.stderr,
            'returncode': result.returncode
        }
    except subprocess.TimeoutExpired:
        logger.error(f"Docker command timed out: {command}")
        return {'success': False, 'error': 'Command timeout'}
    except FileNotFoundError:
        logger.error("Docker command not found - is Docker installed?")
        return {'success': False, 'error': 'Docker not found'}
    except PermissionError as e:
        logger.error(f"Permission denied: {e}")
        return {'success': False, 'error': f'Permission denied: {e}'}
    except Exception as e:
        logger.error(f"Docker command failed: {e}")
        return {'success': False, 'error': str(e)}

def check_docker_available():
    """Check if Docker is available and running"""
    try:
        result = subprocess.run(
            ['docker', 'ps'],
            capture_output=True,
            text=True,
            timeout=5
        )
        return result.returncode == 0
    except Exception:
        return False

def check_container_status(container_name):
    """Check if container is running"""
    result = run_docker_command(f'docker inspect -f \'{{{{.State.Status}}}}\' {container_name}')
    if result['success']:
        return result['stdout'].strip() == 'running'
    return False

def check_container_health(container_name):
    """Check container health status"""
    result = run_docker_command(
        f'docker inspect -f \'{{{{.State.Health.Status}}}}\' {container_name} 2>/dev/null || echo "no-healthcheck"'
    )
    if result['success']:
        status = result['stdout'].strip()
        return status if status else 'unknown'
    return 'unknown'

def check_port_open(host, port, timeout=2):
    """Check if a TCP port is open"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        result = sock.connect_ex((host, port))
        sock.close()
        return result == 0
    except Exception:
        return False

def check_database_connection(host, port, database, user='postgres'):
    """Check database connectivity"""
    try:
        # Try TCP connection first
        if not check_port_open(host, port):
            return {'connected': False, 'error': 'Port not reachable'}
        
        # Try psql if available
        result = subprocess.run(
            f'PGPASSWORD=postgres psql -h {host} -p {port} -U {user} -d {database} -c "SELECT 1" 2>&1',
            capture_output=True,
            text=True,
            timeout=5
        )
        if result.returncode == 0:
            return {'connected': True}
        else:
            return {'connected': True, 'note': 'TCP OK, psql not available or failed'}
    except subprocess.TimeoutExpired:
        return {'connected': False, 'error': 'Connection timeout'}
    except Exception as e:
        return {'connected': False, 'error': str(e)}

def check_kafka_broker(host, port):
    """Check Kafka broker connectivity"""
    try:
        if not check_port_open(host, port):
            return {'connected': False, 'error': 'Port not reachable'}
        return {'connected': True}
    except Exception as e:
        return {'connected': False, 'error': str(e)}

def check_temporal_connection(host, port):
    """Check Temporal server connectivity"""
    try:
        if not check_port_open(host, port):
            return {'connected': False, 'error': 'Port not reachable'}
        return {'connected': True}
    except Exception as e:
        return {'connected': False, 'error': str(e)}

def get_project_root():
    """Get project root directory"""
    return os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def error_handler(f):
    """Decorator for error handling in API endpoints"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        try:
            return f(*args, **kwargs)
        except Exception as e:
            logger.error(f"Error in {f.__name__}: {e}")
            return jsonify({
                'error': str(e),
                'type': type(e).__name__
            }), 500
    return decorated_function

def emit_status_update():
    """Emit status update via WebSocket"""
    if socketio and WEBSOCKET_ENABLED:
        status = get_detailed_status()
        socketio.emit('status_update', status)

# ============== Routes ==============

@app.route('/')
def index():
    """Serve the dashboard"""
    return send_from_directory('static', 'index.html')

@app.route('/api/status')
@error_handler
def get_status():
    """Get current chaos state and system status"""
    return jsonify(get_detailed_status())

def get_detailed_status():
    """Get detailed system status"""
    status = {
        'chaos_state': CHAOS_STATE.copy(),
        'infrastructure': {},
        'services': {},
        'docker_available': check_docker_available(),
        'timestamp': datetime.now().isoformat()
    }

    # Check infrastructure status with detailed health
    for name, config in INFRASTRUCTURE.items():
        container_running = check_container_status(config['container'])
        port_open = check_port_open(config['host'], config['port'])
        health = check_container_health(config['container'])
        
        status['infrastructure'][name] = {
            'running': container_running,
            'port_open': port_open,
            'health': health,
            'container': config['container'],
            'port': config['port']
        }
        
        # Update Prometheus
        if PROMETHEUS_AVAILABLE:
            INFRASTRUCTURE_HEALTH_GAUGE.labels(component=name).set(
                1 if container_running else 0
            )

    # Check service health
    for name, config in SERVICES.items():
        healthy = False
        port_open = False
        
        if config['port']:
            port_open = check_port_open('localhost', config['port'])
            if port_open:
                try:
                    response = requests.get(
                        f'http://localhost:{config["port"]}/actuator/health',
                        timeout=2
                    )
                    healthy = response.status_code == 200
                except:
                    healthy = False
        
        status['services'][name] = {
            'healthy': healthy,
            'port_open': port_open,
            'port': config['port']
        }
        
        # Update Prometheus
        if PROMETHEUS_AVAILABLE:
            SERVICE_HEALTH_GAUGE.labels(service=name).set(1 if healthy else 0)

    return status

@app.route('/api/health/detailed')
@error_handler
def get_detailed_health():
    """Get detailed health check with infrastructure validation"""
    health = {
        'overall': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'components': {},
        'issues': []
    }
    
    # Check Docker
    docker_available = check_docker_available()
    health['components']['docker'] = {
        'status': 'healthy' if docker_available else 'unhealthy',
        'details': 'Docker daemon is running' if docker_available else 'Docker daemon not available'
    }
    if not docker_available:
        health['issues'].append('Docker daemon not available')
        health['overall'] = 'unhealthy'
    
    # Check databases
    for db_name, db_config in [('postgres', INFRASTRUCTURE['postgres']), 
                                ('audit-postgres', INFRASTRUCTURE['audit-postgres'])]:
        result = check_database_connection(
            db_config['host'], 
            db_config['port'], 
            'banking_demo' if 'audit' not in db_name else 'audit_db'
        )
        health['components'][db_name] = {
            'status': 'healthy' if result.get('connected') else 'unhealthy',
            'details': result
        }
        if not result.get('connected'):
            health['issues'].append(f'{db_name}: {result.get("error", "Not connected")}')
            health['overall'] = 'degraded' if health['overall'] == 'healthy' else health['overall']
    
    # Check Kafka
    kafka_result = check_kafka_broker(
        INFRASTRUCTURE['kafka']['host'],
        INFRASTRUCTURE['kafka']['port']
    )
    health['components']['kafka'] = {
        'status': 'healthy' if kafka_result.get('connected') else 'unhealthy',
        'details': kafka_result
    }
    if not kafka_result.get('connected'):
        health['issues'].append(f'Kafka: {kafka_result.get("error", "Not connected")}')
        health['overall'] = 'degraded' if health['overall'] == 'healthy' else health['overall']
    
    # Check Temporal
    temporal_result = check_temporal_connection(
        INFRASTRUCTURE['temporal']['host'],
        INFRASTRUCTURE['temporal']['port']
    )
    health['components']['temporal'] = {
        'status': 'healthy' if temporal_result.get('connected') else 'unhealthy',
        'details': temporal_result
    }
    if not temporal_result.get('connected'):
        health['issues'].append(f'Temporal: {temporal_result.get("error", "Not connected")}')
        health['overall'] = 'degraded' if health['overall'] == 'healthy' else health['overall']
    
    # Check services
    services_healthy = 0
    services_total = len(SERVICES)
    for name, config in SERVICES.items():
        if config['port'] and check_port_open('localhost', config['port']):
            try:
                response = requests.get(
                    f'http://localhost:{config["port"]}/actuator/health',
                    timeout=2
                )
                if response.status_code == 200:
                    services_healthy += 1
            except:
                pass
    
    health['components']['services'] = {
        'status': 'healthy' if services_healthy == services_total else 'degraded',
        'healthy_count': services_healthy,
        'total_count': services_total
    }
    
    if services_healthy < services_total:
        health['overall'] = 'degraded' if health['overall'] == 'healthy' else health['overall']
    
    status_code = 200 if health['overall'] == 'healthy' else 503
    return jsonify(health), status_code

@app.route('/api/metrics')
def get_metrics():
    """Prometheus metrics endpoint"""
    if not PROMETHEUS_AVAILABLE:
        return jsonify({'error': 'Prometheus client not installed'}), 503
    
    # Update gauges before exporting
    if PROMETHEUS_AVAILABLE:
        ACTIVE_CHAOS_GAUGE.set(sum([
            CHAOS_STATE['database_down'],
            CHAOS_STATE['kafka_down'],
            CHAOS_STATE['cdc_down'],
            CHAOS_STATE['temporal_down']
        ]))
        NETWORK_DELAY_GAUGE.set(CHAOS_STATE['network_delay_ms'])
        PACKET_LOSS_GAUGE.set(CHAOS_STATE['packet_loss_percent'])
    
    return Response(generate_latest(), mimetype=CONTENT_TYPE_LATEST)

@app.route('/api/chaos/database', methods=['POST'])
@error_handler
def toggle_database():
    """Toggle database availability"""
    action = request.json.get('action', 'toggle')

    if action == 'down':
        result = run_docker_command('docker stop banking-postgres banking-audit-postgres')
        if result['success']:
            CHAOS_STATE['database_down'] = True
            log_experiment('STOP', 'database', 'success', {
                'containers': ['banking-postgres', 'banking-audit-postgres']
            })
            emit_status_update()
            return jsonify({'status': 'Database stopped', 'success': True})
        else:
            log_experiment('STOP', 'database', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to stop database', 'success': False, 'error': result.get('error')}), 500

    elif action == 'up':
        result = run_docker_command('docker start banking-postgres banking-audit-postgres')
        if result['success']:
            CHAOS_STATE['database_down'] = False
            log_experiment('START', 'database', 'success', {
                'containers': ['banking-postgres', 'banking-audit-postgres']
            })
            emit_status_update()
            return jsonify({'status': 'Database started', 'success': True})
        else:
            log_experiment('START', 'database', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to start database', 'success': False, 'error': result.get('error')}), 500

    else:  # toggle
        if CHAOS_STATE['database_down']:
            return toggle_database(action='up')
        else:
            return toggle_database(action='down')

@app.route('/api/chaos/kafka', methods=['POST'])
@error_handler
def toggle_kafka():
    """Toggle Kafka availability"""
    action = request.json.get('action', 'toggle')

    if action == 'down':
        result = run_docker_command('docker stop banking-kafka banking-zookeeper banking-kafka-ui')
        if result['success']:
            CHAOS_STATE['kafka_down'] = True
            log_experiment('STOP', 'kafka', 'success', {
                'containers': ['banking-kafka', 'banking-zookeeper', 'banking-kafka-ui']
            })
            emit_status_update()
            return jsonify({'status': 'Kafka cluster stopped', 'success': True})
        else:
            log_experiment('STOP', 'kafka', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to stop Kafka', 'success': False, 'error': result.get('error')}), 500

    elif action == 'up':
        result = run_docker_command('docker start banking-zookeeper banking-kafka banking-kafka-ui')
        if result['success']:
            CHAOS_STATE['kafka_down'] = False
            log_experiment('START', 'kafka', 'success', {
                'containers': ['banking-kafka', 'banking-zookeeper', 'banking-kafka-ui']
            })
            emit_status_update()
            return jsonify({'status': 'Kafka cluster started', 'success': True})
        else:
            log_experiment('START', 'kafka', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to start Kafka', 'success': False, 'error': result.get('error')}), 500

    else:
        if CHAOS_STATE['kafka_down']:
            return toggle_kafka(action='up')
        else:
            return toggle_kafka(action='down')

@app.route('/api/chaos/cdc', methods=['POST'])
@error_handler
def toggle_cdc():
    """Toggle CDC (Debezium) availability"""
    action = request.json.get('action', 'toggle')

    if action == 'down':
        result = run_docker_command('docker stop banking-debezium-connect')
        if result['success']:
            CHAOS_STATE['cdc_down'] = True
            log_experiment('STOP', 'cdc', 'success', {'container': 'banking-debezium-connect'})
            emit_status_update()
            return jsonify({'status': 'CDC stopped', 'success': True})
        else:
            log_experiment('STOP', 'cdc', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to stop CDC', 'success': False, 'error': result.get('error')}), 500

    elif action == 'up':
        result = run_docker_command('docker start banking-debezium-connect')
        if result['success']:
            CHAOS_STATE['cdc_down'] = False
            log_experiment('START', 'cdc', 'success', {'container': 'banking-debezium-connect'})
            emit_status_update()
            return jsonify({'status': 'CDC started', 'success': True})
        else:
            log_experiment('START', 'cdc', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to start CDC', 'success': False, 'error': result.get('error')}), 500

    else:
        if CHAOS_STATE['cdc_down']:
            return toggle_cdc(action='up')
        else:
            return toggle_cdc(action='down')

@app.route('/api/chaos/temporal', methods=['POST'])
@error_handler
def toggle_temporal():
    """Toggle Temporal availability"""
    action = request.json.get('action', 'toggle')

    if action == 'down':
        result = run_docker_command('docker stop banking-temporal banking-temporal-ui banking-temporal-admin-tools')
        if result['success']:
            CHAOS_STATE['temporal_down'] = True
            log_experiment('STOP', 'temporal', 'success', {
                'containers': ['banking-temporal', 'banking-temporal-ui', 'banking-temporal-admin-tools']
            })
            emit_status_update()
            return jsonify({'status': 'Temporal stopped', 'success': True})
        else:
            log_experiment('STOP', 'temporal', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to stop Temporal', 'success': False, 'error': result.get('error')}), 500

    elif action == 'up':
        result = run_docker_command('docker start banking-temporal banking-temporal-ui banking-temporal-admin-tools')
        if result['success']:
            CHAOS_STATE['temporal_down'] = False
            log_experiment('START', 'temporal', 'success', {
                'containers': ['banking-temporal', 'banking-temporal-ui', 'banking-temporal-admin-tools']
            })
            emit_status_update()
            return jsonify({'status': 'Temporal started', 'success': True})
        else:
            log_experiment('START', 'temporal', 'failed', {'error': result.get('error', 'Unknown')})
            return jsonify({'status': 'Failed to start Temporal', 'success': False, 'error': result.get('error')}), 500

    else:
        if CHAOS_STATE['temporal_down']:
            return toggle_temporal(action='up')
        else:
            return toggle_temporal(action='down')

@app.route('/api/chaos/delay', methods=['POST'])
@error_handler
def set_network_delay():
    """Set network delay using real tc commands or simulated"""
    delay_ms = request.json.get('delay_ms', 0)
    use_real = os.getenv('USE_REAL_NETWORK_DELAY', 'false').lower() == 'true'
    
    network_chaos = None
    if use_real:
        try:
            network_chaos = get_network_chaos()
            result = network_chaos.add_delay(delay_ms)
            if result['success']:
                CHAOS_STATE['network_delay_ms'] = delay_ms
                CHAOS_STATE['real_network_delay_available'] = True
                log_experiment('SET_DELAY_REAL', 'network', 'success', {'delay_ms': delay_ms})
                emit_status_update()
                return jsonify({
                    'status': f'Real network delay set to {delay_ms}ms',
                    'delay_ms': delay_ms,
                    'real': True
                })
            else:
                logger.warning(f"Real delay failed: {result.get('error', 'Unknown')}, falling back to simulated")
        except Exception as e:
            logger.warning(f"Real delay exception: {e}, falling back to simulated")
    
    # Fallback to simulated delay
    CHAOS_STATE['network_delay_ms'] = delay_ms
    CHAOS_STATE['real_network_delay_available'] = False
    log_experiment('SET_DELAY', 'network', 'success', {'delay_ms': delay_ms})
    emit_status_update()
    
    return jsonify({
        'status': f'Network delay set to {delay_ms}ms (simulated)',
        'delay_ms': delay_ms,
        'real': False
    })

@app.route('/api/chaos/packet-loss', methods=['POST'])
@error_handler
def set_packet_loss():
    """Set packet loss percentage using real tc commands or simulated"""
    loss_percent = request.json.get('loss_percent', 0)
    use_real = os.getenv('USE_REAL_NETWORK_DELAY', 'false').lower() == 'true'
    
    network_chaos = None
    if use_real:
        try:
            network_chaos = get_network_chaos()
            result = network_chaos.add_packet_loss(loss_percent)
            if result['success']:
                CHAOS_STATE['packet_loss_percent'] = loss_percent
                log_experiment('SET_PACKET_LOSS_REAL', 'network', 'success', {'loss_percent': loss_percent})
                emit_status_update()
                return jsonify({
                    'status': f'Real packet loss set to {loss_percent}%',
                    'loss_percent': loss_percent,
                    'real': True
                })
            else:
                logger.warning(f"Real packet loss failed: {result.get('error', 'Unknown')}, falling back to simulated")
        except Exception as e:
            logger.warning(f"Real packet loss exception: {e}, falling back to simulated")
    
    # Fallback to simulated
    CHAOS_STATE['packet_loss_percent'] = loss_percent
    log_experiment('SET_PACKET_LOSS', 'network', 'success', {'loss_percent': loss_percent})
    emit_status_update()
    
    return jsonify({
        'status': f'Packet loss set to {loss_percent}% (simulated)',
        'loss_percent': loss_percent,
        'real': False
    })

@app.route('/api/chaos/network/status', methods=['GET'])
@error_handler
def get_network_status():
    """Get real network chaos status"""
    use_real = os.getenv('USE_REAL_NETWORK_DELAY', 'false').lower() == 'true'
    
    status = {
        'use_real': use_real,
        'simulated': {
            'delay_ms': CHAOS_STATE['network_delay_ms'],
            'packet_loss_percent': CHAOS_STATE['packet_loss_percent']
        }
    }
    
    if use_real:
        try:
            network_chaos = get_network_chaos()
            real_status = network_chaos.get_status()
            status['real'] = real_status
        except Exception as e:
            status['real'] = {'error': str(e)}
    
    return jsonify(status)

@app.route('/api/chaos/service', methods=['POST'])
@error_handler
def control_service():
    """Start/stop individual Java services"""
    service_name = request.json.get('service')
    action = request.json.get('action', 'toggle')

    if service_name not in SERVICES:
        return jsonify({'error': f'Unknown service: {service_name}'}), 400

    pid_file = os.path.join(get_project_root(), 'pids', f'{service_name}.pid')

    if action == 'stop':
        if os.path.exists(pid_file):
            with open(pid_file, 'r') as f:
                pid = int(f.read().strip())
            try:
                os.kill(pid, 9)
                os.remove(pid_file)
                if service_name not in CHAOS_STATE['services_stopped']:
                    CHAOS_STATE['services_stopped'].append(service_name)
                log_experiment('STOP', 'service', 'success', {'service': service_name, 'pid': pid})
                emit_status_update()
                return jsonify({'status': f'{service_name} stopped', 'success': True})
            except ProcessLookupError:
                os.remove(pid_file)
                log_experiment('STOP', 'service', 'success', {'service': service_name, 'note': 'Process not found'})
                return jsonify({'status': f'{service_name} was not running', 'success': True})
        else:
            return jsonify({'status': f'{service_name} was not running', 'success': True})

    elif action == 'start':
        jar_path = os.path.join(get_project_root(), service_name, 'target', f'{service_name}-1.0-SNAPSHOT.jar')
        if os.path.exists(jar_path):
            # Start service in background
            log_file = os.path.join(get_project_root(), 'logs', f'{service_name}.log')
            os.makedirs(os.path.dirname(log_file), exist_ok=True)

            cmd = f'nohup java -jar {jar_path} > {log_file} 2>&1 &'
            subprocess.run(cmd, shell=True)

            if service_name in CHAOS_STATE['services_stopped']:
                CHAOS_STATE['services_stopped'].remove(service_name)
            log_experiment('START', 'service', 'success', {'service': service_name})
            emit_status_update()
            return jsonify({'status': f'{service_name} starting...', 'success': True})
        else:
            return jsonify({'error': f'JAR not found: {jar_path}'}), 404

    else:  # toggle
        if os.path.exists(pid_file):
            return control_service(service_name=service_name, action='stop')
        else:
            return control_service(service_name=service_name, action='start')

@app.route('/api/chaos/docker', methods=['POST'])
@error_handler
def control_docker():
    """Control Docker containers"""
    action = request.json.get('action')
    container = request.json.get('container')

    if action == 'stop-all':
        result = run_docker_command('docker-compose stop')
        log_experiment('STOP_ALL', 'docker', 'success' if result['success'] else 'failed')
        emit_status_update()
        return jsonify({'status': 'All containers stopped', 'success': result['success']})

    elif action == 'start-all':
        result = run_docker_command('docker-compose start')
        log_experiment('START_ALL', 'docker', 'success' if result['success'] else 'failed')
        emit_status_update()
        return jsonify({'status': 'All containers started', 'success': result['success']})

    elif action == 'restart':
        if container:
            result = run_docker_command(f'docker restart {container}')
            log_experiment('RESTART', 'docker', 'success' if result['success'] else 'failed', {'container': container})
            emit_status_update()
            return jsonify({'status': f'{container} restarted', 'success': result['success']})

    elif action == 'stop':
        if container:
            result = run_docker_command(f'docker stop {container}')
            log_experiment('STOP', 'docker', 'success' if result['success'] else 'failed', {'container': container})
            emit_status_update()
            return jsonify({'status': f'{container} stopped', 'success': result['success']})

    elif action == 'start':
        if container:
            result = run_docker_command(f'docker start {container}')
            log_experiment('START', 'docker', 'success' if result['success'] else 'failed', {'container': container})
            emit_status_update()
            return jsonify({'status': f'{container} started', 'success': result['success']})

    elif action == 'kill-random':
        import random
        containers = list(INFRASTRUCTURE.keys())
        selected = random.choice(containers)
        container_name = INFRASTRUCTURE[selected]['container']
        result = run_docker_command(f'docker stop {container_name}')
        log_experiment('KILL_RANDOM', 'docker', 'success' if result['success'] else 'failed', {'container': container_name})
        emit_status_update()
        return jsonify({'status': f'Killed {selected} ({container_name})', 'success': result['success']})

    return jsonify({'error': 'Invalid action'}), 400

@app.route('/api/chaos/stress', methods=['POST'])
@error_handler
def stress_system():
    """Apply system stress"""
    stress_type = request.json.get('type')
    action = request.json.get('action', 'start')

    if stress_type == 'cpu':
        if action == 'start':
            CHAOS_STATE['cpu_stress'] = True
            def cpu_stress():
                while CHAOS_STATE['cpu_stress']:
                    pass  # Busy wait
            thread = threading.Thread(target=cpu_stress, daemon=True)
            thread.start()
            log_experiment('START_STRESS', 'cpu', 'success')
            return jsonify({'status': 'CPU stress started'})
        else:
            CHAOS_STATE['cpu_stress'] = False
            log_experiment('STOP_STRESS', 'cpu', 'success')
            return jsonify({'status': 'CPU stress stopped'})

    elif stress_type == 'memory':
        if action == 'start':
            CHAOS_STATE['memory_stress'] = True
            log_experiment('START_STRESS', 'memory', 'success')
            return jsonify({'status': 'Memory stress started (simulated)'})
        else:
            CHAOS_STATE['memory_stress'] = False
            log_experiment('STOP_STRESS', 'memory', 'success')
            return jsonify({'status': 'Memory stress stopped'})

    elif stress_type == 'disk':
        if action == 'start':
            CHAOS_STATE['disk_io_stress'] = True
            log_experiment('START_STRESS', 'disk', 'success')
            return jsonify({'status': 'Disk I/O stress started (simulated)'})
        else:
            CHAOS_STATE['disk_io_stress'] = False
            log_experiment('STOP_STRESS', 'disk', 'success')
            return jsonify({'status': 'Disk I/O stress stopped'})

    return jsonify({'error': 'Invalid stress type'}), 400

@app.route('/api/chaos/scenario', methods=['POST'])
@error_handler
def run_scenario():
    """Run predefined chaos scenario"""
    scenario = request.json.get('scenario')

    scenarios = {
        'database-failure': [
            ('api/chaos/database', {'action': 'down'}),
        ],
        'kafka-outage': [
            ('api/chaos/kafka', {'action': 'down'}),
        ],
        'temporal-down': [
            ('api/chaos/temporal', {'action': 'down'}),
        ],
        'full-infrastructure-down': [
            ('api/chaos/database', {'action': 'down'}),
            ('api/chaos/kafka', {'action': 'down'}),
            ('api/chaos/cdc', {'action': 'down'}),
            ('api/chaos/temporal', {'action': 'down'}),
        ],
        'network-degradation': [
            ('api/chaos/delay', {'delay_ms': 500}),
            ('api/chaos/packet-loss', {'loss_percent': 10}),
        ],
        'cascading-failure': [
            ('api/chaos/database', {'action': 'down'}),
            ('api/chaos/kafka', {'action': 'down'}),
            ('api/chaos/delay', {'delay_ms': 1000}),
        ],
        'recover-all': [
            ('api/chaos/database', {'action': 'up'}),
            ('api/chaos/kafka', {'action': 'up'}),
            ('api/chaos/cdc', {'action': 'up'}),
            ('api/chaos/temporal', {'action': 'up'}),
            ('api/chaos/delay', {'delay_ms': 0}),
            ('api/chaos/packet-loss', {'loss_percent': 0}),
        ]
    }

    if scenario not in scenarios:
        return jsonify({'error': f'Unknown scenario: {scenario}'}), 400

    results = []
    for endpoint, payload in scenarios[scenario]:
        # Simulate the call
        results.append({
            'endpoint': endpoint,
            'payload': payload,
            'status': 'executed'
        })

    log_experiment('RUN_SCENARIO', scenario, 'success', {'steps': len(results)})
    emit_status_update()

    return jsonify({
        'status': f'Scenario {scenario} executed',
        'results': results
    })

@app.route('/api/chaos/reset', methods=['POST'])
@error_handler
def reset_chaos():
    """Reset all chaos state"""
    CHAOS_STATE['database_down'] = False
    CHAOS_STATE['kafka_down'] = False
    CHAOS_STATE['cdc_down'] = False
    CHAOS_STATE['temporal_down'] = False
    CHAOS_STATE['network_delay_ms'] = 0
    CHAOS_STATE['packet_loss_percent'] = 0
    CHAOS_STATE['services_stopped'] = []
    CHAOS_STATE['cpu_stress'] = False
    CHAOS_STATE['memory_stress'] = False
    CHAOS_STATE['disk_io_stress'] = False
    CHAOS_STATE['chaos_mode_active'] = False

    # Clean up real network chaos if enabled
    use_real = os.getenv('USE_REAL_NETWORK_DELAY', 'false').lower() == 'true'
    if use_real:
        try:
            network_chaos = get_network_chaos()
            network_chaos.cleanup()
        except:
            pass

    log_experiment('RESET', 'all', 'success')
    emit_status_update()

    return jsonify({'status': 'Chaos state reset'})

@app.route('/api/chaos/log')
@error_handler
def get_experiment_log():
    """Get experiment log"""
    return jsonify({
        'experiments': CHAOS_STATE['experiments_log'][-50:]  # Last 50
    })

@app.route('/api/chaos/workflow', methods=['POST'])
@error_handler
def chaos_workflow():
    """Chaos actions specific to Temporal workflows"""
    action = request.json.get('action')
    workflow_id = request.json.get('workflow_id')

    if action == 'cancel-random':
        # Get running workflows and cancel one
        result = run_docker_command(
            'docker exec banking-temporal-admin-tools temporal workflow list --namespace default --query "ExecutionStatus=\'Running\'" --limit 10'
        )
        if result['success'] and result['stdout'].strip():
            import random
            lines = result['stdout'].strip().split('\n')[1:]  # Skip header
            if lines:
                selected = random.choice(lines)
                workflow_id = selected.split()[0] if selected else None
                if workflow_id:
                    cancel_result = run_docker_command(
                        f'docker exec banking-temporal-admin-tools temporal workflow cancel --workflow-id {workflow_id}'
                    )
                    log_experiment('CANCEL_RANDOM_WORKFLOW', 'temporal',
                                 'success' if cancel_result['success'] else 'failed',
                                 {'workflow_id': workflow_id})
                    emit_status_update()
                    return jsonify({
                        'status': f'Cancelled workflow {workflow_id}',
                        'success': cancel_result['success']
                    })
        return jsonify({'status': 'No running workflows found', 'success': True})

    elif action == 'terminate-all':
        result = run_docker_command(
            'docker exec banking-temporal-admin-tools temporal workflow terminate --namespace default'
        )
        log_experiment('TERMINATE_ALL_WORKFLOWS', 'temporal',
                      'success' if result['success'] else 'failed')
        emit_status_update()
        return jsonify({'status': 'Terminate all command sent', 'success': result['success']})

    elif action == 'query-random':
        result = run_docker_command(
            'docker exec banking-temporal-admin-tools temporal workflow list --namespace default --query "ExecutionStatus=\'Running\'" --limit 5'
        )
        if result['success'] and result['stdout'].strip():
            lines = result['stdout'].strip().split('\n')[1:]
            if lines:
                selected = lines[0]
                workflow_id = selected.split()[0] if selected else None
                if workflow_id:
                    query_result = run_docker_command(
                        f'docker exec banking-temporal-admin-tools temporal workflow query --workflow-id {workflow_id} --type CurrentState'
                    )
                    return jsonify({
                        'workflow_id': workflow_id,
                        'query_result': query_result['stdout'] if query_result['success'] else query_result['stderr'],
                        'success': query_result['success']
                    })
        return jsonify({'status': 'No running workflows found', 'success': True})

    return jsonify({'error': 'Invalid action'}), 400

@app.route('/api/chaos/inject-fault', methods=['POST'])
@error_handler
def inject_fault():
    """Inject specific faults into the system"""
    fault_type = request.json.get('fault_type')
    duration_seconds = request.json.get('duration', 30)

    def auto_recover(fault, delay_ms=0, packet_loss=0):
        time.sleep(duration_seconds)
        if fault == 'timeout' or fault == 'slow-db':
            CHAOS_STATE['network_delay_ms'] = delay_ms
        elif fault == 'connection-reset':
            CHAOS_STATE['packet_loss_percent'] = packet_loss
        log_experiment('AUTO_RECOVER', fault, 'success', {'duration': duration_seconds})
        emit_status_update()

    if fault_type == 'timeout':
        old_delay = CHAOS_STATE['network_delay_ms']
        CHAOS_STATE['network_delay_ms'] = 5000
        log_experiment('INJECT_FAULT', 'timeout', 'success', {'duration': duration_seconds})
        threading.Thread(target=auto_recover, args=('timeout', old_delay), daemon=True).start()
        emit_status_update()
        return jsonify({'status': f'Timeout fault injected for {duration_seconds}s'})

    elif fault_type == 'connection-reset':
        old_loss = CHAOS_STATE['packet_loss_percent']
        CHAOS_STATE['packet_loss_percent'] = 100
        log_experiment('INJECT_FAULT', 'connection-reset', 'success', {'duration': duration_seconds})
        threading.Thread(target=auto_recover, args=('connection-reset', 0, old_loss), daemon=True).start()
        emit_status_update()
        return jsonify({'status': f'Connection reset fault injected for {duration_seconds}s'})

    elif fault_type == 'slow-db':
        old_delay = CHAOS_STATE['network_delay_ms']
        CHAOS_STATE['network_delay_ms'] = 2000
        log_experiment('INJECT_FAULT', 'slow-db', 'success', {'duration': duration_seconds})
        threading.Thread(target=auto_recover, args=('slow-db', old_delay), daemon=True).start()
        emit_status_update()
        return jsonify({'status': f'Slow DB fault injected for {duration_seconds}s'})

    elif fault_type == 'message-queue-backlog':
        log_experiment('INJECT_FAULT', 'message-queue-backlog', 'success', {'duration': duration_seconds})
        threading.Thread(target=auto_recover, args=('message-queue-backlog',), daemon=True).start()
        emit_status_update()
        return jsonify({'status': f'MQ backlog fault injected for {duration_seconds}s (simulated)'})

    return jsonify({'error': 'Invalid fault type'}), 400

# ============== WebSocket Events ==============
if socketio and WEBSOCKET_ENABLED:
    @socketio.on('connect')
    def handle_connect():
        """Handle client connection"""
        logger.info('Client connected to WebSocket')
        emit('connected', {'message': 'Connected to Chaos Dashboard'})
        # Send current status
        emit_status_update()

    @socketio.on('disconnect')
    def handle_disconnect():
        """Handle client disconnection"""
        logger.info('Client disconnected from WebSocket')

    @socketio.on('request_status')
    def handle_request_status():
        """Handle explicit status request"""
        emit_status_update()

# ============== Main ==============
if __name__ == '__main__':
    print("=" * 60)
    print("🌪️  CHAOS ENGINEERING DASHBOARD")
    print("=" * 60)
    print()
    
    # Check Docker availability
    CHAOS_STATE['docker_available'] = check_docker_available()
    docker_status = "✅ Available" if CHAOS_STATE['docker_available'] else "❌ Not available"
    print(f"Docker: {docker_status}")
    
    # Check network chaos availability
    try:
        network_chaos = get_network_chaos()
        nc_status = network_chaos.get_status()
        CHAOS_STATE['real_network_delay_available'] = nc_status.get('supports_real_delay', False)
        if CHAOS_STATE['real_network_delay_available']:
            print("Real Network Delay: ✅ Available (Linux tc)")
        else:
            print(f"Real Network Delay: ⚠️  Simulated ({network_chaos.system})")
    except Exception:
        print("Real Network Delay: ⚠️  Simulated")
    
    print()
    print(f"Dashboard URL: http://localhost:5000")
    print(f"API URL: http://localhost:5000/api")
    print(f"Metrics URL: http://localhost:5000/api/metrics")
    if WEBSOCKET_ENABLED:
        print(f"WebSocket: Enabled")
    print()
    print("=" * 60)
    
    port = int(os.getenv('FLASK_PORT', '5000'))
    host = os.getenv('FLASK_HOST', '0.0.0.0')
    debug = os.getenv('FLASK_DEBUG', '0') == '1'
    
    if socketio and WEBSOCKET_ENABLED:
        socketio.run(app, host=host, port=port, debug=debug)
    else:
        app.run(host=host, port=port, debug=debug)
