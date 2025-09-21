#!/bin/bash

echo "🔍 === Diagnóstico Temporal.io ==="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "1. 🌐 Verificando conectividade com Temporal Server..."
TEMPORAL_STATUS=$(curl -s http://localhost:7233/api/v1/namespaces 2>/dev/null)
if [ $? -eq 0 ]; then
    echo -e "   ${GREEN}✅ Temporal Server respondendo${NC}"
    echo "   Namespaces disponíveis:"
    echo "$TEMPORAL_STATUS" | jq -r '.namespaces[].namespaceInfo.name' 2>/dev/null | sed 's/^/     - /' || echo "     - default"
else
    echo -e "   ${RED}❌ Temporal Server não está respondendo${NC}"
    echo "   Verifique se o container está rodando: docker ps | grep temporal"
fi

echo
echo "2. 📊 Verificando workflows ativos..."
WORKFLOWS=$(curl -s "http://localhost:7233/api/v1/namespaces/default/workflows" 2>/dev/null)
if [ $? -eq 0 ]; then
    WORKFLOW_COUNT=$(echo "$WORKFLOWS" | jq '.executions | length' 2>/dev/null || echo "0")
    echo "   Workflows ativos: $WORKFLOW_COUNT"
    
    if [ "$WORKFLOW_COUNT" -gt 0 ]; then
        echo "   Workflows em execução:"
        echo "$WORKFLOWS" | jq -r '.executions[] | "     - ID: \(.execution.workflowId) | Status: \(.status) | Tipo: \(.type.name)"' 2>/dev/null || echo "     Erro ao listar workflows"
    fi
else
    echo -e "   ${YELLOW}⚠️  Não foi possível consultar workflows${NC}"
fi

echo
echo "3. 🔧 Verificando configurações do transfer-service..."
if pgrep -f "transfer-service.*jar" > /dev/null; then
    echo -e "   ${GREEN}✅ Transfer-service está rodando${NC}"
    
    # Verificar logs recentes
    echo "   Últimos logs (últimas 5 linhas):"
    tail -5 /tmp/transfer-service.log 2>/dev/null | sed 's/^/     /' || echo "     Logs não encontrados em /tmp/transfer-service.log"
else
    echo -e "   ${RED}❌ Transfer-service não está rodando${NC}"
fi

echo
echo "4. 🚨 Problemas comuns e soluções:"
echo -e "   ${BLUE}Problema: Workflow task not found${NC}"
echo "   Causa: Timeout ou concorrência"
echo "   Solução: Aumentar timeouts, evitar debug com breakpoints"
echo
echo -e "   ${BLUE}Problema: UnableToAcquireLockException${NC}"
echo "   Causa: Múltiplos workers ou timeout de task"
echo "   Solução: Configurar maxConcurrentWorkflowTaskExecutions=1"
echo
echo -e "   ${BLUE}Problema: Activity already timed out${NC}"
echo "   Causa: Activity demorou mais que o timeout configurado"
echo "   Solução: Aumentar startToCloseTimeout das activities"

echo
echo "5. 💡 Comandos úteis:"
echo "   # Ver workflows via Temporal UI"
echo "   open http://localhost:8088"
echo
echo "   # Reiniciar transfer-service"
echo "   pkill -f transfer-service"
echo "   java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar"
echo
echo "   # Ver logs em tempo real"
echo "   tail -f /tmp/transfer-service.log"

echo
echo "6. 🔄 Teste rápido de workflow:"
echo "   curl -X POST http://localhost:8082/api/transfers \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"sourceAccountNumber\":\"123456\",\"destinationAccountNumber\":\"789012\",\"amount\":10.00,\"currency\":\"BRL\"}'"

echo
echo "=== Fim do diagnóstico ==="