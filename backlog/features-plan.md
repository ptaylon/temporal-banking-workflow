# Plano de Funcionalidades Avançadas - Temporal.io Banking Demo

## Análise do Sistema Atual

O sistema bancário atual implementa um fluxo básico de transferências usando Temporal.io com:
- Workflow simples de transferência monetária
- Padrão Saga para compensação
- Retry policies configuráveis
- Integração com CDC via Debezium
- Auditoria completa via Kafka

## Funcionalidades CORE do Temporal.io Não Exploradas

### ⚠️ **Features Temporal Atualmente NÃO Utilizadas:**
- **Signals** - Comunicação externa com workflows em execução
- **Queries** - Consulta de estado interno de workflows
- **Timers** - Delays e timeouts programáticos
- **Child Workflows** - Composição e orquestração hierárquica
- **Continue As New** - Workflows de longa duração infinita
- **Side Effects** - Operações não-determinísticas seguras
- **Local Activities** - Atividades otimizadas para operações rápidas
- **Async Activities** - Execução paralela de atividades
- **Workflow Updates** - Modificação de workflows em execução
- **Search Attributes** - Indexação e busca de workflows
- **Schedules** - Execução programada e recorrente
- **Versioning** - Evolução de workflows sem breaking changes

## Funcionalidades Avançadas do Temporal.io para Implementar

### 1. **Signals & Queries - Comunicação Bidirecional**

#### 1.1 Sistema de Pausar/Retomar Transferências
**Complexidade**: ⭐⭐⭐
- **Temporal Features Utilizadas**: 
  - `@SignalMethod` para pausar/retomar workflows
  - `@QueryMethod` para consultar status de pausa
  - `Workflow.await()` para aguardar sinal de retomada
- **Implementação**:
  ```java
  @SignalMethod
  void pauseTransfer();
  
  @SignalMethod  
  void resumeTransfer();
  
  @QueryMethod
  boolean isPaused();
  ```
- **Casos de Uso**:
  - Intervenção manual em transferências suspeitas
  - Manutenção programada de sistemas
  - Controle de fluxo durante picos de demanda

#### 1.2 Atualização de Limites em Tempo Real
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `@SignalMethod` para receber novos limites
  - `@QueryMethod` para consultar limites atuais
  - `Workflow.getVersion()` para compatibilidade
- **Implementação**:
  ```java
  @SignalMethod
  void updateTransferLimits(TransferLimits newLimits);
  
  @QueryMethod
  TransferLimits getCurrentLimits();
  ```
- **Casos de Uso**:
  - Ajuste de limites por compliance
  - Promoções temporárias de limite
  - Resposta a eventos de fraude

### 2. **Timers & Delays - Controle Temporal Avançado**

#### 2.1 Transferências com Delay Configurável
**Complexidade**: ⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Workflow.sleep(Duration)` para delays programados
  - `Workflow.newTimer()` para timeouts canceláveis
  - `@SignalMethod` para cancelar delays
- **Implementação**:
  ```java
  // Delay configurável antes da execução
  Workflow.sleep(Duration.ofHours(transferRequest.getDelayHours()));
  
  // Timer cancelável
  Promise<Void> timer = Workflow.newTimer(Duration.ofMinutes(30));
  Promise<Void> cancelSignal = Workflow.await(() -> cancelRequested);
  Promise.anyOf(timer, cancelSignal);
  ```
- **Casos de Uso**:
  - Transferências programadas para horário específico
  - Cooling-off period para grandes transferências
  - Janelas de manutenção bancária

#### 2.2 Sistema de Timeout Escalonado
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - Múltiplos `Workflow.newTimer()` para diferentes níveis
  - `Promise.anyOf()` para race conditions
  - `@SignalMethod` para intervenção manual
- **Implementação**:
  ```java
  Promise<Void> level1Timer = Workflow.newTimer(Duration.ofMinutes(5));
  Promise<Void> level2Timer = Workflow.newTimer(Duration.ofMinutes(15));
  Promise<Void> level3Timer = Workflow.newTimer(Duration.ofHours(1));
  
  int completedIndex = Promise.anyOf(level1Timer, level2Timer, level3Timer).get();
  ```
- **Casos de Uso**:
  - Escalação automática de aprovações
  - SLA diferenciado por valor
  - Timeout progressivo para retry

### 3. **Child Workflows - Orquestração Hierárquica**

#### 3.1 Transferências em Lote com Child Workflows
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Workflow.newChildWorkflowStub()` para cada transferência individual
  - `Async.function()` para execução paralela de children
  - `Promise.allOf()` para aguardar conclusão de todos
- **Implementação**:
  ```java
  List<Promise<TransferResponse>> childPromises = new ArrayList<>();
  
  for (TransferRequest request : batchRequest.getTransfers()) {
      MoneyTransferWorkflow childWorkflow = Workflow.newChildWorkflowStub(
          MoneyTransferWorkflow.class,
          ChildWorkflowOptions.newBuilder()
              .setWorkflowId("transfer-" + request.getTransferId())
              .build()
      );
      
      Promise<TransferResponse> promise = Async.function(childWorkflow::executeTransfer, request);
      childPromises.add(promise);
  }
  
  Promise.allOf(childPromises).get();
  ```
- **Casos de Uso**:
  - Folha de pagamento com milhares de transferências
  - Distribuição de dividendos
  - Liquidação de operações em lote

#### 3.2 Workflow de Reconciliação Hierárquica
**Complexidade**: ⭐⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - Parent workflow para coordenação geral
  - Child workflows por banco/região
  - `@QueryMethod` para status agregado
  - `@SignalMethod` para correções manuais
- **Implementação**:
  ```java
  // Parent Workflow
  Map<String, ReconciliationWorkflow> childWorkflows = new HashMap<>();
  
  for (String bankCode : bankCodes) {
      ReconciliationWorkflow child = Workflow.newChildWorkflowStub(
          ReconciliationWorkflow.class,
          ChildWorkflowOptions.newBuilder()
              .setWorkflowId("reconciliation-" + bankCode + "-" + date)
              .build()
      );
      childWorkflows.put(bankCode, child);
      Async.procedure(child::reconcileBank, bankCode, date);
  }
  ```
- **Casos de Uso**:
  - Reconciliação distribuída por região
  - Processamento hierárquico de dados
  - Coordenação de múltiplos sistemas

### 4. **Continue As New - Workflows Infinitos**

#### 4.1 Transferências Recorrentes Infinitas
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Workflow.continueAsNew()` para evitar crescimento infinito do histórico
  - `@SignalMethod` para modificar recorrência
  - `@QueryMethod` para status da próxima execução
- **Implementação**:
  ```java
  @Override
  public void executeRecurringTransfer(RecurringTransferRequest request) {
      // Executar transferência atual
      executeCurrentTransfer(request);
      
      // Aguardar próxima execução
      Workflow.sleep(request.getInterval());
      
      // Verificar se deve continuar
      if (shouldContinue && !cancelled) {
          // Continue as new para evitar histórico infinito
          Workflow.continueAsNew(request.withNextExecution());
      }
  }
  
  @SignalMethod
  public void cancelRecurring() {
      this.cancelled = true;
  }
  
  @QueryMethod  
  public LocalDateTime getNextExecution() {
      return nextExecutionTime;
  }
  ```
- **Casos de Uso**:
  - Débito automático mensal
  - Transferências de poupança programada
  - Pagamentos recorrentes infinitos

#### 4.2 Monitoramento Contínuo de Contas
**Complexidade**: ⭐⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Continue As New` para monitoramento perpétuo
  - `Side Effects` para chamadas não-determinísticas
  - `Local Activities` para verificações rápidas
- **Implementação**:
  ```java
  @Override
  public void monitorAccount(String accountNumber) {
      while (true) {
          // Verificação local rápida
          boolean needsDetailedCheck = Workflow.newLocalActivityStub(
              MonitoringActivities.class
          ).quickBalanceCheck(accountNumber);
          
          if (needsDetailedCheck) {
              // Side effect para timestamp não-determinístico
              long timestamp = Workflow.sideEffect(Long.class, System::currentTimeMillis);
              
              // Análise detalhada
              activities.performDetailedAnalysis(accountNumber, timestamp);
          }
          
          // Aguardar próxima verificação
          Workflow.sleep(Duration.ofMinutes(5));
          
          // Continue as new a cada 24h para limpar histórico
          if (shouldResetDaily()) {
              Workflow.continueAsNew(accountNumber);
          }
      }
  }
  ```
- **Casos de Uso**:
  - Monitoramento de fraude 24/7
  - Alertas de saldo baixo
  - Compliance contínuo

### 5. **Side Effects & Local Activities - Otimizações Avançadas**

#### 5.1 Geração Segura de IDs e Timestamps
**Complexidade**: ⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Workflow.sideEffect()` para operações não-determinísticas
  - `Workflow.getVersion()` para evolução segura
- **Implementação**:
  ```java
  // Geração segura de UUID
  String correlationId = Workflow.sideEffect(
      String.class, 
      () -> UUID.randomUUID().toString()
  );
  
  // Timestamp não-determinístico
  long processingTime = Workflow.sideEffect(
      Long.class, 
      System::currentTimeMillis
  );
  
  // Random para jitter em retry
  int jitterMs = Workflow.sideEffect(
      Integer.class,
      () -> new Random().nextInt(1000)
  );
  ```
- **Casos de Uso**:
  - IDs únicos para correlação
  - Timestamps para auditoria
  - Jitter para evitar thundering herd

#### 5.2 Cache Local para Dados Frequentes
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Local Activities` para operações rápidas sem persistência
  - Cache em memória do worker
  - Fallback para activities normais
- **Implementação**:
  ```java
  // Local activity para cache hit
  private final LocalActivityOptions localOptions = LocalActivityOptions.newBuilder()
      .setStartToCloseTimeout(Duration.ofSeconds(2))
      .build();
      
  private final CacheActivities localCache = 
      Workflow.newLocalActivityStub(CacheActivities.class, localOptions);
  
  // Tentar cache local primeiro
  Optional<ExchangeRate> cachedRate = localCache.getCachedExchangeRate(currency);
  
  if (cachedRate.isEmpty()) {
      // Fallback para activity normal
      ExchangeRate rate = activities.fetchExchangeRate(currency);
      localCache.cacheExchangeRate(currency, rate);
      return rate;
  }
  ```
- **Casos de Uso**:
  - Cache de taxas de câmbio
  - Validações rápidas de conta
  - Lookup de configurações

### 6. **Search Attributes & Visibility - Observabilidade Avançada**

#### 6.1 Indexação Customizada de Workflows
**Complexidade**: ⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Search Attributes` para indexação customizada
  - `Workflow.upsertSearchAttributes()` para atualização dinâmica
  - Visibility APIs para consultas complexas
- **Implementação**:
  ```java
  // Definir search attributes no início do workflow
  Map<String, Object> searchAttributes = new HashMap<>();
  searchAttributes.put("AccountNumber", transferRequest.getSourceAccountNumber());
  searchAttributes.put("Amount", transferRequest.getAmount());
  searchAttributes.put("Currency", transferRequest.getCurrency());
  searchAttributes.put("Priority", calculatePriority(transferRequest));
  
  Workflow.upsertSearchAttributes(searchAttributes);
  
  // Atualizar durante execução
  searchAttributes.put("CurrentStatus", currentStatus.name());
  searchAttributes.put("ProcessingTime", processingTimeMs);
  Workflow.upsertSearchAttributes(searchAttributes);
  ```
- **Casos de Uso**:
  - Busca por transferências por conta
  - Filtros por valor e moeda
  - Análise de performance por região

#### 6.2 Dashboard de Monitoramento em Tempo Real
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `List Workflows API` com filtros avançados
  - `Describe Workflow` para detalhes específicos
  - `Query Workflow` para estado interno
- **Implementação**:
  ```java
  // Buscar workflows por critérios
  ListWorkflowExecutionsRequest request = ListWorkflowExecutionsRequest.newBuilder()
      .setQuery("WorkflowType='MoneyTransferWorkflow' AND ExecutionStatus='Running' AND Amount > 10000")
      .build();
  
  // Consultar estado interno via Query
  WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
  TransferResponse status = stub.query("getStatus", TransferResponse.class);
  
  // Métricas agregadas
  String aggregationQuery = "SELECT COUNT(*) as total, AVG(Amount) as avgAmount " +
                           "FROM workflows WHERE WorkflowType='MoneyTransferWorkflow'";
  ```
- **Casos de Uso**:
  - Dashboard executivo em tempo real
  - Alertas baseados em métricas
  - Análise de tendências

### 7. **Schedules - Execução Programada Nativa**

#### 7.1 Transferências Programadas com Cron
**Complexidade**: ⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Schedule` para execução baseada em cron
  - `ScheduleSpec` para definir periodicidade complexa
  - `SchedulePolicy` para controle de sobreposição
- **Implementação**:
  ```java
  // Criar schedule para transferências mensais
  ScheduleClient scheduleClient = ScheduleClient.newInstance(workflowServiceStubs);
  
  Schedule schedule = Schedule.newBuilder()
      .setAction(ScheduleActionStartWorkflow.newBuilder()
          .setWorkflowType("MoneyTransferWorkflow")
          .setArguments(transferRequest)
          .build())
      .setSpec(ScheduleSpec.newBuilder()
          .setCronExpressions(Arrays.asList("0 9 1 * *")) // Todo dia 1 às 9h
          .build())
      .setPolicy(SchedulePolicy.newBuilder()
          .setOverlapPolicy(ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_SKIP)
          .build())
      .build();
      
  scheduleClient.createSchedule("monthly-salary-" + accountNumber, schedule);
  ```
- **Casos de Uso**:
  - Pagamento de salários mensais
  - Transferências de poupança automática
  - Débitos recorrentes

#### 7.2 Manutenção Programada de Sistema
**Complexidade**: ⭐⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Schedule` com múltiplas expressões cron
  - `Pause/Unpause Schedule` para controle dinâmico
  - Integration com signals para coordenação
- **Implementação**:
  ```java
  // Schedule para diferentes tipos de manutenção
  ScheduleSpec maintenanceSpec = ScheduleSpec.newBuilder()
      .setCronExpressions(Arrays.asList(
          "0 2 * * 0",    // Backup semanal - Domingo 2h
          "0 3 1 * *",    // Reconciliação mensal - Dia 1 às 3h
          "0 1 * * 1-5"   // Limpeza diária - Segunda a sexta 1h
      ))
      .build();
      
  // Pausar schedule durante emergências
  scheduleClient.pauseSchedule("system-maintenance");
  
  // Retomar após resolução
  scheduleClient.unpauseSchedule("system-maintenance");
  ```
- **Casos de Uso**:
  - Backup automático de dados
  - Reconciliação programada
  - Limpeza de dados temporários

### 8. **Workflow Updates - Modificação em Tempo Real**

#### 8.1 Atualização de Parâmetros Durante Execução
**Complexidade**: ⭐⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `@UpdateMethod` para modificações seguras
  - `@UpdateValidatorMethod` para validação
  - Versionamento para compatibilidade
- **Implementação**:
  ```java
  @UpdateMethod
  public void updateTransferAmount(BigDecimal newAmount) {
      if (currentStatus == TransferStatus.INITIATED || 
          currentStatus == TransferStatus.VALIDATED) {
          this.transferAmount = newAmount;
          // Revalidar com novo valor
          Workflow.await(() -> revalidationComplete);
      }
  }
  
  @UpdateValidatorMethod(updateName = "updateTransferAmount")
  public void validateAmountUpdate(BigDecimal newAmount) {
      if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
          throw new IllegalArgumentException("Amount must be positive");
      }
      if (currentStatus == TransferStatus.COMPLETED) {
          throw new IllegalStateException("Cannot update completed transfer");
      }
  }
  
  @QueryMethod
  public TransferUpdateStatus getUpdateCapabilities() {
      return new TransferUpdateStatus(currentStatus, allowedUpdates);
  }
  ```
- **Casos de Uso**:
  - Correção de valores antes da execução
  - Mudança de conta destino
  - Atualização de prioridade

#### 8.2 Migração de Workflows em Produção
**Complexidade**: ⭐⭐⭐⭐⭐
- **Temporal Features Utilizadas**:
  - `Workflow.getVersion()` para versionamento
  - `Update` para migração gradual
  - `Reset` para correção de estado
- **Implementação**:
  ```java
  @Override
  public TransferResponse executeTransfer(TransferRequest request) {
      int version = Workflow.getVersion("add-fraud-check", 
          Workflow.DEFAULT_VERSION, 1);
      
      if (version == Workflow.DEFAULT_VERSION) {
          // Versão antiga - sem fraud check
          return executeTransferV1(request);
      } else {
          // Versão nova - com fraud check
          return executeTransferV2(request);
      }
  }
  
  @UpdateMethod
  public void migrateToNewVersion() {
      // Migrar estado para nova versão
      this.fraudCheckEnabled = true;
      this.migrationComplete = true;
  }
  ```
- **Casos de Uso**:
  - Deploy gradual de novas funcionalidades
  - Correção de bugs em workflows ativos
  - Migração de schema de dados

## Roadmap de Implementação Focado em Features Temporal

### ✅ **CONCLUÍDO - Correções de Base (23/09/2025)**
**Status**: ✅ **COMPLETO** - Build bem-sucedido
- ✅ **WorkflowFeatureConfig** - Implementação completa com @ConfigurationProperties
- ✅ **Duplicação de código** - Removida entre TransferService e TransferControlService
- ✅ **Testes corrigidos** - TransferControlServiceTest atualizado
- ✅ **Qualidade de código** - Validações, tratamento de exceções e constantes
- ✅ **Build do projeto** - Todos os módulos compilam sem erros

### Fase 1: Comunicação e Controle (1-2 semanas) - **EM ANDAMENTO**
**Prioridade**: 🔥 Alta - Features fundamentais não utilizadas
**Status**: 🚧 **PRONTO PARA INICIAR** - Base técnica estabelecida
1. **Signals & Queries** - Pausar/retomar transferências, consulta de status
2. **Timers** - Delays configuráveis e timeouts escalonados
3. **Search Attributes** - Indexação para observabilidade

### Fase 2: Orquestração Avançada (2-3 semanas)  
**Prioridade**: 🔥 Alta - Explorar capacidades de composição
1. **Child Workflows** - Transferências em lote hierárquicas
2. **Side Effects** - Geração segura de IDs e timestamps
3. **Local Activities** - Cache local para otimização

### Fase 3: Workflows de Longa Duração (2-3 semanas)
**Prioridade**: 🟡 Média - Casos de uso específicos
1. **Continue As New** - Transferências recorrentes infinitas
2. **Schedules** - Execução programada com cron nativo
3. **Monitoramento Contínuo** - Workflows perpétuos de análise

### Fase 4: Funcionalidades Avançadas (3-4 semanas)
**Prioridade**: 🟢 Baixa - Features experimentais
1. **Workflow Updates** - Modificação em tempo real
2. **Versioning** - Migração segura de workflows
3. **Advanced Visibility** - Dashboards e métricas customizadas

## Benefícios das Features Temporal Específicas

### 🔄 **Signals & Queries**
- **Interação em tempo real** com workflows em execução
- **Controle dinâmico** sem necessidade de restart
- **Observabilidade interna** do estado do workflow
- **Integração com sistemas externos** via webhooks

### ⏰ **Timers & Delays**
- **Controle temporal preciso** sem dependências externas
- **Timeouts canceláveis** para flexibilidade
- **Jitter automático** para evitar thundering herd
- **SLA enforcement** nativo

### 👨‍👩‍👧‍👦 **Child Workflows**
- **Composição hierárquica** para casos complexos
- **Isolamento de falhas** por sub-processo
- **Paralelização natural** de operações
- **Reutilização de workflows** existentes

### 🔄 **Continue As New**
- **Workflows infinitos** sem crescimento de memória
- **Histórico controlado** para compliance
- **Performance constante** independente do tempo de execução
- **Recorrência nativa** sem schedulers externos

### 🔍 **Search Attributes**
- **Indexação customizada** para business queries
- **Dashboards em tempo real** sem ETL
- **Alertas baseados em estado** do workflow
- **Análise de tendências** nativa

### 📅 **Schedules**
- **Cron nativo** sem dependência de schedulers
- **Controle de sobreposição** automático
- **Pause/resume** dinâmico
- **Timezone awareness** nativo

### 🔧 **Updates & Versioning**
- **Modificação segura** de workflows ativos
- **Deploy sem downtime** de novas versões
- **Migração gradual** de workflows
- **Rollback seguro** de mudanças

## Features Temporal Mais Impactantes para Implementar

### 🥇 **Top Priority - Máximo Impacto**

#### 1. **Signals & Queries** 
**Impacto**: 🔥🔥🔥🔥🔥
- Transforma workflows de "fire-and-forget" para interativos
- Permite controle em tempo real sem restart
- Base para todas as outras funcionalidades avançadas

#### 2. **Child Workflows**
**Impacto**: 🔥🔥🔥🔥🔥  
- Unlock para processamento em lote real
- Composição hierárquica natural
- Isolamento de falhas por sub-processo

#### 3. **Search Attributes**
**Impacto**: 🔥🔥🔥🔥
- Observabilidade business-level imediata
- Dashboards sem ETL complexo
- Base para alertas e monitoramento

### 🥈 **High Priority - Alto Impacto**

#### 4. **Timers & Delays**
**Impacto**: 🔥🔥🔥🔥
- Controle temporal nativo
- SLA enforcement automático
- Eliminação de dependências externas

#### 5. **Side Effects & Local Activities**
**Impacto**: 🔥🔥🔥
- Performance optimization significativa
- Operações não-determinísticas seguras
- Cache local para dados frequentes

### 🥉 **Medium Priority - Casos Específicos**

#### 6. **Continue As New**
**Impacto**: 🔥🔥🔥
- Workflows infinitos para casos específicos
- Performance constante em long-running processes
- Compliance com retenção de dados

#### 7. **Schedules**
**Impacto**: 🔥🔥
- Substituição de schedulers externos
- Cron nativo com controle avançado
- Casos de uso específicos (pagamentos recorrentes)

### 🔬 **Experimental - Funcionalidades Avançadas**

#### 8. **Workflow Updates**
**Impacto**: 🔥
- Modificação em tempo real (casos raros)
- Deploy sem downtime (casos específicos)
- Complexidade alta vs benefício

## Conclusão

O Temporal.io oferece um conjunto robusto de funcionalidades que permitem implementar sistemas bancários altamente resilientes e escaláveis. As funcionalidades propostas exploram todo o potencial da plataforma, desde workflows simples até orquestrações complexas com múltiplos sistemas externos.

A implementação gradual dessas funcionalidades permitirá:
1. **Aprendizado incremental** das capacidades do Temporal
2. **Validação de conceitos** em ambiente controlado
3. **Evolução arquitetural** baseada em necessidades reais
4. **Preparação para produção** com padrões enterprise

Cada funcionalidade adiciona uma camada de complexidade que demonstra diferentes aspectos do Temporal.io, desde durabilidade básica até orquestração distribuída complexa, preparando o sistema para cenários reais de produção bancária.