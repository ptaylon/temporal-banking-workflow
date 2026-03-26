"""
Unit tests for Chaos Engineering API
"""
import pytest
import json
from unittest.mock import patch, MagicMock


class TestStatusEndpoint:
    """Tests for /api/status endpoint"""

    def test_get_status_returns_chaos_state(self, client, reset_chaos_state):
        """Test that status endpoint returns chaos state"""
        response = client.get('/api/status')
        assert response.status_code == 200
        
        data = json.loads(response.data)
        assert 'chaos_state' in data
        assert 'infrastructure' in data
        assert 'services' in data
        assert 'timestamp' in data

    def test_get_status_initial_state(self, client, reset_chaos_state):
        """Test initial chaos state is all false/zero"""
        response = client.get('/api/status')
        data = json.loads(response.data)
        
        chaos_state = data['chaos_state']
        assert chaos_state['database_down'] == False
        assert chaos_state['kafka_down'] == False
        assert chaos_state['cdc_down'] == False
        assert chaos_state['temporal_down'] == False
        assert chaos_state['network_delay_ms'] == 0
        assert chaos_state['packet_loss_percent'] == 0


class TestDatabaseChaos:
    """Tests for /api/chaos/database endpoint"""

    @patch('chaos_api.run_docker_command')
    def test_database_down(self, mock_docker, client, reset_chaos_state):
        """Test stopping database"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/database',
                              json={'action': 'down'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'Database stopped' in data['status']
        
        # Verify state changed
        from chaos_api import CHAOS_STATE
        assert CHAOS_STATE['database_down'] == True

    @patch('chaos_api.run_docker_command')
    def test_database_up(self, mock_docker, client, reset_chaos_state):
        """Test starting database"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        # First set database as down
        from chaos_api import CHAOS_STATE
        CHAOS_STATE['database_down'] = True
        
        response = client.post('/api/chaos/database',
                              json={'action': 'up'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'Database started' in data['status']
        assert CHAOS_STATE['database_down'] == False


class TestKafkaChaos:
    """Tests for /api/chaos/kafka endpoint"""

    @patch('chaos_api.run_docker_command')
    def test_kafka_down(self, mock_docker, client, reset_chaos_state):
        """Test stopping Kafka cluster"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/kafka',
                              json={'action': 'down'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'Kafka cluster stopped' in data['status']
        assert CHAOS_STATE['kafka_down'] == True

    @patch('chaos_api.run_docker_command')
    def test_kafka_up(self, mock_docker, client, reset_chaos_state):
        """Test starting Kafka cluster"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        from chaos_api import CHAOS_STATE
        CHAOS_STATE['kafka_down'] = True
        
        response = client.post('/api/chaos/kafka',
                              json={'action': 'up'},
                              content_type='application/json')
        
        assert response.status_code == 200
        assert CHAOS_STATE['kafka_down'] == False


class TestCDCChaos:
    """Tests for /api/chaos/cdc endpoint"""

    @patch('chaos_api.run_docker_command')
    def test_cdc_down(self, mock_docker, client, reset_chaos_state):
        """Test stopping CDC"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/cdc',
                              json={'action': 'down'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'CDC stopped' in data['status']
        assert CHAOS_STATE['cdc_down'] == True


class TestTemporalChaos:
    """Tests for /api/chaos/temporal endpoint"""

    @patch('chaos_api.run_docker_command')
    def test_temporal_down(self, mock_docker, client, reset_chaos_state):
        """Test stopping Temporal"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/temporal',
                              json={'action': 'down'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'Temporal stopped' in data['status']
        assert CHAOS_STATE['temporal_down'] == True


class TestNetworkConditions:
    """Tests for network delay and packet loss endpoints"""

    def test_set_network_delay(self, client, reset_chaos_state):
        """Test setting network delay"""
        response = client.post('/api/chaos/delay',
                              json={'delay_ms': 500},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['delay_ms'] == 500
        assert '500ms' in data['status']

    def test_set_packet_loss(self, client, reset_chaos_state):
        """Test setting packet loss"""
        response = client.post('/api/chaos/packet-loss',
                              json={'loss_percent': 25},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['loss_percent'] == 25
        assert '25%' in data['status']

    def test_network_delay_zero_by_default(self, client, reset_chaos_state):
        """Test default network delay is zero"""
        response = client.get('/api/status')
        data = json.loads(response.data)
        assert data['chaos_state']['network_delay_ms'] == 0


class TestScenarios:
    """Tests for predefined chaos scenarios"""

    @patch('chaos_api.run_docker_command')
    def test_database_failure_scenario(self, mock_docker, client, reset_chaos_state):
        """Test database failure scenario"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/scenario',
                              json={'scenario': 'database-failure'},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'database-failure' in data['status']

    @patch('chaos_api.run_docker_command')
    def test_recover_all_scenario(self, mock_docker, client, reset_chaos_state):
        """Test recover all scenario"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        # Set some chaos state
        from chaos_api import CHAOS_STATE
        CHAOS_STATE['database_down'] = True
        CHAOS_STATE['network_delay_ms'] = 500
        
        response = client.post('/api/chaos/scenario',
                              json={'scenario': 'recover-all'},
                              content_type='application/json')
        
        assert response.status_code == 200

    def test_unknown_scenario_returns_error(self, client, reset_chaos_state):
        """Test unknown scenario returns 400"""
        response = client.post('/api/chaos/scenario',
                              json={'scenario': 'unknown-scenario'},
                              content_type='application/json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data


class TestReset:
    """Tests for reset endpoint"""

    def test_reset_chaos(self, client, reset_chaos_state):
        """Test resetting all chaos state"""
        from chaos_api import CHAOS_STATE
        
        # Set some chaos
        CHAOS_STATE['database_down'] = True
        CHAOS_STATE['network_delay_ms'] = 1000
        CHAOS_STATE['packet_loss_percent'] = 50
        
        response = client.post('/api/chaos/reset')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert data['status'] == 'Chaos state reset'
        
        # Verify state is reset
        assert CHAOS_STATE['database_down'] == False
        assert CHAOS_STATE['network_delay_ms'] == 0
        assert CHAOS_STATE['packet_loss_percent'] == 0


class TestExperimentLog:
    """Tests for experiment logging"""

    @patch('chaos_api.run_docker_command')
    def test_experiment_is_logged(self, mock_docker, client, reset_chaos_state):
        """Test that experiments are logged"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        client.post('/api/chaos/database',
                   json={'action': 'down'},
                   content_type='application/json')
        
        response = client.get('/api/chaos/log')
        data = json.loads(response.data)
        
        assert 'experiments' in data
        assert len(data['experiments']) > 0
        assert data['experiments'][-1]['action'] == 'STOP'
        assert data['experiments'][-1]['target'] == 'database'


class TestDockerCommands:
    """Tests for Docker control endpoints"""

    @patch('chaos_api.run_docker_command')
    def test_stop_all_containers(self, mock_docker, client, reset_chaos_state):
        """Test stopping all containers"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/docker',
                              json={'action': 'stop-all'},
                              content_type='application/json')
        
        assert response.status_code == 200

    @patch('chaos_api.run_docker_command')
    def test_kill_random_container(self, mock_docker, client, reset_chaos_state):
        """Test killing random container"""
        mock_docker.return_value = {'success': True, 'stdout': '', 'stderr': ''}
        
        response = client.post('/api/chaos/docker',
                              json={'action': 'kill-random'},
                              content_type='application/json')
        
        assert response.status_code == 200


class TestFaultInjection:
    """Tests for fault injection endpoints"""

    def test_inject_timeout_fault(self, client, reset_chaos_state):
        """Test injecting timeout fault"""
        response = client.post('/api/chaos/inject-fault',
                              json={'fault_type': 'timeout', 'duration': 10},
                              content_type='application/json')
        
        assert response.status_code == 200
        data = json.loads(response.data)
        assert 'Timeout fault injected' in data['status']

    def test_inject_connection_reset_fault(self, client, reset_chaos_state):
        """Test injecting connection reset fault"""
        response = client.post('/api/chaos/inject-fault',
                              json={'fault_type': 'connection-reset', 'duration': 10},
                              content_type='application/json')
        
        assert response.status_code == 200

    def test_inject_slow_db_fault(self, client, reset_chaos_state):
        """Test injecting slow DB fault"""
        response = client.post('/api/chaos/inject-fault',
                              json={'fault_type': 'slow-db', 'duration': 10},
                              content_type='application/json')
        
        assert response.status_code == 200

    def test_unknown_fault_returns_error(self, client, reset_chaos_state):
        """Test unknown fault type returns 400"""
        response = client.post('/api/chaos/inject-fault',
                              json={'fault_type': 'unknown-fault'},
                              content_type='application/json')
        
        assert response.status_code == 400


class TestServiceControl:
    """Tests for Java service control"""

    def test_unknown_service_returns_error(self, client, reset_chaos_state):
        """Test controlling unknown service returns 400"""
        response = client.post('/api/chaos/service',
                              json={'service': 'unknown-service', 'action': 'stop'},
                              content_type='application/json')
        
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
