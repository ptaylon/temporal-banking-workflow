"""
Pytest configuration and fixtures for Chaos Engineering API tests
"""
import pytest
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from chaos_api import app, CHAOS_STATE


@pytest.fixture
def client():
    """Create a test client for the Flask application"""
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client


@pytest.fixture
def chaos_state():
    """Provide access to the global CHAOS_STATE for testing"""
    return CHAOS_STATE


@pytest.fixture
def reset_chaos_state():
    """Reset chaos state before each test"""
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
    CHAOS_STATE['experiments_log'] = []
    yield
    # Cleanup after test
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
    CHAOS_STATE['experiments_log'] = []
