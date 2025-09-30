#!/bin/bash

echo "=== Teste do CDC para Audit Service ==="

# 1. Verificar se os serviços estão rodando
echo "1. Verificando serviços..."
echo "Postgres: $(curl -s http://localhost:5432 2>/dev/null && echo "OK" || echo "ERRO")"
echo "Kafka: $(docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | wc -l) tópicos"
echo "Debezium: $(curl -s http://localhost:8083/ 2>/dev/null && echo "OK" || echo "ERRO")"
echo "Audit Service: $(curl -s http://localhost:8085/actuator/health 2>/dev/null | jq -r .status || echo "ERRO")"
echo

# 2. Verificar tópicos CDC
echo "2. Tópicos CDC disponíveis:"
docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep banking || echo "Nenhum tópico banking encontrado"
echo

# 3. Criar uma conta de teste
echo "3. Criando conta de teste..."
ACCOUNT_RESPONSE=$(curl -s -X POST http://localhost:8081/api/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "TEST123",
    "ownerName": "Test User",
    "balance": 1000.00,
    "currency": "USD"
  }')

echo "Resposta da criação da conta: $ACCOUNT_RESPONSE"
echo

# 4. Aguardar processamento CDC
echo "4. Aguardando processamento CDC (5 segundos)..."
sleep 5

# 5. Verificar eventos de auditoria
echo "5. Verificando eventos de auditoria para a conta TEST123..."
AUDIT_RESPONSE=$(curl -s http://localhost:8085/api/audit/accounts/TEST123)
echo "Eventos de auditoria: $AUDIT_RESPONSE"
echo

# 6. Verificar mensagens no tópico Kafka
echo "6. Últimas mensagens no tópico banking.public.accounts:"
docker exec banking-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic banking.public.accounts \
  --from-beginning \
  --max-messages 1 \
  --timeout-ms 5000 2>/dev/null || echo "Nenhuma mensagem encontrada"

echo
echo "=== Fim do teste ==="