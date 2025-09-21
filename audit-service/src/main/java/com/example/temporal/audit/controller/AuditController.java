package com.example.temporal.audit.controller;

import com.example.temporal.audit.model.AuditEvent;
import com.example.temporal.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditEvent>> getEntityAuditHistory(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        return ResponseEntity.ok(auditService.getEventsForEntity(entityType, entityId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AuditEvent>> searchAuditEvents(
            @RequestParam String entityType,
            @RequestParam List<String> eventTypes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditService.getEventsByTypeInRange(entityType, eventTypes, start, end));
    }
}