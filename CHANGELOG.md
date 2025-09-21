# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

## [1.1.0] - 2024-09-20

### ✅ Adicionado
- **Configuração CDC completa** para audit-service via Kafka
- **Scripts automatizados** para setup e diagnóstico
- **Documentação abrangente** em português
- **Arquivo de contribuição** (CONTRIBUTING.md)
- **Exemplos de API** completos (request.http)
- **Variáveis de ambiente** de exemplo (.env.example)

### 🔧 Corrigido
- **CDC do audit-service** agora funciona corretamente via Kafka
- **Configurações padronizadas** entre serviços
- **Dependências faltantes** no audit-service

### 🧹 Removido
- **docker-compose.yml** redundante (mantido apenas docker-compose-banking.yml)
- **scripts/register-connector.sh** redundante
- **scripts/test-cdc-setup.sh** vazio
- **AUDIT_CDC_FIX.md** e **DOCKER_COMPOSE_README.md** (consolidados no README)
- **Arquivos .DS_Store** desnecessários

### 📝 Alterado
- **README.md** completamente reescrito com instruções detalhadas
- **Scripts de diagnóstico** melhorados com cores e status visual
- **Configurações de porta** padronizadas
- **.gitignore** atualizado para incluir arquivos de offset

### 🏗️ Arquitetura
- **Antes**: audit-service tentava usar Debezium Embedded (não funcionava)
- **Depois**: audit-service consome eventos CDC via Kafka Topics (funciona)

### 📋 Scripts Disponíveis
- `scripts/start-banking-demo.sh` - Inicialização automatizada completa
- `scripts/setup-cdc-complete.sh` - Configuração CDC completa
- `scripts/diagnose-cdc.sh` - Diagnóstico visual do sistema
- `scripts/test-audit-cdc.sh` - Teste completo do CDC

### 🎯 Melhorias de Qualidade
- **Eliminação de redundâncias** - 5 arquivos removidos
- **Padronização** - Configurações consistentes
- **Documentação** - Instruções claras e completas
- **Automação** - Scripts para facilitar desenvolvimento
- **Manutenibilidade** - Código mais limpo e organizado

## [1.0.0] - Data Inicial
### ✅ Adicionado
- Implementação inicial do sistema bancário
- Microserviços: Account, Transfer, Validation, Notification, Audit
- Integração com Temporal.io
- Configuração básica do CDC
- Testes unitários e de integração