package com.example.temporal.audit.cdc;

import com.example.temporal.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseChangeHandler implements DebeziumEngine.ChangeConsumer<RecordChangeEvent<SourceRecord>> {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public void handleBatch(List<RecordChangeEvent<SourceRecord>> records,
                          DebeziumEngine.RecordCommitter<RecordChangeEvent<SourceRecord>> committer) throws InterruptedException {
        for (RecordChangeEvent<SourceRecord> record : records) {
            SourceRecord sourceRecord = record.record();
            Struct sourceRecordValue = (Struct) sourceRecord.value();

            if (sourceRecordValue != null) {
                String operation = sourceRecordValue.getString("op");
                String table = sourceRecord.topic().split("\\.")[2];
                Struct after = sourceRecordValue.getStruct("after");
                Struct before = sourceRecordValue.getStruct("before");

                Map<String, Object> afterState = after != null ? 
                    convertStructToMap(after) : null;
                Map<String, Object> beforeState = before != null ? 
                    convertStructToMap(before) : null;

                String entityId = extractEntityId(afterState, beforeState);
                String eventType = determineEventType(operation, table);
                
                auditService.recordEvent(
                    eventType,
                    table,
                    entityId,
                    beforeState,
                    afterState,
                    "SYSTEM" // In a real system, this would come from a security context
                );
            }
            committer.markProcessed(record);
        }
        committer.markBatchFinished();
    }

    private String extractEntityId(Map<String, Object> afterState, Map<String, Object> beforeState) {
        if (afterState != null && afterState.containsKey("id")) {
            return afterState.get("id").toString();
        } else if (beforeState != null && beforeState.containsKey("id")) {
            return beforeState.get("id").toString();
        }
        return "unknown";
    }

    private String determineEventType(String operation, String table) {
        switch (operation) {
            case "c":
                return table.toUpperCase() + "_CREATED";
            case "u":
                return table.toUpperCase() + "_UPDATED";
            case "d":
                return table.toUpperCase() + "_DELETED";
            default:
                return "UNKNOWN_OPERATION";
        }
    }

    private Map<String, Object> convertStructToMap(Struct struct) {
        Map<String, Object> map = new java.util.HashMap<>();
        struct.schema().fields().forEach(field -> {
            Object value = struct.get(field);
            map.put(field.name(), value);
        });
        return map;
    }
}