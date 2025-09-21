# Banking Demo - Makefile Principal
# Use 'make help' para ver todos os comandos disponíveis

.PHONY: help setup build test clean debug fix

# Cores para output
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
RED := \033[0;31m
NC := \033[0m # No Color

# Configurações
DOCKER_COMPOSE_FILE := docker-compose-banking.yml
SERVICES := account-service transfer-service validation-service notification-service audit-service

help: ## 📋 Mostra todos os comandos disponíveis
	@echo "$(BLUE)🏦 Banking Demo - Comandos Disponíveis$(NC)"
	@echo ""
	@echo "$(YELLOW)📦 Setup e Inicialização:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*setup|setup.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)🔨 Build e Deploy:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*build|build.*|compile.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)🧪 Testes:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*test|test.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)🔍 Debug e Diagnóstico:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*debug|debug.*|diagnose.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)🛠️ Correções:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*fix|fix.*|reset.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)🧹 Limpeza:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## .*clean|clean.*|stop.*/ {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ============================================================================
# 📦 SETUP E INICIALIZAÇÃO
# ============================================================================

setup: ## 🚀 Setup completo do projeto (infraestrutura + CDC + build)
	@echo "$(BLUE)🚀 Iniciando setup completo...$(NC)"
	@$(MAKE) setup-infra
	@$(MAKE) setup-cdc
	@$(MAKE) build-all
	@echo "$(GREEN)✅ Setup completo concluído!$(NC)"

setup-infra: ## 🐳 Inicia toda a infraestrutura Docker
	@echo "$(BLUE)🐳 Iniciando infraestrutura...$(NC)"
	@docker-compose -f $(DOCKER_COMPOSE_FILE) down || true
	@docker-compose -f $(DOCKER_COMPOSE_FILE) up -d
	@echo "$(YELLOW)⏳ Aguardando serviços ficarem prontos...$(NC)"
	@sleep 30
	@$(MAKE) check-infra

setup-cdc: ## 🔗 Configura CDC (Debezium + PostgreSQL)
	@echo "$(BLUE)🔗 Configurando CDC...$(NC)"
	@docker exec banking-postgres psql -U postgres -d banking_demo -c "\
		DO \$$\$$ \
		BEGIN \
			IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'dbz_publication') THEN \
				CREATE PUBLICATION dbz_publication FOR TABLE public.accounts, public.transfers; \
			END IF; \
		END \$$\$$;" || true
	@sleep 5
	@curl -X DELETE http://localhost:8083/connectors/banking-connector 2>/dev/null || true
	@sleep 2
	@curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{ \
		"name": "banking-connector", \
		"config": { \
			"connector.class": "io.debezium.connector.postgresql.PostgresConnector", \
			"database.hostname": "postgres", \
			"database.port": "5432", \
			"database.user": "postgres", \
			"database.password": "postgres", \
			"database.dbname": "banking_demo", \
			"topic.prefix": "banking", \
			"table.include.list": "public.accounts,public.transfers", \
			"plugin.name": "pgoutput", \
			"publication.name": "dbz_publication", \
			"slot.name": "dbz_slot", \
			"key.converter": "org.apache.kafka.connect.json.JsonConverter", \
			"value.converter": "org.apache.kafka.connect.json.JsonConverter", \
			"key.converter.schemas.enable": "false", \
			"value.converter.schemas.enable": "false", \
			"snapshot.mode": "initial" \
		} \
	}' || echo "$(YELLOW)⚠️  Erro ao registrar conector - tente novamente$(NC)"

check-infra: ## 🔍 Verifica se a infraestrutura está funcionando
	@echo "$(BLUE)🔍 Verificando infraestrutura...$(NC)"
	@echo -n "PostgreSQL Main: "
	@curl -s http://localhost:5432 >/dev/null 2>&1 && echo "$(GREEN)✅$(NC)" || echo "$(RED)❌$(NC)"
	@echo -n "PostgreSQL Audit: "
	@curl -s http://localhost:5433 >/dev/null 2>&1 && echo "$(GREEN)✅$(NC)" || echo "$(RED)❌$(NC)"
	@echo -n "Temporal Server: "
	@curl -s http://localhost:7233/api/v1/namespaces >/dev/null 2>&1 && echo "$(GREEN)✅$(NC)" || echo "$(RED)❌$(NC)"
	@echo -n "Kafka: "
	@docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1 && echo "$(GREEN)✅$(NC)" || echo "$(RED)❌$(NC)"
	@echo -n "Debezium Connect: "
	@curl -s http://localhost:8083/ >/dev/null 2>&1 && echo "$(GREEN)✅$(NC)" || echo "$(RED)❌$(NC)"

# ============================================================================
# 🔨 BUILD E DEPLOY
# ============================================================================

build-all: ## 🔨 Compila todos os microserviços
	@echo "$(BLUE)🔨 Compilando todos os serviços...$(NC)"
	@./mvnw clean package -DskipTests
	@echo "$(GREEN)✅ Compilação concluída!$(NC)"

build-service: ## 🔨 Compila um serviço específico (uso: make build-service SERVICE=account-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)❌ Especifique o serviço: make build-service SERVICE=account-service$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)🔨 Compilando $(SERVICE)...$(NC)"
	@./mvnw clean package -pl $(SERVICE) -DskipTests
	@echo "$(GREEN)✅ $(SERVICE) compilado!$(NC)"

run-service: ## 🚀 Executa um serviço específico (uso: make run-service SERVICE=account-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)❌ Especifique o serviço: make run-service SERVICE=account-service$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)🚀 Executando $(SERVICE)...$(NC)"
	@java -jar $(SERVICE)/target/$(SERVICE)-1.0-SNAPSHOT.jar

# ============================================================================
# 🧪 TESTES
# ============================================================================

test-all: ## 🧪 Executa todos os testes
	@echo "$(BLUE)🧪 Executando todos os testes...$(NC)"
	@./mvnw test
	@echo "$(GREEN)✅ Testes concluídos!$(NC)"

test-cdc: ## 🧪 Testa o CDC completo
	@echo "$(BLUE)🧪 Testando CDC...$(NC)"
	@./scripts/test-audit-cdc.sh

test-transfer: ## 🧪 Testa transferência completa
	@echo "$(BLUE)🧪 Testando transferência...$(NC)"
	@./scripts/test-transfer.sh



# ============================================================================
# 🔍 DEBUG E DIAGNÓSTICO
# ============================================================================

debug-all: ## 🔍 Diagnóstico completo do sistema
	@echo "$(BLUE)🔍 === Diagnóstico Completo ===$(NC)"
	@echo ""
	@echo "$(YELLOW)📦 Containers Docker:$(NC)"
	@docker ps --format "table {{.Names}}\t{{.Status}}" | grep banking || echo "$(RED)❌ Nenhum container banking$(NC)"
	@echo ""
	@echo "$(YELLOW)🔗 CDC Status:$(NC)"
	@curl -s http://localhost:8083/connectors/banking-connector/status 2>/dev/null | jq -r '"Conector: " + .connector.state + " | Task: " + .tasks[0].state' || echo "$(RED)❌ CDC não configurado$(NC)"
	@echo ""
	@echo "$(YELLOW)📨 Tópicos Kafka:$(NC)"
	@docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep banking | sed 's/^/  ✓ /' || echo "$(RED)❌ Nenhum tópico banking$(NC)"
	@echo ""
	@echo "$(YELLOW)⚡ Temporal:$(NC)"
	@WORKFLOWS=$$(curl -s "http://localhost:7233/api/v1/namespaces/default/workflows" 2>/dev/null | jq '.executions | length' 2>/dev/null || echo "0"); echo "  Workflows ativos: $$WORKFLOWS"
	@echo ""
	@echo "$(YELLOW)🚀 Microserviços:$(NC)"
	@$(MAKE) debug-services

debug-services: ## 🔍 Verifica status dos microserviços
	@for service in $(SERVICES); do \
		port=$$(case $$service in \
			account-service) echo 8081;; \
			transfer-service) echo 8082;; \
			validation-service) echo 8087;; \
			notification-service) echo 8086;; \
			audit-service) echo 8085;; \
		esac); \
		printf "  %-20s " "$$service:"; \
		curl -s http://localhost:$$port/actuator/health >/dev/null 2>&1 && echo "$(GREEN)✅ UP$(NC)" || echo "$(RED)❌ DOWN$(NC)"; \
	done

debug-cdc: ## 🔍 Debug específico do CDC
	@echo "$(BLUE)🔍 Debug CDC Detalhado$(NC)"
	@echo ""
	@echo "$(YELLOW)1. Status do Conector:$(NC)"
	@curl -s http://localhost:8083/connectors/banking-connector/status 2>/dev/null | jq . || echo "$(RED)❌ Conector não encontrado$(NC)"
	@echo ""
	@echo "$(YELLOW)2. Última mensagem no tópico accounts:$(NC)"
	@timeout 3s docker exec banking-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic banking.public.accounts --from-beginning --max-messages 1 2>/dev/null | head -1 | jq . 2>/dev/null || echo "$(YELLOW)⚠️  Nenhuma mensagem ou formato inválido$(NC)"
	@echo ""
	@echo "$(YELLOW)3. Consumer Groups:$(NC)"
	@docker exec banking-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list 2>/dev/null | grep audit || echo "$(YELLOW)⚠️  Nenhum consumer group audit$(NC)"

debug-temporal: ## 🔍 Debug específico do Temporal
	@echo "$(BLUE)🔍 Debug Temporal Detalhado$(NC)"
	@echo ""
	@echo "$(YELLOW)1. Namespaces:$(NC)"
	@curl -s http://localhost:7233/api/v1/namespaces 2>/dev/null | jq -r '.namespaces[].namespaceInfo.name' | sed 's/^/  ✓ /' || echo "$(RED)❌ Temporal Server offline$(NC)"
	@echo ""
	@echo "$(YELLOW)2. Workflows Ativos:$(NC)"
	@curl -s "http://localhost:7233/api/v1/namespaces/default/workflows" 2>/dev/null | jq -r '.executions[] | "  ID: " + .execution.workflowId + " | Status: " + .status + " | Tipo: " + .type.name' || echo "$(GREEN)✅ Nenhum workflow ativo$(NC)"

# ============================================================================
# 🛠️ CORREÇÕES
# ============================================================================

fix-audit-db: ## 🛠️ Recria tabela de auditoria (deixa Hibernate criar)
	@echo "$(BLUE)🛠️  Recriando tabela de auditoria...$(NC)"
	@docker exec banking-audit-postgres psql -U postgres -d audit_db -c "DROP TABLE IF EXISTS audit_events CASCADE;" || true
	@echo "$(GREEN)✅ Tabela removida - Hibernate criará automaticamente!$(NC)"
	@echo "$(YELLOW)💡 Reinicie o audit-service para recriar a tabela$(NC)"

reset-cdc: ## 🔄 Reset completo do CDC
	@echo "$(BLUE)🔄 Resetando CDC...$(NC)"
	@curl -X DELETE http://localhost:8083/connectors/banking-connector 2>/dev/null || true
	@docker exec banking-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group audit-service --reset-offsets --to-earliest --topic banking.public.accounts --execute 2>/dev/null || true
	@docker exec banking-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group audit-service --reset-offsets --to-earliest --topic banking.public.transfers --execute 2>/dev/null || true
	@$(MAKE) setup-cdc
	@echo "$(GREEN)✅ CDC resetado!$(NC)"

reset-temporal: ## 🔄 Reset workflows Temporal
	@echo "$(BLUE)🔄 Resetando workflows Temporal...$(NC)"
	@./scripts/reset-temporal-workflows.sh

reset-audit-consumer: ## 🔄 Reset consumer do audit-service
	@echo "$(BLUE)🔄 Resetando consumer de auditoria...$(NC)"
	@./scripts/reset-audit-consumer.sh

# ============================================================================
# 🧹 LIMPEZA
# ============================================================================

clean: ## 🧹 Limpeza completa (para containers e build)
	@echo "$(BLUE)🧹 Limpeza completa...$(NC)"
	@$(MAKE) stop
	@./mvnw clean
	@docker system prune -f
	@echo "$(GREEN)✅ Limpeza concluída!$(NC)"

stop: ## 🛑 Para todos os serviços Docker
	@echo "$(BLUE)🛑 Parando serviços...$(NC)"
	@docker-compose -f $(DOCKER_COMPOSE_FILE) down
	@pkill -f ".*-service.*jar" 2>/dev/null || true
	@echo "$(GREEN)✅ Serviços parados!$(NC)"

stop-services: ## 🛑 Para apenas os microserviços Java
	@echo "$(BLUE)🛑 Parando microserviços Java...$(NC)"
	@pkill -f ".*-service.*jar" 2>/dev/null || true
	@echo "$(GREEN)✅ Microserviços parados!$(NC)"

# ============================================================================
# 🌐 URLS ÚTEIS
# ============================================================================

urls: ## 🌐 Mostra URLs úteis do sistema
	@echo "$(BLUE)🌐 URLs do Sistema:$(NC)"
	@echo "  • Temporal UI:    http://localhost:8088"
	@echo "  • Kafka UI:       http://localhost:8090"
	@echo "  • Debezium API:   http://localhost:8083"
	@echo ""
	@echo "$(BLUE)🏦 APIs dos Microserviços:$(NC)"
	@echo "  • Account Service:      http://localhost:8081"
	@echo "  • Transfer Service:     http://localhost:8082"
	@echo "  • Validation Service:   http://localhost:8087"
	@echo "  • Notification Service: http://localhost:8086"
	@echo "  • Audit Service:        http://localhost:8085"