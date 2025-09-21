#!/bin/bash

echo "Registering Debezium connector for PostgreSQL..."

curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{
    "name": "banking-connector",
    "config": {
        "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
        "database.hostname": "postgres",
        "database.port": "5432",
        "database.user": "postgres",
        "database.password": "postgres",
        "database.dbname": "banking_demo",
        "database.server.name": "banking",
        "topic.prefix": "banking",
        "table.include.list": "public.accounts,public.transfers",
        "plugin.name": "pgoutput",
        "publication.name": "dbz_publication",
        "slot.name": "dbz_slot",
        "key.converter": "org.apache.kafka.connect.json.JsonConverter",
        "value.converter": "org.apache.kafka.connect.json.JsonConverter",
        "key.converter.schemas.enable": "false",
        "value.converter.schemas.enable": "false"
    }
}'

echo "Done."