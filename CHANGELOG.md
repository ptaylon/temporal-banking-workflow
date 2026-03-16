# Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

## [2.0.0] - 2026-03-14

### 🎉 MAJOR REFACTORING COMPLETE

#### ✅ PHASE 1: Hexagonal Architecture
- **Validation-Service** - Complete hexagonal architecture implementation
  - Domain layer with TransferValidationDomain
  - Input ports: ValidateTransferUseCase, QueryValidationUseCase
  - Output ports: ValidationPersistencePort, AccountServicePort, TransferLimitPort, FraudRulePort
  - Infrastructure adapters: REST, persistence, HTTP clients
  - Idempotency support

- **Notification-Service** - Complete hexagonal architecture implementation
  - Domain layer with NotificationDomain
  - Input ports: SendNotificationUseCase, QueryNotificationUseCase
  - Output ports: NotificationPersistencePort, EmailPort
  - Infrastructure adapters: Kafka listener, REST, persistence, email
  - Idempotency support

- **Audit-Service** - Complete hexagonal architecture implementation
  - Domain layer with AuditEventDomain
  - Input ports: ProcessCDCEventUseCase, QueryAuditUseCase
  - Output ports: AuditPersistencePort, CDCEventParserPort
  - Infrastructure adapters: Kafka listener (CDC), REST, persistence, CDC parser
  - Idempotency based on Kafka offset

#### ✅ PHASE 2: Duplicate Code Removal
- Removed 5 duplicate files from transfer-service
  - Old TransferService.java
  - Old TransferControlService.java
  - Old TransferController.java
  - Old TransferPersistenceService.java
  - Old FeatureFlagService.java (moved to config)
- Moved FeatureFlagService to config package
- Updated all adapters to use domain ports
- Removed 10 empty directories

#### ✅ PHASE 3: Advanced Temporal Features

##### Search Attributes
- Created SearchAttributesConfig utility class
- Implemented upsertSearchAttributes in MoneyTransferWorkflowImpl
- Created TransferSearchService with 6 search methods
- Created TransferSearchRestController with 6 REST endpoints
- 6 search attributes implemented:
  - TransferAmount (Double)
  - SourceAccount (Keyword)
  - DestinationAccount (Keyword)
  - Currency (Keyword)
  - TransferStatus (Keyword)
  - Priority (Int, 0-5)

##### Timers (Configurable Delays)
- Extended TransferRequest with delay configurations
  - delayInSeconds - Delay before starting transfer
  - timeoutInSeconds - Timeout for entire transfer
  - allowCancelDuringDelay - Can cancel during delay?
- Implemented handleConfigurableDelay() in MoneyTransferWorkflowImpl
- Cancellable delay using Promise.anyOf()
- Delay status tracking (delayCompleted, delayCancelled)

##### Signals & Queries (Already Implemented)
- pauseTransfer() - Pause workflow execution
- resumeTransfer() - Resume paused workflow
- cancelTransfer(String reason) - Cancel workflow
- isPaused() - Query pause status
- getControlStatus() - Query detailed control status

#### ✅ PHASE 4: Test Coverage
- Created ValidationServiceTest (4 tests passing)
- Created NotificationServiceTest (11 tests passing)
- Created TransferServiceTest (9 tests)
- Fixed MoneyTransferWorkflowTest (updated method signatures)
- Fixed MoneyTransferWorkflowControlTest (updated method signatures)
- Deleted outdated E2E tests (TransferControllerE2ETest, TransferControlServiceTest)

#### 🔧 Technical Changes
- Changed MoneyTransferActivities.updateTransferStatus() signature
  - From: `updateTransferStatus(Long transferId, String status)`
  - To: `updateTransferStatus(Long transferId, TransferStatus status)`
- Changed MoneyTransferActivities.updateTransferStatusWithReason() signature
  - From: `updateTransferStatusWithReason(Long, String, String)`
  - To: `updateTransferStatusWithReason(Long, TransferStatus, String)`
- Updated all activity implementations
- Updated all workflow implementations

#### 📊 Metrics
- Total Java files: 124 (119 main + 5 test)
- Test coverage: ~40% (30 tests passing)
- Build time: ~3 seconds
- Empty directories removed: 10
- Duplicate files removed: 5

#### 📝 Documentation
- Created REFACTORING_PROGRESS.md - Detailed progress tracker
- Created REFACTORING_COMPLETE_REPORT.md - Final report
- Created PHASE3_IMPLEMENTATION_PLAN.md - Implementation details
- Created PHASE2_REMOVAL_PLAN.md - Cleanup strategy
- Updated CHANGELOG.md - This file

### 🧹 Removed
- Outdated E2E tests referencing deleted classes
- 10 empty directories
- All duplicate service classes
- Portuguese comments from code (being standardized)

---

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