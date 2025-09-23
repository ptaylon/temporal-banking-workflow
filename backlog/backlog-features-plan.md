# Backlog de Implementação - Features Avançadas Temporal.io

## 📋 Visão Geral do Backlog

Este backlog contém tarefas específicas para implementar as funcionalidades avançadas do Temporal.io identificadas no `features-plan.md`. As tarefas estão organizadas por prioridade e complexidade, seguindo uma abordagem incremental.

**Status Legend:**
- [ ] **Não Iniciado** - Tarefa não começada
- [🔄] **Em Progresso** - Tarefa sendo executada
- [✅] **Concluído** - Tarefa finalizada
- [⚠️] **Bloqueado** - Tarefa com dependências pendentes

---

## 🔥 **FASE 1: Comunicação e Controle (1-2 semanas)**
*Prioridade: ALTA - Features fundamentais não utilizadas*

### Epic 1.1: Signals & Queries - Sistema de Pausar/Retomar

- [ ] **1.1.1** Criar DTOs para controle de transferência
  - Criar `TransferControlRequest.java` com campos pause/resume
  - Criar `TransferControlResponse.java` com status atual
  - Adicionar enum `TransferControlAction` (PAUSE, RESUME, CANCEL)
  - _Estimativa: 2h_

- [ ] **1.1.2** Estender interface MoneyTransferWorkflow com Signals e Queries
  - Adicionar `@SignalMethod void pauseTransfer()`
  - Adicionar `@SignalMethod void resumeTransfer()`
  - Adicionar `@SignalMethod void cancelTransfer(String reason)`
  - Adicionar `@QueryMethod boolean isPaused()`
  - Adicionar `@QueryMethod TransferControlStatus getControlStatus()`
  - _Estimativa: 1h_

- [ ] **1.1.3** Implementar lógica de controle no MoneyTransferWorkflowImpl
  - Adicionar campos de estado: `paused`, `cancelled`, `pauseReason`
  - Implementar métodos signal para controle de estado
  - Implementar métodos query para consulta de estado
  - Adicionar `Workflow.await(() -> !paused)` nos pontos críticos
  - _Estimativa: 4h_

- [ ] **1.1.4** Criar endpoints REST para controle de transferências
  - `POST /api/transfers/{workflowId}/pause` - Pausar transferência
  - `POST /api/transfers/{workflowId}/resume` - Retomar transferência
  - `POST /api/transfers/{workflowId}/cancel` - Cancelar transferência
  - `GET /api/transfers/{workflowId}/control-status` - Status de controle
  - _Estimativa: 3h_

- [ ] **1.1.5** Implementar service layer para controle de workflows
  - Criar `TransferControlService.java`
  - Implementar métodos para enviar signals
  - Implementar métodos para fazer queries
  - Adicionar tratamento de erros e validações
  - _Estimativa: 3h_

- [ ] **1.1.6** Criar testes unitários para funcionalidade de controle
  - Testes para signals (pause/resume/cancel)
  - Testes para queries (status)
  - Testes de integração com workflow
  - Mock de cenários de erro
  - _Estimativa: 4h_

- [ ] **1.1.7** Criar testes end-to-end para controle via API
  - Teste de pausar transferência em execução
  - Teste de retomar transferência pausada
  - Teste de cancelar transferência
  - Teste de consultar status de controle
  - _Estimativa: 3h_

### Epic 1.2: Timers - Delays Configuráveis

- [ ] **1.2.1** Estender TransferRequest com configurações de delay
  - Adicionar campo `delayBeforeExecution` (Duration)
  - Adicionar campo `timeoutAfterDelay` (Duration)
  - Adicionar campo `allowCancelDuringDelay` (boolean)
  - _Estimativa: 1h_

- [ ] **1.2.2** Implementar delay configurável no workflow
  - Adicionar lógica de `Workflow.sleep()` baseada no request
  - Implementar timer cancelável com `Workflow.newTimer()`
  - Adicionar signal para cancelar delay
  - Usar `Promise.anyOf()` para race condition entre timer e cancel
  - _Estimativa: 3h_

- [ ] **1.2.3** Implementar sistema de timeout escalonado
  - Criar enum `TimeoutLevel` (LEVEL_1, LEVEL_2, LEVEL_3)
  - Implementar múltiplos timers com `Promise.anyOf()`
  - Adicionar lógica de escalação baseada no nível atingido
  - Adicionar métricas para cada nível de timeout
  - _Estimativa: 4h_

- [ ] **1.2.4** Criar endpoints para gerenciar delays
  - `POST /api/transfers/{workflowId}/cancel-delay` - Cancelar delay
  - `GET /api/transfers/{workflowId}/delay-status` - Status do delay
  - `PUT /api/transfers/{workflowId}/extend-delay` - Estender delay
  - _Estimativa: 2h_

- [ ] **1.2.5** Adicionar testes para funcionalidades de timer
  - Teste de delay configurável
  - Teste de cancelamento de delay
  - Teste de timeout escalonado
  - Teste de race conditions
  - _Estimativa: 4h_

### Epic 1.3: Search Attributes - Indexação Customizada

- [ ] **1.3.1** Configurar Search Attributes no Temporal Server
  - Definir custom search attributes no cluster
  - Configurar tipos: `AccountNumber` (Keyword), `Amount` (Double), `Currency` (Keyword)
  - Configurar `Priority` (Int), `Region` (Keyword), `Status` (Keyword)
  - _Estimativa: 2h_

- [ ] **1.3.2** Implementar upsert de Search Attributes no workflow
  - Adicionar `Workflow.upsertSearchAttributes()` no início do workflow
  - Atualizar attributes durante mudanças de status
  - Adicionar attributes calculados (priority, processing time)
  - _Estimativa: 2h_

- [ ] **1.3.3** Criar service para consultas avançadas
  - Criar `TransferSearchService.java`
  - Implementar busca por conta, valor, moeda
  - Implementar filtros por status e período
  - Implementar agregações (count, avg, sum)
  - _Estimativa: 4h_

- [ ] **1.3.4** Criar endpoints para busca avançada
  - `GET /api/transfers/search` - Busca com filtros
  - `GET /api/transfers/search/by-account/{accountNumber}` - Por conta
  - `GET /api/transfers/search/by-amount-range` - Por faixa de valor
  - `GET /api/transfers/analytics/summary` - Métricas agregadas
  - _Estimativa: 3h_

- [ ] **1.3.5** Implementar dashboard básico de monitoramento
  - Criar página HTML simples para visualização
  - Implementar gráficos básicos (Chart.js)
  - Mostrar transferências em tempo real
  - Mostrar métricas agregadas
  - _Estimativa: 5h_

- [ ] **1.3.6** Criar testes para search attributes
  - Teste de indexação de attributes
  - Teste de busca por diferentes critérios
  - Teste de agregações
  - Teste de performance com volume
  - _Estimativa: 3h_

---

## 🔥 **FASE 2: Orquestração Avançada (2-3 semanas)**
*Prioridade: ALTA - Explorar capacidades de composição*

### Epic 2.1: Child Workflows - Transferências em Lote

- [ ] **2.1.1** Criar DTOs para transferências em lote
  - Criar `BatchTransferRequest.java` com lista de transferências
  - Criar `BatchTransferResponse.java` com status agregado
  - Criar `BatchTransferStatus.java` para acompanhamento
  - _Estimativa: 2h_

- [ ] **2.1.2** Criar interface e implementação do BatchTransferWorkflow
  - Criar `BatchTransferWorkflow.java` interface
  - Implementar `BatchTransferWorkflowImpl.java`
  - Usar `Workflow.newChildWorkflowStub()` para cada transferência
  - Implementar execução paralela com `Async.function()`
  - _Estimativa: 5h_

- [ ] **2.1.3** Implementar coordenação de child workflows
  - Usar `Promise.allOf()` para aguardar conclusão
  - Implementar tratamento de falhas parciais
  - Adicionar compensação para falhas em lote
  - Implementar retry policy específico para lotes
  - _Estimativa: 4h_

- [ ] **2.1.4** Criar service e controller para lotes
  - Criar `BatchTransferService.java`
  - Implementar validação de lote (limites, duplicatas)
  - Criar endpoints REST para lotes
  - Implementar paginação para lotes grandes
  - _Estimativa: 4h_

- [ ] **2.1.5** Implementar monitoramento de lotes
  - Adicionar queries para status agregado
  - Implementar progress tracking (X de Y concluídas)
  - Adicionar métricas específicas para lotes
  - _Estimativa: 3h_

- [ ] **2.1.6** Criar testes para child workflows
  - Teste de execução paralela
  - Teste de falhas parciais
  - Teste de compensação em lote
  - Teste de performance com volume
  - _Estimativa: 5h_

### Epic 2.2: Side Effects - Geração Segura de IDs

- [ ] **2.2.1** Implementar geração segura de correlation IDs
  - Usar `Workflow.sideEffect()` para UUID generation
  - Adicionar correlation ID em todos os logs
  - Propagar correlation ID para activities
  - _Estimativa: 2h_

- [ ] **2.2.2** Implementar timestamps não-determinísticos
  - Usar `Workflow.sideEffect()` para timestamps
  - Adicionar timestamps para auditoria
  - Implementar jitter para retry policies
  - _Estimativa: 2h_

- [ ] **2.2.3** Criar utility class para side effects
  - Criar `WorkflowSideEffects.java`
  - Implementar métodos helper para operações comuns
  - Adicionar validação e tratamento de erros
  - _Estimativa: 2h_

### Epic 2.3: Local Activities - Cache Local

- [ ] **2.3.1** Criar interface para cache activities
  - Criar `CacheActivities.java` interface
  - Definir métodos para cache de exchange rates
  - Definir métodos para cache de account info
  - _Estimativa: 1h_

- [ ] **2.3.2** Implementar local activities para cache
  - Implementar `CacheActivitiesImpl.java`
  - Usar cache em memória (ConcurrentHashMap)
  - Implementar TTL para cache entries
  - Adicionar fallback para cache miss
  - _Estimativa: 4h_

- [ ] **2.3.3** Integrar cache no workflow principal
  - Usar `Workflow.newLocalActivityStub()`
  - Implementar cache-first strategy
  - Adicionar fallback para activities normais
  - _Estimativa: 2h_

- [ ] **2.3.4** Criar testes para local activities
  - Teste de cache hit/miss
  - Teste de TTL expiration
  - Teste de fallback strategy
  - _Estimativa: 3h_

---

## 🟡 **FASE 3: Workflows de Longa Duração (2-3 semanas)**
*Prioridade: MÉDIA - Casos de uso específicos*

### Epic 3.1: Continue As New - Transferências Recorrentes

- [ ] **3.1.1** Criar DTOs para transferências recorrentes
  - Criar `RecurringTransferRequest.java`
  - Adicionar campos: interval, endDate, maxExecutions
  - Criar `RecurringTransferStatus.java`
  - _Estimativa: 2h_

- [ ] **3.1.2** Criar workflow para transferências recorrentes
  - Criar `RecurringTransferWorkflow.java` interface
  - Implementar `RecurringTransferWorkflowImpl.java`
  - Usar `Workflow.continueAsNew()` para evitar crescimento
  - Implementar signals para cancelar/modificar recorrência
  - _Estimativa: 5h_

- [ ] **3.1.3** Implementar lógica de recorrência
  - Calcular próxima execução baseada no interval
  - Implementar diferentes tipos de interval (daily, weekly, monthly)
  - Adicionar validação de business days
  - _Estimativa: 4h_

- [ ] **3.1.4** Criar endpoints para transferências recorrentes
  - `POST /api/recurring-transfers` - Criar recorrência
  - `GET /api/recurring-transfers/{id}` - Status da recorrência
  - `PUT /api/recurring-transfers/{id}/pause` - Pausar recorrência
  - `DELETE /api/recurring-transfers/{id}` - Cancelar recorrência
  - _Estimativa: 3h_

- [ ] **3.1.5** Implementar monitoramento de recorrências
  - Dashboard para recorrências ativas
  - Alertas para falhas em recorrências
  - Métricas de execução de recorrências
  - _Estimativa: 4h_

### Epic 3.2: Schedules - Execução Programada Nativa

- [ ] **3.2.1** Configurar Temporal Schedules
  - Implementar `ScheduleClient` configuration
  - Criar service para gerenciar schedules
  - Implementar diferentes tipos de cron expressions
  - _Estimativa: 3h_

- [ ] **3.2.2** Criar workflows programados
  - Implementar schedule para backup diário
  - Implementar schedule para reconciliação mensal
  - Implementar schedule para limpeza de dados
  - _Estimativa: 4h_

- [ ] **3.2.3** Implementar controle dinâmico de schedules
  - Pause/unpause schedules via API
  - Modificar schedules em runtime
  - Monitorar execução de schedules
  - _Estimativa: 3h_

---

## 🟢 **FASE 4: Funcionalidades Avançadas (3-4 semanas)**
*Prioridade: BAIXA - Features experimentais*

### Epic 4.1: Workflow Updates - Modificação em Tempo Real

- [ ] **4.1.1** Implementar Update Methods no workflow
  - Adicionar `@UpdateMethod` para atualizar amount
  - Adicionar `@UpdateValidatorMethod` para validação
  - Implementar update de destination account
  - _Estimativa: 4h_

- [ ] **4.1.2** Criar endpoints para updates
  - `PUT /api/transfers/{workflowId}/amount` - Atualizar valor
  - `PUT /api/transfers/{workflowId}/destination` - Atualizar destino
  - `GET /api/transfers/{workflowId}/update-capabilities` - Capacidades
  - _Estimativa: 3h_

- [ ] **4.1.3** Implementar versionamento de workflows
  - Usar `Workflow.getVersion()` para compatibilidade
  - Implementar migração gradual de workflows
  - Criar testes para diferentes versões
  - _Estimativa: 5h_

### Epic 4.2: Advanced Visibility - Dashboards Customizados

- [ ] **4.2.1** Implementar APIs avançadas de visibility
  - Usar `ListWorkflowExecutions` com filtros complexos
  - Implementar agregações customizadas
  - Criar métricas de performance
  - _Estimativa: 4h_

- [ ] **4.2.2** Criar dashboard executivo
  - Implementar dashboard web responsivo
  - Mostrar KPIs em tempo real
  - Implementar alertas visuais
  - _Estimativa: 6h_

---

## 📊 **Métricas de Progresso**

### Por Fase:
- **Fase 1**: 20 tarefas (≈ 40h de desenvolvimento)
- **Fase 2**: 15 tarefas (≈ 35h de desenvolvimento)  
- **Fase 3**: 10 tarefas (≈ 25h de desenvolvimento)
- **Fase 4**: 8 tarefas (≈ 22h de desenvolvimento)

### Por Complexidade:
- **Baixa** (⭐): 15 tarefas
- **Média** (⭐⭐): 20 tarefas
- **Alta** (⭐⭐⭐): 18 tarefas

### Por Tipo:
- **Backend/Core**: 35 tarefas
- **API/Endpoints**: 12 tarefas
- **Testes**: 15 tarefas
- **UI/Dashboard**: 6 tarefas

---

## 🎯 **Próximos Passos Recomendados**

### Começar com:
1. **Epic 1.1** - Signals & Queries (base para tudo)
2. **Epic 1.3** - Search Attributes (observabilidade imediata)
3. **Epic 1.2** - Timers (controle temporal)

### Critérios de Sucesso:
- [ ] Workflows podem ser pausados/retomados via API
- [ ] Transferências são indexadas e pesquisáveis
- [ ] Delays configuráveis funcionam corretamente
- [ ] Testes automatizados passam
- [ ] Performance não degrada

### Dependências Identificadas:
- Temporal Server configurado com Search Attributes
- Infraestrutura de testes atualizada
- Documentação de APIs atualizada

---

## 📝 **Notas de Implementação**

### Padrões a Seguir:
- Usar DTOs específicos para cada funcionalidade
- Implementar testes para cada epic
- Manter backward compatibility
- Documentar APIs com OpenAPI/Swagger
- Seguir padrões de logging e monitoramento existentes

### Riscos Identificados:
- **Performance**: Child workflows podem impactar performance
- **Complexidade**: Updates em tempo real aumentam complexidade
- **Testes**: Testes de workflows longos são desafiadores
- **Observabilidade**: Muitos search attributes podem impactar performance

### Mitigações:
- Implementar feature flags para funcionalidades experimentais
- Monitorar performance continuamente
- Implementar circuit breakers onde necessário
- Documentar troubleshooting guides