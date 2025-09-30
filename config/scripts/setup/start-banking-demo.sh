#!/bin/bash

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🏦 === Banking Demo - Inicialização Completa ===${NC}"
echo

# Função para aguardar serviço
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=1
    
    echo -n "   Aguardando $name"
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e " ${GREEN}✅${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    echo -e " ${RED}❌ Timeout${NC}"
    return 1
}

# 1. Verificar pré-requisitos
echo "1. 🔍 Verificando pré-requisitos..."

# Docker
if command -v docker &> /dev/null; then
    echo -e "   Docker: ${GREEN}✅${NC}"
else
    echo -e "   Docker: ${RED}❌ Não encontrado${NC}"
    exit 1
fi

# Docker Compose
if command -v docker-compose &> /dev/null; then
    echo -e "   Docker Compose: ${GREEN}✅${NC}"
else
    echo -e "   Docker Compose: ${RED}❌ Não encontrado${NC}"
    exit 1
fi

# Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "   Java $JAVA_VERSION: ${GREEN}✅${NC}"
    else
        echo -e "   Java $JAVA_VERSION: ${YELLOW}⚠️  Recomendado Java 21+${NC}"
    fi
else
    echo -e "   Java: ${RED}❌ Não encontrado${NC}"
    exit 1
fi

# Maven
if command -v mvn &> /dev/null || [ -f "./mvnw" ]; then
    echo -e "   Maven: ${GREEN}✅${NC}"
else
    echo -e "   Maven: ${RED}❌ Não encontrado${NC}"
    exit 1
fi

echo

# 2. Parar serviços existentes
echo "2. 🛑 Parando serviços existentes..."
docker-compose -f docker-compose-banking.yml down > /dev/null 2>&1
echo -e "   ${GREEN}✅ Serviços parados${NC}"
echo

# 3. Iniciar infraestrutura
echo "3. 🚀 Iniciando infraestrutura..."
docker-compose -f docker-compose-banking.yml up -d

echo "   Aguardando serviços ficarem prontos..."
wait_for_service "http://localhost:5432" "PostgreSQL Main" &
wait_for_service "http://localhost:5433" "PostgreSQL Audit" &
wait_for_service "http://localhost:9092" "Kafka" &
wait_for_service "http://localhost:8083" "Debezium Connect" &
wait_for_service "http://localhost:7233/api/v1/namespaces" "Temporal Server" &
wait

echo

# 4. Configurar CDC
echo "4. 🔗 Configurando CDC..."
sleep 5  # Aguardar um pouco mais para garantir que tudo está pronto
if ./scripts/setup/setup-cdc-complete.sh > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ CDC configurado com sucesso${NC}"
else
    echo -e "   ${YELLOW}⚠️  Erro na configuração CDC - execute manualmente: ./scripts/setup/setup-cdc-complete.sh${NC}"
fi
echo

# 5. Compilar serviços
echo "5. 🔨 Compilando microserviços..."
if ./mvnw clean package -DskipTests > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ Compilação concluída${NC}"
else
    echo -e "   ${RED}❌ Erro na compilação${NC}"
    echo "   Execute manualmente: ./mvnw clean package -DskipTests"
    exit 1
fi
echo

# 6. Instruções para iniciar serviços
echo "6. 🎯 Próximos passos:"
echo
echo -e "${YELLOW}Para iniciar os microserviços, execute em terminais separados:${NC}"
echo
echo -e "${BLUE}# Account Service${NC}"
echo "java -jar account-service/target/account-service-1.0-SNAPSHOT.jar"
echo
echo -e "${BLUE}# Transfer Service${NC}"
echo "java -jar transfer-service/target/transfer-service-1.0-SNAPSHOT.jar"
echo
echo -e "${BLUE}# Validation Service${NC}"
echo "java -jar validation-service/target/validation-service-1.0-SNAPSHOT.jar"
echo
echo -e "${BLUE}# Notification Service${NC}"
echo "java -jar notification-service/target/notification-service-1.0-SNAPSHOT.jar"
echo
echo -e "${BLUE}# Audit Service${NC}"
echo "java -jar audit-service/target/audit-service-1.0-SNAPSHOT.jar"
echo

# 7. URLs úteis
echo "7. 🌐 URLs úteis:"
echo "   • Temporal UI: http://localhost:8088"
echo "   • Kafka UI: http://localhost:8090"
echo "   • Debezium API: http://localhost:8083"
echo

# 8. Scripts de teste
echo "8. 🧪 Scripts de teste:"
echo "   • Diagnóstico: ./scripts/debug/diagnose-system.sh"
echo "   • Teste completo: ./scripts/test/test-full-flow.sh"
echo

echo -e "${GREEN}🎉 Infraestrutura iniciada com sucesso!${NC}"
echo -e "${YELLOW}💡 Aguarde alguns minutos para todos os serviços estarem completamente prontos.${NC}"