package com.example.temporal.common.dto;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for REST API.
 * Provides consistent error structure across all endpoints.
 */
@Data
@Builder
@Accessors(chain = true)
public class ErrorResponse {

    /**
     * Error message describing what went wrong
     */
    private String message;

    /**
     * Error code for programmatic handling
     */
    private String code;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;

    /**
     * Additional error details (optional)
     */
    private Object details;

    /**
     * Creates a simple error response with just a message
     *
     * @param message the error message
     * @return error response
     */
    public static ErrorResponse simple(final String message) {
        return ErrorResponse.builder()
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message and code
     *
     * @param message the error message
     * @param code the error code
     * @return error response
     */
    public static ErrorResponse withCode(final String message, final String code) {
        return ErrorResponse.builder()
                .message(message)
                .code(code)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Creates an error response with message and additional details
     *
     * @param message the error message
     * @param details additional details
     * @return error response
     */
    public static ErrorResponse withDetails(final String message, final Object details) {
        return ErrorResponse.builder()
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
