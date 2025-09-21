#!/bin/bash

echo "🧪 === Teste de Transferência ==="

# Verificar se os serviços estão rodando
echo "1. 🔍 Verificando serviços..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "❌ Account-service não está rodando (porta 8081)"
    exit 1
fi

if ! curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "❌ Transfer-service não está rodando (porta 8082)"
    exit 1
fi

echo "✅ Serviços estão rodando"

# Criar contas de teste
echo "2. 🏦 Criando contas de teste..."

ACCOUNT1=$(curl -s -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "TEST001",
    "ownerName": "João Teste",
    "balance": 1000.00,
    "currency": "BRL"
  }')

ACCOUNT2=$(curl -s -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "TEST002",
    "ownerName": "Maria Teste",
    "balance": 500.00,
    "currency": "BRL"
  }')

echo "✅ Contas criadas"

# Executar transferência
echo "3. 💸 Executando transferência..."
TRANSFER_RESPONSE=$(curl -s -X POST http://localhost:8082/api/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "TEST001",
    "destinationAccountNumber": "TEST002",
    "amount": 50.00,
    "currency": "BRL"
  }')

echo "Resposta da transferência:"
echo "$TRANSFER_RESPONSE" | jq . 2>/dev/null || echo "$TRANSFER_RESPONSE"

# Extrair workflow ID se possível
WORKFLOW_ID=$(echo "$TRANSFER_RESPONSE" | jq -r '.transferId // .workflowId // empty' 2>/dev/null)

if [ -n "$WORKFLOW_ID" ]; then
    echo "4. 📊 Workflow ID: $WORKFLOW_ID"
    echo "   Monitore em: http://localhost:8088"
    
    # Aguardar um pouco e verificar status
    echo "5. ⏳ Aguardando processamento (10 segundos)..."
    sleep 10
    
    echo "6. 🔍 Verificando status final..."
    STATUS_RESPONSE=$(curl -s "http://localhost:8082/api/transfers/$WORKFLOW_ID" 2>/dev/null)
    echo "Status final:"
    echo "$STATUS_RESPONSE" | jq . 2>/dev/null || echo "$STATUS_RESPONSE"
else
    echo "⚠️  Não foi possível extrair o workflow ID"
fi

echo
echo "✅ Teste concluído!"
echo "💡 Verifique os logs dos serviços para mais detalhes"