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

import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
public class DebeziumConfig {

    private final Map<String, String> debeziumProps;

    public DebeziumConfig(@Value("#{${debezium}}") Map<String, String> debeziumProps) {
        this.debeziumProps = debeziumProps;
    }

    @Bean
    public DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine() {
        Properties props = new Properties();
        props.setProperty("name", debeziumProps.get("name"));
        props.setProperty("connector.class", debeziumProps.get("connector-class"));
        props.setProperty("offset.storage", debeziumProps.get("offset-storage"));
        props.setProperty("offset.storage.file.filename", debeziumProps.get("offset-storage-file"));
        props.setProperty("offset.flush.interval.ms", debeziumProps.get("offset-flush-interval-ms"));
        props.setProperty("topic.prefix", debeziumProps.get("topic-prefix"));
        props.setProperty("database.hostname", debeziumProps.get("database.hostname"));
        props.setProperty("database.port", debeziumProps.get("database.port"));
        props.setProperty("database.user", debeziumProps.get("database.username"));
        props.setProperty("database.password", debeziumProps.get("database.password"));
        props.setProperty("database.dbname", debeziumProps.get("database.dbname"));
        props.setProperty("plugin.name", debeziumProps.get("plugin-name"));
        props.setProperty("table.include.list", debeziumProps.get("table-include-list"));
        props.setProperty("publication.name", debeziumProps.get("publication-name"));

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
