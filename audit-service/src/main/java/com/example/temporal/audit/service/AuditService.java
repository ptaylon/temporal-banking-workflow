package com.example.temporal.audit.service;

import com.example.temporal.audit.model.AuditEvent;
import com.example.temporal.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordEvent(String eventType, String entityType, String entityId,
                          Map<String, Object> oldState, Map<String, Object> newState,
                          String userId) {
        try {
            AuditEvent event = new AuditEvent()
                    .setEventType(eventType)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setOldState(oldState != null ? objectMapper.writeValueAsString(oldState) : null)
                    .setNewState(newState != null ? objectMapper.writeValueAsString(newState) : null)
                    .setUserId(userId);

            auditEventRepository.save(event);
            log.info("Recorded audit event: {} for entity {}/{}", eventType, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to record audit event", e);
            throw new RuntimeException("Failed to record audit event", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsForEntity(String entityType, String entityId) {
        return auditEventRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getEventsByTypeInRange(String entityType, 
            List<String> eventTypes, LocalDateTime start, LocalDateTime end) {
        return auditEventRepository.findByEntityTypeAndEventTypesInRange(
                entityType, eventTypes, start, end);
    }
}