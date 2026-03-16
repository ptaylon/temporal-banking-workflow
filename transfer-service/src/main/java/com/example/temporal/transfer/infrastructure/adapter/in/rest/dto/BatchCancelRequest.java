package com.example.temporal.transfer.infrastructure.adapter.in.rest.dto;

import lombok.Data;

import java.util.List;

/**
 * Request DTO for batch cancel operations.
 */
@Data
public class BatchCancelRequest {
    private List<String> workflowIds;
    private String reason;
}
