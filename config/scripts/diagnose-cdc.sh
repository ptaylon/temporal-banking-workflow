#!/bin/bash

echo "🔍 === Diagnóstico CDC para Banking Demo ==="
echo

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para verificar status
check_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ OK${NC}"
    else
        echo -e "${RED}❌ ERRO${NC}"
    fi
}

# 1. Verificar containers
echo "1. 🐳 Verificando containers Docker..."
echo "   PostgreSQL Main:"
docker ps --format "{{.Names}}\t{{.Status}}" | grep banking-postgres > /dev/null
check_status

echo "   PostgreSQL Audit:"
docker ps --format "{{.Names}}\t{{.Status}}" | grep banking-audit-postgres > /dev/null
check_status

echo "   Kafka:"
docker ps --format "{{.Names}}\t{{.Status}}" | grep banking-kafka > /dev/null
check_status

echo "   Debezium Connect:"
docker ps --format "{{.Names}}\t{{.Status}}" | grep banking-debezium > /dev/null
check_status

echo "   Temporal:"
docker ps --format "{{.Names}}\t{{.Status}}" | grep banking-temporal > /dev/null
check_status

echo

# 2. Verificar conectividade dos serviços
echo "2. 🌐 Verificando conectividade dos serviços..."

echo "   Debezium Connect (8083):"
curl -s http://localhost:8083/ > /dev/null
check_status

echo "   Temporal Server (7233):"
curl -s http://localhost:7233/api/v1/namespaces > /dev/null
check_status

echo "   Kafka UI (8090):"
curl -s http://localhost:8090 > /dev/null
check_status

echo

# 3. Verificar microserviços
echo "3. 🚀 Verificando microserviços..."

services=("8081:Account" "8082:Transfer" "8085:Audit" "8086:Notification" "8087:Validation")
for service in "${services[@]}"; do
    port="${service%%:*}"
    name="${service##*:}"
    echo "   $name Service ($port):"
    curl -s http://localhost:$port/actuator/health > /dev/null
    check_status
done

echo

# 4. Verificar conector Debezium
echo "4. 🔗 Verificando conector Debezium..."
CONNECTOR_STATUS=$(curl -s http://localhost:8083/connectors/banking-connector/status 2>/dev/null)
if [ $? -eq 0 ] && echo "$CONNECTOR_STATUS" | jq -e '.connector.state == "RUNNING"' > /dev/null 2>&1; then
    echo -e "   Status: ${GREEN}✅ RUNNING${NC}"
    TASKS=$(echo "$CONNECTOR_STATUS" | jq -r '.tasks[].state' 2>/dev/null)
    echo "   Tasks: $TASKS"
else
    echo -e "   Status: ${RED}❌ NÃO ENCONTRADO OU PARADO${NC}"
fi

echo

# 5. Verificar tópicos Kafka
echo "5. 📨 Verificando tópicos Kafka..."
TOPICS=$(docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep banking)
if [ -n "$TOPICS" ]; then
    echo -e "   ${GREEN}✅ Tópicos encontrados:${NC}"
    echo "$TOPICS" | sed 's/^/     - /'
else
    echo -e "   ${RED}❌ Nenhum tópico banking encontrado${NC}"
fi

echo

# 6. Verificar publicação PostgreSQL
echo "6. 📊 Verificando configuração PostgreSQL..."
PUB_CHECK=$(docker exec banking-postgres psql -U postgres -d banking_demo -t -c "SELECT count(*) FROM pg_publication WHERE pubname = 'dbz_publication';" 2>/dev/null | tr -d ' ')
if [ "$PUB_CHECK" = "1" ]; then
    echo -e "   Publicação: ${GREEN}✅ dbz_publication existe${NC}"
    # Verificar tabelas na publicação
    TABLES=$(docker exec banking-postgres psql -U postgres -d banking_demo -t -c "SELECT tablename FROM pg_publication_tables WHERE pubname = 'dbz_publication';" 2>/dev/null | tr -d ' ' | tr '\n' ',' | sed 's/,$//')
    echo "     Tabelas: $TABLES"
else
    echo -e "   Publicação: ${RED}❌ dbz_publication não encontrada${NC}"
fi

SLOT_CHECK=$(docker exec banking-postgres psql -U postgres -d banking_demo -t -c "SELECT count(*) FROM pg_replication_slots WHERE slot_name = 'dbz_slot';" 2>/dev/null | tr -d ' ')
if [ "$SLOT_CHECK" = "1" ]; then
    echo -e "   Slot de replicação: ${GREEN}✅ dbz_slot existe${NC}"
    # Verificar se está ativo
    SLOT_ACTIVE=$(docker exec banking-postgres psql -U postgres -d banking_demo -t -c "SELECT active FROM pg_replication_slots WHERE slot_name = 'dbz_slot';" 2>/dev/null | tr -d ' ')
    echo "     Status: $SLOT_ACTIVE"
else
    echo -e "   Slot de replicação: ${RED}❌ dbz_slot não encontrado${NC}"
fi

# Verificar configurações WAL
WAL_LEVEL=$(docker exec banking-postgres psql -U postgres -d banking_demo -t -c "SHOW wal_level;" 2>/dev/null | tr -d ' ')
echo "   WAL Level: $WAL_LEVEL"

echo

# 7. Resumo e recomendações
echo "7. 📋 Resumo e Recomendações:"
echo
if [ "$PUB_CHECK" != "1" ] || [ "$SLOT_CHECK" != "1" ]; then
    echo -e "   ${YELLOW}⚠️  Execute: ./scripts/setup-cdc-complete.sh${NC}"
fi

if [ -z "$TOPICS" ]; then
    echo -e "   ${YELLOW}⚠️  Tópicos Kafka não encontrados. Verifique o conector Debezium.${NC}"
fi

echo -e "   ${GREEN}💡 Para testar o sistema completo: ./scripts/test-audit-cdc.sh${NC}"
echo -e "   ${GREEN}💡 Interfaces web:${NC}"
echo "     - Temporal UI: http://localhost:8088"
echo "     - Kafka UI: http://localhost:8090"

echo
echo "🏁 === Fim do diagnóstico ==="