# 🌪️ Chaos Engineering Dashboard

Dashboard interativo para testar a resiliência do seu workflow Temporal Banking através da injeção controlada de falhas.

## 🎯 Objetivo

Simular cenários de caos para verificar se o Temporal realmente ajuda na construção de fluxos de longa duração, testando:

- **Retry policies** - Políticas de retry do Temporal
- **Workflow persistence** - Persistência de estado dos workflows
- **Compensation** - Compensação em caso de falha (Saga pattern)
- **Durability** - Durabilidade em falhas de infraestrutura

## 🚀 Quick Start

### 1. Instalar dependências

```bash
cd chaos-engineer

# Criar ambiente virtual (recomendado)
python3 -m venv venv
source venv/bin/activate  # Linux/Mac
# ou
venv\Scripts\activate  # Windows

# Instalar pacotes
pip install -r requirements.txt
```

### 2. Iniciar o Dashboard

```bash
# Certifique-se que o docker-compose está rodando
cd ..
docker-compose up -d

# Iniciar o servidor do caos
cd chaos-engineer
make run
# ou
python chaos_api.py
```

### 3. Acessar o Dashboard

Abra seu navegador em: **http://localhost:5000**

## 📋 Funcionalidades

### Controles de Infraestrutura

| Componente | Ação | Efeito Esperado |
|------------|------|-----------------|
| **Database** | Stop/Start | Testa retry do Temporal em falhas de DB |
| **Kafka** | Stop/Start | Testa resiliência de eventos CDC |
| **CDC** | Stop/Start | Testa audit trail assíncrono |
| **Temporal** | Stop/Start | Testa persistência de workflows |

### Condições de Rede

- **Network Delay**: Adiciona latência (0-5000ms) - Simulado ou Real (Linux com tc)
- **Packet Loss**: Simula perda de pacotes (0-100%) - Simulado ou Real (Linux com tc)

### Serviços Java

Controle individual de cada microsserviço:
- Account Service (:8081)
- Transfer Service (:8082)
- Validation Service (:8087)
- Notification Service (:8086)
- Audit Service (:8085)

### Controle Docker

- **Stop All / Start All**: Para/toda a infraestrutura
- **Kill Random**: Mata um container aleatório
- **Controle Individual**: Start/Stop/Restart por container

### Workflow Chaos

Ações específicas para workflows Temporal:
- **Cancel Random**: Cancela um workflow em execução
- **Terminate All**: Termina todos os workflows
- **Query Random**: Query o estado de um workflow

### Injeção de Falhas

Falhas com duração configurável:
- **Timeout**: Simula timeout de 5s
- **Connection Reset**: Perda total de conexão
- **Slow Database**: Latência de 2s no DB
- **MQ Backlog**: Backlog na fila de mensagens

### Health Checks Avançados

- **Detailed Health**: `/api/health/detailed` - Valida conexão com cada componente
- **Service Health**: Verifica saúde de cada microsserviço
- **Infrastructure Health**: Verifica containers e portas

### Métricas Prometheus

- **Metrics Endpoint**: `/api/metrics` - Formato Prometheus
- **Experiment Counter**: Total de experimentos por tipo/status
- **Active Chaos Gauge**: Número de experimentos ativos
- **Network Delay Gauge**: Delay atual em ms
- **Packet Loss Gauge**: Perda de pacotes atual em %
- **Service Health Gauge**: Saúde de cada serviço
- **Infrastructure Health Gauge**: Saúde de cada componente

### WebSocket (Opcional)

- **Real-time Updates**: Atualizações em tempo real do dashboard
- **Status Updates**: Estado do sistema push para o cliente
- **Experiment Log**: Logs de experimentos em tempo real

### Rede Real (Linux apenas)

- **tc (Traffic Control)**: Delay e packet loss reais na rede
- **Requer sudo**: Privilégios de root para manipulação de rede
- **Auto-detecção**: Interface de rede detectada automaticamente

## 🎬 Cenários Predefinidos

### 1. Database Failure
```
Para: PostgreSQL (banking + audit)
Testa: Retry policies e compensação
```

### 2. Kafka Outage
```
Para: Kafka + Zookeeper + Kafka UI
Testa: Resiliência de eventos CDC
```

### 3. Temporal Down
```
Para: Temporal Server + UI + Admin Tools
Testa: Persistência e recuperação de workflows
```

### 4. Full Infrastructure Down
```
Para: Database, Kafka, CDC, Temporal
Testa: Recuperação completa do sistema
```

### 5. Network Degradation
```
Delay: 500ms
Packet Loss: 10%
Testa: Tolerância a latência e perda
```

### 6. Cascading Failure
```
Para: Database + Kafka + Delay 1000ms
Testa: Falha em cascata e recuperação
```

### 7. Recover All
```
Inicia: Todos os serviços
Reseta: Delay e packet loss para 0
Testa: Recuperação completa
```

## 🧪 Experimentos Sugeridos

### Experimento 1: Transferência durante falha de DB

1. Inicie uma transferência via API
2. Durante o processamento, pare o PostgreSQL
3. Observe no Temporal UI (:8088) os retries
4. Reinicie o PostgreSQL
5. Verifique se a transferência completou

**O que observar:**
- Workflow permanece em "Running"
- Atividades são retryadas automaticamente
- Estado é preservado após recuperação

### Experimento 2: Falha de Kafka durante CDC

1. Inicie múltiplas transferências
2. Pare o Kafka
3. Verifique que as transferências completam (não dependem de Kafka síncrono)
4. Reinicie o Kafka
5. Verifique se os eventos CDC foram processados

**O que observar:**
- Workflows completam sem Kafka
- Audit trail é eventualmente consistente
- Eventos são processados após recuperação

### Experimento 3: Temporal Down durante workflow longo

1. Inicie transferência com delay (delayInSeconds: 60)
2. Durante o delay, pare o Temporal
3. Aguarde o período de delay passar
4. Reinicie o Temporal
5. Verifique se o workflow completou corretamente

**O que observar:**
- Timer persiste no banco do Temporal
- Workflow retoma de onde parou
- Compensação funciona se necessário

### Experimento 4: Cascading Failure

1. Inicie várias transferências simultâneas
2. Execute o cenário "Cascading Failure"
3. Monitore logs de todos os serviços
4. Execute "Recover All"
5. Verifique estado final de todas as transferências

**O que observar:**
- Serviços lidam com dependências indisponíveis
- Idempotência previne duplicação
- Estado final é consistente

## 🔍 Monitoramento

### Temporal UI
- URL: http://localhost:8088
- Visualize workflows em execução
- Veja retries e falhas
- Query estado dos workflows

### Kafka UI
- URL: http://localhost:8090
- Monitore tópicos CDC
- Veja consumer lag
- Inspecione mensagens

### Experiment Log
- Dashboard mostra histórico de experimentos
- Timestamp de cada ação
- Status de execução

## ⚠️ CHAOS MODE

Para executar ações destrutivas (parar serviços, terminar workflows):

1. Clique em **"⚠️ CHAOS MODE"** no canto superior direito
2. O botão mudará para **"✅ CHAOS MODE ON"**
3. Ações destrutivas agora requerem confirmação
4. Clique novamente para desativar

## 📊 Métricas

O dashboard mostra em tempo real:

- **Total Experiments**: Número de experimentos executados
- **Active Chaos**: Quantos serviços estão "down"
- **Network Delay**: Latência atual simulada
- **Packet Loss**: Perda de pacotes atual

## 🛑 Emergency Recovery

Se algo der errado:

```bash
# Parar tudo
docker-compose stop

# Limpar volumes (se necessário)
docker-compose down -v

# Reiniciar tudo
docker-compose up -d

# Aguardar 30 segundos
sleep 30

# Reconfigurar CDC
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @config/debezium-connector.json

# Ou use o dashboard: Cenário "Recover All"
```

## 📝 API Reference

### Status & Health

```bash
# Get current chaos state and system status
GET http://localhost:5000/api/status

# Detailed health check with infrastructure validation
GET http://localhost:5000/api/health/detailed

# Prometheus metrics
GET http://localhost:5000/api/metrics
```

### Chaos Controls

```bash
# Infrastructure control
POST http://localhost:5000/api/chaos/database
POST http://localhost:5000/api/chaos/kafka
POST http://localhost:5000/api/chaos/cdc
POST http://localhost:5000/api/chaos/temporal

# Network conditions
POST http://localhost:5000/api/chaos/delay
POST http://localhost:5000/api/chaos/packet-loss
GET  http://localhost:5000/api/chaos/network/status

# Service control
POST http://localhost:5000/api/chaos/service

# Docker control
POST http://localhost:5000/api/chaos/docker

# Workflow chaos
POST http://localhost:5000/api/chaos/workflow

# Fault injection
POST http://localhost:5000/api/chaos/inject-fault

# Scenarios
POST http://localhost:5000/api/chaos/scenario
```

### System Control

```bash
# Stress system
POST http://localhost:5000/api/chaos/stress

# Reset all chaos
POST http://localhost:5000/api/chaos/reset

# Get experiment log
GET http://localhost:5000/api/chaos/log
```

### WebSocket Events (if enabled)

```javascript
// Connect to WebSocket
const socket = io('http://localhost:5000');

// Listen for status updates
socket.on('status_update', (data) => {
  console.log('Status update:', data);
});

// Listen for experiment logs
socket.on('experiment_log', (data) => {
  console.log('Experiment:', data);
});

// Request current status
socket.emit('request_status');
```

### Example Requests

```bash
# Stop database
curl -X POST http://localhost:5000/api/chaos/database \
  -H "Content-Type: application/json" \
  -d '{"action": "down"}'

# Set network delay
curl -X POST http://localhost:5000/api/chaos/delay \
  -H "Content-Type: application/json" \
  -d '{"delay_ms": 500}'

# Run scenario
curl -X POST http://localhost:5000/api/chaos/scenario \
  -H "Content-Type: application/json" \
  -d '{"scenario": "database-failure"}'

# Get detailed health
curl http://localhost:5000/api/health/detailed

# Get Prometheus metrics
curl http://localhost:5000/api/metrics
```

## 🔧 Configuration

Copy `.env.example` to `.env` and customize:

```bash
# Flask Configuration
FLASK_ENV=production
FLASK_DEBUG=0
FLASK_PORT=5000

# Network Simulation
USE_REAL_NETWORK_DELAY=false  # Set to 'true' for real tc commands (Linux)

# WebSocket
WEBSOCKET_ENABLED=false  # Set to 'true' for real-time updates

# Logging
LOG_LEVEL=INFO
```

## 🐳 Docker Usage

### Build Image

```bash
make docker-build
# or
docker build -t chaos-dashboard:latest .
```

### Run Container

```bash
make docker-run
# or
docker run -d \
  --name chaos-dashboard \
  --network host \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e FLASK_PORT=5000 \
  chaos-dashboard:latest
```

**Note:** The container needs access to the Docker socket to control other containers.

### Stop Container

```bash
make docker-stop
```

## 🧪 Testing

### Run Tests

```bash
make test
# or
pytest tests/ -v
```

### Run Tests with Coverage

```bash
make coverage
# or
pytest tests/ -v --cov=chaos_api --cov=network_chaos --cov-report=html
```

Coverage report will be generated in `htmlcov/index.html`.

### Test Requirements

```bash
pip install -r requirements.txt
# This includes pytest and related testing packages
```

## 🌐 Advanced Features

### Real Network Delay (Linux Only)

For real network manipulation using `tc` (traffic control):

```bash
# Requires sudo privileges
sudo USE_REAL_NETWORK_DELAY=true python chaos_api.py
# or
make run-real-network
```

**Features:**
- Real packet delay using `tc qdisc`
- Real packet loss using `tc netem`
- Automatic interface detection
- Cleanup on reset

**Requirements:**
- Linux operating system
- Root/sudo privileges
- `tc` command installed (usually part of `iproute2` package)

### WebSocket Support

For real-time dashboard updates:

```bash
WEBSOCKET_ENABLED=true python chaos_api.py
# or
make run-ws
```

**Benefits:**
- Instant status updates
- Real-time experiment logging
- Push notifications for chaos events

### Prometheus Integration

Metrics are available at `/api/metrics` in Prometheus format:

```prometheus
# HELP chaos_experiments_total Total number of chaos experiments
# TYPE chaos_experiments_total counter
chaos_experiments_total{type="STOP",target="database",status="success"} 5.0

# HELP chaos_active_count Number of active chaos experiments
# TYPE chaos_active_count gauge
chaos_active_count 2.0

# HELP chaos_network_delay_ms Current network delay in milliseconds
# TYPE chaos_network_delay_ms gauge
chaos_network_delay_ms 500.0
```

**Scrape config:**

```yaml
scrape_configs:
  - job_name: 'chaos-dashboard'
    static_configs:
      - targets: ['localhost:5000']
    metrics_path: '/api/metrics'
```

## 🎓 Aprendizados Esperados

Ao usar este dashboard, você deve observar:

1. **Workflows são duráveis**: Sobrevivem a falhas de infraestrutura
2. **Retries automáticos**: Atividades falham mas são retryadas
3. **Estado preservado**: Progresso não é perdido
4. **Compensação funciona**: Saga rollback em falhas
5. **Timers são confiáveis**: Delays persistem corretamente
6. **Idempotência é crucial**: Previne efeitos colaterais duplicados

## 📞 Troubleshooting

### Dashboard não inicia
```bash
# Verificar Python 3.8+
python3 --version

# Reinstalar dependências
pip install -r requirements.txt --force-reinstall

# Verificar logs
python chaos_api.py 2>&1 | tee chaos_dashboard.log
```

### API não responde
```bash
# Verificar se o servidor está rodando
curl http://localhost:5000/api/status

# Verificar logs do servidor
# Verificar se a porta está em uso
lsof -i :5000
```

### Docker commands falham
```bash
# Verificar Docker
docker ps

# Verificar docker-compose
docker-compose ps

# Verificar permissões do socket Docker
ls -la /var/run/docker.sock

# Navegar até a raiz do projeto
cd /Users/ptaylon/development/temporal/temporal-banking-workflow
```

### Real Network Delay não funciona
```bash
# Verificar se tc está disponível
tc -h

# Verificar se está rodando como root
whoami

# No Linux, usar sudo
sudo make run-real-network

# Verificar interface de rede
ip route | grep default
```

### WebSocket não conecta
```bash
# Verificar se Flask-SocketIO está instalado
pip show flask-socketio

# Verificar se WEBSOCKET_ENABLED=true no .env

# Verificar console do navegador para erros
```

### Prometheus metrics não aparecem
```bash
# Verificar se prometheus-client está instalado
pip show prometheus-client

# Testar endpoint
curl http://localhost:5000/api/metrics

# Verificar formato Prometheus
curl -v http://localhost:5000/api/metrics
```

### Tests falham
```bash
# Instalar dependências de teste
pip install pytest pytest-cov pytest-flask

# Rodar testes em modo verbose
pytest tests/ -v

# Rodar testes específicos
pytest tests/test_chaos_api.py::TestStatusEndpoint -v
```

## 📄 License

MIT License - Mesmo license do projeto principal

---

**Criado para testar a resiliência do Temporal Banking Workflow** 🌪️

**Novas Features:**
- ✅ Health checks avançados com validação de infraestrutura
- ✅ Métricas Prometheus para monitoramento
- ✅ WebSocket para atualizações em tempo real
- ✅ Rede real com tc (Linux)
- ✅ Testes unitários e de integração
- ✅ Dockerfile para containerização
- ✅ Error handling melhorado
