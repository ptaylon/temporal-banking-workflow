package com.example.temporal.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for idempotent operations
 * Automatically handles idempotency using database unique constraint
 * 
 * Usage:
 * @Idempotent(key = "#idempotencyKey", operationType = "DEBIT")
 * public void debit(String accountNumber, BigDecimal amount, String idempotencyKey) {
 *     // Business logic only - idempotency handled automatically
 * }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * SpEL expression to extract idempotency key from method parameters
     * Example: "#idempotencyKey", "#request.idempotencyKey", "#accountNumber + '-debit'"
     */
    String key();

    /**
     * Type of operation (DEBIT, CREDIT, TRANSFER, etc.)
     * Used for logging and debugging
     */
    String operationType() default "OPERATION";

    /**
     * Entity ID involved in the operation
     * SpEL expression, e.g., "#accountNumber", "#transferId"
     */
    String entityId() default "";

    /**
     * Exception message when duplicate operation is detected
     */
    String duplicateMessage() default "Operation already processed";
}
