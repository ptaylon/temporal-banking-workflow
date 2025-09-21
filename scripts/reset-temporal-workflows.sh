#!/bin/bash

echo "🔄 === Reset de Workflows Temporal ==="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "1. 🛑 Parando transfer-service..."
pkill -f "transfer-service.*jar" 2>/dev/null || true
sleep 3

echo "2. 📊 Listando workflows ativos..."
WORKFLOWS=$(curl -s "http://localhost:7233/api/v1/namespaces/default/workflows" 2>/dev/null)
if [ $? -eq 0 ]; then
    WORKFLOW_COUNT=$(echo "$WORKFLOWS" | jq '.executions | length' 2>/dev/null || echo "0")
    echo "   Workflows encontrados: $WORKFLOW_COUNT"
    
    if [ "$WORKFLOW_COUNT" -gt 0 ]; then
        echo "   Terminando workflows em execução..."
        echo "$WORKFLOWS" | jq -r '.executions[].execution.workflowId' 2>/dev/null | while read -r workflow_id; do
            if [ -n "$workflow_id" ]; then
                echo "     Terminando workflow: $workflow_id"
                curl -s -X POST "http://localhost:7233/api/v1/namespaces/default/workflows/$workflow_id/terminate" \
                    -H "Content-Type: application/json" \
                    -d '{"reason": "Manual cleanup"}' > /dev/null 2>&1
            fi
        done
    fi
else
    echo -e "   ${YELLOW}⚠️  Não foi possível consultar workflows${NC}"
fi

echo "3. ⏳ Aguardando limpeza..."
sleep 5

echo "4. 🔍 Verificando limpeza..."
WORKFLOWS_AFTER=$(curl -s "http://localhost:7233/api/v1/namespaces/default/workflows" 2>/dev/null)
if [ $? -eq 0 ]; then
    WORKFLOW_COUNT_AFTER=$(echo "$WORKFLOWS_AFTER" | jq '.executions | length' 2>/dev/null || echo "0")
    echo "   Workflows restantes: $WORKFLOW_COUNT_AFTER"
else
    echo -e "   ${YELLOW}⚠️  Não foi possível verificar workflows${NC}"
fi

echo
echo -e "${GREEN}✅ Reset concluído!${NC}"
echo
echo "📋 Próximos passos:"
echo "1. Recompile o transfer-service: mvn clean package -pl transfer-service -DskipTests"
echo "2. Inicie o transfer-service: java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar"
echo "3. Teste um novo workflow: ./scripts/test-transfer.sh"
echo "4. Monitore via UI: http://localhost:8088"