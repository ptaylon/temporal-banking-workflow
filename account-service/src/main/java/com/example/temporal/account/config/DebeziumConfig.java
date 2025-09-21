package com.example.temporal.account.config;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class DebeziumConfig {

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    // Debezium 2.5+ requires 'topic.prefix' (replaces the old 'database.server.name')
    // Provide a sensible default and allow overriding via application.yml
    @Value("${debezium.topic-prefix:account-service}")
    private String topicPrefix;

    @Bean
    public DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine() {
        final Properties props = new Properties();
        props.setProperty("name", "account-service-postgres-connector");
        props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
        props.setProperty("offset.storage.file.filename", "./offsets.dat");
        props.setProperty("offset.flush.interval.ms", "60000");

        // Required topic prefix for naming emitted topics
        props.setProperty("topic.prefix", topicPrefix);
        
        // PostgreSQL connection details
        props.setProperty("database.hostname", extractHostname(dbUrl));
        props.setProperty("database.port", extractPort(dbUrl));
        props.setProperty("database.user", dbUsername);
        props.setProperty("database.password", dbPassword);
        props.setProperty("database.dbname", extractDatabaseName(dbUrl));
        
        // PostgreSQL specific properties
        props.setProperty("plugin.name", "pgoutput");
        props.setProperty("table.include.list", "public.accounts");
        props.setProperty("publication.name", "dbz_publication");

        return DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(props)
                .notifying(this::handleChangeEvent)
                .build();
    }

    private void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent) {
        SourceRecord sourceRecord = sourceRecordRecordChangeEvent.record();
        log.info("Key = {}, Value = {}", sourceRecord.key(), sourceRecord.value());
        // Here you would typically send this to Kafka or process it directly
    }

    private String extractHostname(String jdbcUrl) {
        // Example URL: jdbc:postgresql://localhost:5432/dbname
        String[] parts = jdbcUrl.split("//")[1].split(":");
        return parts[0];
    }

    private String extractPort(String jdbcUrl) {
        String[] parts = jdbcUrl.split("//")[1].split(":");
        return parts[1].split("/")[0];
    }

    private String extractDatabaseName(String jdbcUrl) {
        String[] parts = jdbcUrl.split("/");
        return parts[parts.length - 1];
    }
}