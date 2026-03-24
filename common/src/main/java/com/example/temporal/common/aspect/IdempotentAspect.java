package com.example.temporal.common.aspect;

import com.example.temporal.common.annotation.Idempotent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for handling @Idempotent annotation
 * Automatically wraps annotated methods with idempotency logic
 */
@Slf4j
@Aspect
@Component
@Order(1) // High priority to run before @Transactional
public class IdempotentAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Intercepts methods annotated with @Idempotent
     */
    @Around("@annotation(com.example.temporal.common.annotation.Idempotent)")
    public Object aroundIdempotent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Idempotent idempotent = method.getAnnotation(Idempotent.class);

        // Extract idempotency key from SpEL expression
        String idempotencyKey = extractKey(idempotent.key(), joinPoint, method);
        
        // If no key, execute normally
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.debug("No idempotency key provided, executing normally");
            return joinPoint.proceed();
        }

        // Extract entity ID if specified
        String entityId = extractEntityId(idempotent.entityId(), joinPoint, method);

        log.debug("Executing idempotent operation: {} with key: {}", 
                idempotent.operationType(), idempotencyKey);

        try {
            // Execute with idempotency - joinPoint.proceed() pode lançar Throwable
            Object result = joinPoint.proceed();
            log.info("Idempotent operation completed successfully: {} - {}", 
                    idempotent.operationType(), idempotencyKey);
            return result;

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Unique constraint violada = já existe
            log.info("Duplicate {} operation detected, already processed: {}", 
                    idempotent.operationType(), idempotencyKey);
            throw new IdempotentOperationException(idempotent.duplicateMessage());
        }
    }

    /**
     * Extracts idempotency key using SpEL
     */
    private String extractKey(String keyExpression, ProceedingJoinPoint joinPoint, Method method) {
        return extractValue(keyExpression, joinPoint, method);
    }

    /**
     * Extracts entity ID using SpEL
     */
    private String extractEntityId(String entityIdExpression, ProceedingJoinPoint joinPoint, Method method) {
        if (entityIdExpression == null || entityIdExpression.isEmpty()) {
            return null;
        }
        return extractValue(entityIdExpression, joinPoint, method);
    }

    /**
     * Extracts value using SpEL expression
     */
    private String extractValue(String expression, ProceedingJoinPoint joinPoint, Method method) {
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            
            // Add method parameters to context
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // Add 'this' to context
            context.setRootObject(joinPoint.getTarget());

            // Evaluate expression
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            
            return value != null ? value.toString() : null;

        } catch (Exception e) {
            log.warn("Failed to extract value from expression: {}", expression, e);
            return null;
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
