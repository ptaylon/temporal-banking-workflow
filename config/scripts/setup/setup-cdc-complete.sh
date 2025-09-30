#!/bin/bash

echo "=== Configuração completa do CDC ==="

# 1. Configurar publicação no PostgreSQL
echo "1. Configurando publicação PostgreSQL..."
docker exec banking-postgres psql -U postgres -d banking_demo -c "
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'dbz_publication') THEN
        CREATE PUBLICATION dbz_publication FOR TABLE public.accounts, public.transfers;
        RAISE NOTICE 'Publication dbz_publication created successfully';
    ELSE
        RAISE NOTICE 'Publication dbz_publication already exists';
    END IF;
END \$\$;
"

# 2. Aguardar Debezium Connect estar pronto
echo "2. Aguardando Debezium Connect..."
until curl -s http://localhost:8083/ > /dev/null; do
    echo "Aguardando Debezium Connect iniciar..."
    sleep 5
done

# 3. Remover conector existente se houver
echo "3. Removendo conector existente..."
curl -X DELETE http://localhost:8083/connectors/banking-connector 2>/dev/null || true

# 4. Registrar novo conector
echo "4. Registrando conector Debezium..."
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{
    "name": "banking-connector",
    "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "database.hostname": "postgres",
        "database.port": "5432",
        "database.user": "postgres",
        "database.password": "postgres",
        "database.dbname": "banking_demo",
        "topic.prefix": "banking",
        "table.include.list": "public.accounts,public.transfers",
        "plugin.name": "pgoutput",
        "publication.name": "dbz_publication",
        "slot.name": "dbz_slot",
        "key.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "key.converter.schemas.enable": "false",
        "value.converter.schemas.enable": "false",
        "snapshot.mode": "initial",
        "transforms": "unwrap",
        "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
        "transforms.unwrap.drop.tombstones": "false",
        "transforms.unwrap.delete.handling.mode": "rewrite"
    }
}'

echo
echo "5. Verificando status do conector..."
sleep 3
curl -s http://localhost:8083/connectors/banking-connector/status | jq .

echo
echo "=== Configuração concluída ==="