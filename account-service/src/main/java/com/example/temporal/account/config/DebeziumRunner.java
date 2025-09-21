package com.example.temporal.account.config;

import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumRunner {

    private final DebeziumEngine<RecordChangeEvent<SourceRecord>> debeziumEngine;
    private ExecutorService executor;

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadExecutor();
        executor.execute(debeziumEngine);
        log.info("Debezium engine started");
    }

    @PreDestroy
    public void stop() throws IOException {
        if (debeziumEngine != null) {
            debeziumEngine.close();
            log.info("Debezium engine stopped");
        }
        if (executor != null) {
            executor.shutdown();
        }
    }
}
