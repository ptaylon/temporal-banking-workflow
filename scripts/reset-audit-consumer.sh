#!/bin/bash

echo "🔄 === Reset do Consumer Audit Service ==="

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "1. 🛑 Parando audit-service se estiver rodando..."
# Tentar parar o processo Java do audit-service
pkill -f "audit-service.*jar" 2>/dev/null || true
sleep 2

echo "2. 🗑️  Resetando consumer group..."
# Reset do consumer group para começar do início
docker exec banking-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group audit-service \
  --reset-offsets \
  --to-earliest \
  --topic banking.public.accounts \
  --execute 2>/dev/null || echo "   Tópico banking.public.accounts não encontrado"

docker exec banking-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group audit-service \
  --reset-offsets \
  --to-earliest \
  --topic banking.public.transfers \
  --execute 2>/dev/null || echo "   Tópico banking.public.transfers não encontrado"

echo "3. 🧹 Limpando arquivos de offset locais..."
rm -f audit-offsets.dat 2>/dev/null || true
rm -f offsets.dat 2>/dev/null || true

echo "4. 📊 Verificando tópicos disponíveis..."
TOPICS=$(docker exec banking-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep banking)
if [ -n "$TOPICS" ]; then
    echo -e "   ${GREEN}✅ Tópicos encontrados:${NC}"
    echo "$TOPICS" | sed 's/^/     - /'
else
    echo -e "   ${YELLOW}⚠️  Nenhum tópico banking encontrado${NC}"
    echo "   Execute: ./scripts/setup-cdc-complete.sh"
fi

echo "5. 🔍 Verificando mensagens nos tópicos..."
for topic in banking.public.accounts banking.public.transfers; do
    echo "   Verificando $topic..."
    MSG_COUNT=$(docker exec banking-kafka kafka-run-class kafka.tools.GetOffsetShell \
        --broker-list localhost:9092 \
        --topic $topic 2>/dev/null | awk -F: '{sum += $3} END {print sum}' || echo "0")
    echo "     Mensagens: $MSG_COUNT"
done

echo
echo -e "${GREEN}✅ Reset concluído!${NC}"
echo
echo "📋 Próximos passos:"
echo "1. Recompile o audit-service: mvn clean package -pl audit-service -DskipTests"
echo "2. Inicie o audit-service: java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar"
echo "3. Teste com: ./scripts/test-audit-cdc.sh"