package com.example.temporal.common.infrastructure.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

/**
 * Executor for idempotent operations using database unique constraint
 * Simple and efficient - no separate idempotency table needed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyExecutor {

    /**
     * Executes an operation with idempotency guarantee
     * Uses database unique constraint on idempotency_key
     * 
     * @param idempotencyKey Unique key for the operation
     * @param operationType Type of operation (DEBIT, CREDIT, TRANSFER, etc.)
     * @param entityId Entity ID involved (account number, transfer ID, etc.)
     * @param operation The actual operation to execute
     * @return Result of the operation
     * @throws IdempotentOperationException If operation was already processed
     */
    public <T> T executeWithResult(
            String idempotencyKey,
            String operationType,
            String entityId,
            Callable<T> operation) throws Exception {

        // Se não tem idempotency key, executa normalmente
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return operation.call();
        }

        log.debug("Executing idempotent operation: {} for entity: {} with key: {}", 
                operationType, entityId, idempotencyKey);

        try {
            // Tenta executar - banco garante unicidade
            T result = operation.call();
            log.info("Idempotent operation completed successfully: {} - {}", operationType, idempotencyKey);
            return result;

        } catch (DataIntegrityViolationException e) {
            // Unique constraint violada = já existe
            log.info("Duplicate {} operation detected, already processed: {}", operationType, idempotencyKey);
            throw new IdempotentOperationException("Operation already processed");

        } catch (Exception e) {
            log.error("Idempotent operation failed: {} - {}", operationType, idempotencyKey, e);
            throw e;
        }
    }

    /**
     * Executes an operation with idempotency guarantee
     * Simpler version without return value
     */
    public void execute(
            String idempotencyKey,
            String operationType,
            String entityId,
            Runnable operation) {

        try {
            executeWithResult(idempotencyKey, operationType, entityId, () -> {
                operation.run();
                return null;
            });
        } catch (Exception e) {
            if (e instanceof IdempotentOperationException) {
                throw (IdempotentOperationException) e;
            }
            throw new RuntimeException("Operation failed", e);
        }
    }

    /**
     * Exception thrown when duplicate idempotent operation is detected
     */
    public static class IdempotentOperationException extends RuntimeException {
        public IdempotentOperationException(String message) {
            super(message);
        }
    }
}
