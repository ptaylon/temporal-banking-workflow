package com.example.temporal.account.config;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Getter
@Configuration
@ConfigurationProperties(prefix = "debezium")
@ConditionalOnProperty(name = "debezium.enabled", havingValue = "true", matchIfMissing = false)
public class DebeziumConfig {

    private Map<String, String> database;
    private String name;
    private String connectorClass;
    private String offsetStorage;
    private String offsetStorageFile;
    private int offsetFlushIntervalMs;
    private String pluginName;
    private String topicPrefix;
    private String tableIncludeList;
    private String publicationName;

    @Bean
    public DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine() {
        Properties props = new Properties();
        props.setProperty("name", name);
        props.setProperty("connector.class", connectorClass);
        props.setProperty("offset.storage", offsetStorage);
        props.setProperty("offset.storage.file.filename", offsetStorageFile);
        props.setProperty("offset.flush.interval.ms", String.valueOf(offsetFlushIntervalMs));
        props.setProperty("topic.prefix", topicPrefix);
        props.setProperty("database.hostname", database.get("hostname"));
        props.setProperty("database.port", database.get("port"));
        props.setProperty("database.user", database.get("username"));
        props.setProperty("database.password", database.get("password"));
        props.setProperty("database.dbname", database.get("dbname"));
        props.setProperty("plugin.name", pluginName);
        props.setProperty("table.include.list", tableIncludeList);
        props.setProperty("publication.name", publicationName);

        return DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(props)
                .notifying(this::handleChangeEvent)
                .build();
    }

    private void handleChangeEvent(RecordChangeEvent<SourceRecord> sourceRecordRecordChangeEvent) {
        SourceRecord sourceRecord = sourceRecordRecordChangeEvent.record();
        log.info("Key = {}, Value = {}", sourceRecord.key(), sourceRecord.value());
    }

}
