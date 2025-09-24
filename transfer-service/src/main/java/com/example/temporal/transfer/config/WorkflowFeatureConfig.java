package com.example.temporal.transfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração de feature flags para funcionalidades do workflow
 */
@Data
@Component
@ConfigurationProperties(prefix = "workflow.features")
public class WorkflowFeatureConfig {
    
    /**
     * Habilita funcionalidades de controle (pause/resume/cancel)
     */
    private boolean controlEnabled = true;
    
    /**
     * Habilita timers configuráveis
     */
    private boolean timersEnabled = true;
    
    /**
     * Habilita search attributes
     */
    private boolean searchAttributesEnabled = true;
    
    /**
     * Habilita child workflows
     */
    private boolean childWorkflowsEnabled = false;
    
    /**
     * Habilita transferências recorrentes
     */
    private boolean recurringTransfersEnabled = false;
    
    /**
     * Habilita workflow updates
     */
    private boolean workflowUpdatesEnabled = false;
}