"""
Network Chaos Module - Real network delay and packet loss injection
Uses tc (traffic control) for Linux or pfctl for macOS
"""
import subprocess
import platform
import logging

logger = logging.getLogger(__name__)


class NetworkChaos:
    """
    Real network chaos injection using system tools.
    Requires root/admin privileges for actual network manipulation.
    """

    def __init__(self, interface=None):
        """
        Initialize network chaos controller.
        
        Args:
            interface: Network interface to manipulate (e.g., 'eth0', 'en0')
                      Auto-detected if not provided
        """
        self.system = platform.system()
        self.interface = interface or self._detect_interface()
        self.qdisc_setup = False
        
    def _detect_interface(self):
        """Detect default network interface"""
        try:
            if self.system == 'Linux':
                # Try to get default interface from ip route
                result = subprocess.run(
                    ['ip', 'route', '|', 'grep', 'default', '|', 'awk', "'{print $5}'"],
                    capture_output=True,
                    text=True,
                    shell=True
                )
                if result.stdout.strip():
                    return result.stdout.strip()
                return 'eth0'
            elif self.system == 'Darwin':  # macOS
                result = subprocess.run(
                    ['netstat', '-rn', '|', 'grep', 'default', '|', 'grep', '-v', 'utun', '|', 'awk', "'{print $4}'"],
                    capture_output=True,
                    text=True,
                    shell=True
                )
                if result.stdout.strip():
                    return result.stdout.strip()
                return 'en0'
        except Exception as e:
            logger.warning(f"Could not detect interface: {e}")
            return 'eth0' if self.system == 'Linux' else 'en0'

    def _run_command(self, command, use_sudo=False):
        """
        Run system command with optional sudo.
        
        Args:
            command: Command to run
            use_sudo: Whether to prepend sudo
            
        Returns:
            dict with success status and output
        """
        try:
            if use_sudo and self.system != 'Windows':
                command = f"sudo {command}"
            
            result = subprocess.run(
                command,
                shell=True,
                capture_output=True,
                text=True,
                timeout=10
            )
            
            return {
                'success': result.returncode == 0,
                'stdout': result.stdout,
                'stderr': result.stderr,
                'returncode': result.returncode
            }
        except subprocess.TimeoutExpired:
            return {'success': False, 'error': 'Command timeout'}
        except PermissionError as e:
            return {'success': False, 'error': f'Permission denied: {e}'}
        except Exception as e:
            return {'success': False, 'error': str(e)}

    def setup_qdisc(self):
        """
        Setup queueing discipline for traffic control (Linux only).
        Must be run once before adding delays.
        """
        if self.system != 'Linux':
            return {'success': True, 'message': 'Not needed on this platform'}
        
        if self.qdisc_setup:
            return {'success': True, 'message': 'Already setup'}
        
        # Remove existing qdisc if any
        self._run_command(f"tc qdisc del dev {self.interface} root 2>/dev/null")
        
        # Add new qdisc with netem
        result = self._run_command(f"tc qdisc add dev {self.interface} root netem")
        
        if result['success']:
            self.qdisc_setup = True
            logger.info(f"Qdisc setup complete on {self.interface}")
        
        return result

    def add_delay(self, delay_ms):
        """
        Add network delay.
        
        Args:
            delay_ms: Delay in milliseconds
            
        Returns:
            dict with result
        """
        if self.system == 'Linux':
            return self._add_delay_linux(delay_ms)
        elif self.system == 'Darwin':
            return self._add_delay_macos(delay_ms)
        else:
            return {'success': False, 'error': f'Unsupported platform: {self.system}'}

    def _add_delay_linux(self, delay_ms):
        """Add delay on Linux using tc"""
        # Setup qdisc if needed
        if not self.qdisc_setup:
            self.setup_qdisc()
        
        if delay_ms == 0:
            # Remove delay
            result = self._run_command(f"tc qdisc change dev {self.interface} root netem")
        else:
            # Convert to seconds for tc
            delay_sec = delay_ms / 1000.0
            result = self._run_command(
                f"tc qdisc change dev {self.interface} root netem delay {delay_sec}s"
            )
        
        if result['success']:
            logger.info(f"Delay set to {delay_ms}ms on {self.interface}")
        else:
            logger.warning(f"Failed to set delay: {result.get('stderr', 'Unknown error')}")
        
        return result

    def _add_delay_macos(self, delay_ms):
        """
        Add delay on macOS using pfctl and dummynet.
        Requires sudo privileges.
        """
        if delay_ms == 0:
            # Remove firewall rules
            self._run_command("pfctl -f /etc/pf.conf", use_sudo=True)
            self._run_command("pfctl -d", use_sudo=True)
            return {'success': True, 'message': 'Delay removed'}
        
        # Enable packet filter
        result = self._run_command("pfctl -e", use_sudo=True)
        if not result['success'] and 'already enabled' not in result.get('stderr', ''):
            logger.warning(f"Failed to enable pf: {result.get('stderr', '')}")
        
        # Create dummynet pipe with delay
        # Note: This is a simplified version - full implementation would need
        # proper pf.conf configuration
        logger.warning("macOS delay injection requires manual pf.conf configuration")
        return {
            'success': False,
            'error': 'macOS requires manual pf.conf setup. See README for instructions.'
        }

    def add_packet_loss(self, loss_percent):
        """
        Add packet loss.
        
        Args:
            loss_percent: Percentage of packets to drop (0-100)
            
        Returns:
            dict with result
        """
        if self.system == 'Linux':
            return self._add_packet_loss_linux(loss_percent)
        elif self.system == 'Darwin':
            return self._add_packet_loss_macos(loss_percent)
        else:
            return {'success': False, 'error': f'Unsupported platform: {self.system}'}

    def _add_packet_loss_linux(self, loss_percent):
        """Add packet loss on Linux using tc"""
        if not self.qdisc_setup:
            self.setup_qdisc()
        
        if loss_percent == 0:
            result = self._run_command(f"tc qdisc change dev {self.interface} root netem")
        else:
            result = self._run_command(
                f"tc qdisc change dev {self.interface} root netem loss {loss_percent}%"
            )
        
        if result['success']:
            logger.info(f"Packet loss set to {loss_percent}% on {self.interface}")
        
        return result

    def _add_packet_loss_macos(self, loss_percent):
        """Add packet loss on macOS using pfctl"""
        if loss_percent == 0:
            self._run_command("pfctl -f /etc/pf.conf", use_sudo=True)
            return {'success': True, 'message': 'Packet loss removed'}
        
        logger.warning("macOS packet loss requires manual pf.conf configuration")
        return {
            'success': False,
            'error': 'macOS requires manual pf.conf setup for packet loss'
        }

    def add_bandwidth_limit(self, limit_kbps):
        """
        Limit bandwidth.
        
        Args:
            limit_kbps: Bandwidth limit in kbps
            
        Returns:
            dict with result
        """
        if self.system != 'Linux':
            return {'success': False, 'error': 'Bandwidth limiting only supported on Linux'}
        
        if not self.qdisc_setup:
            self.setup_qdisc()
        
        if limit_kbps == 0:
            result = self._run_command(f"tc qdisc change dev {self.interface} root netem")
        else:
            # Convert kbps to bit rate for tc
            rate_kbit = limit_kbps
            result = self._run_command(
                f"tc qdisc add dev {self.interface} root tbf rate {rate_kbit}kbit burst 32kbit latency 400ms"
            )
        
        return result

    def cleanup(self):
        """
        Remove all network chaos and restore normal operation.
        """
        if self.system == 'Linux':
            result = self._run_command(f"tc qdisc del dev {self.interface} root 2>/dev/null")
            self.qdisc_setup = False
        elif self.system == 'Darwin':
            self._run_command("pfctl -f /etc/pf.conf", use_sudo=True)
            self._run_command("pfctl -d", use_sudo=True)
        else:
            result = {'success': False, 'error': 'Unsupported platform'}
            return result
        
        logger.info("Network chaos cleaned up")
        return result

    def get_status(self):
        """
        Get current network chaos status.
        
        Returns:
            dict with current configuration
        """
        status = {
            'platform': self.system,
            'interface': self.interface,
            'qdisc_setup': self.qdisc_setup,
            'delay_ms': 0,
            'packet_loss_percent': 0,
            'supports_real_delay': self.system == 'Linux'
        }
        
        if self.system == 'Linux':
            try:
                result = self._run_command(f"tc qdisc show dev {self.interface}")
                if result['success'] and result['stdout']:
                    output = result['stdout']
                    # Parse delay
                    if 'delay' in output:
                        import re
                        delay_match = re.search(r'delay\s+([\d.]+)ms', output)
                        if delay_match:
                            status['delay_ms'] = float(delay_match.group(1))
                    # Parse loss
                    if 'loss' in output:
                        import re
                        loss_match = re.search(r'loss\s+([\d.]+)%', output)
                        if loss_match:
                            status['packet_loss_percent'] = float(loss_match.group(1))
            except Exception as e:
                logger.warning(f"Could not get tc status: {e}")
        
        return status


# Singleton instance for use in Flask app
_network_chaos_instance = None


def get_network_chaos(interface=None):
    """Get or create network chaos singleton"""
    global _network_chaos_instance
    if _network_chaos_instance is None:
        _network_chaos_instance = NetworkChaos(interface)
    return _network_chaos_instance
