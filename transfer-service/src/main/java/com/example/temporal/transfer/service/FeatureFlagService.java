package com.example.temporal.transfer.service;

import com.example.temporal.transfer.config.WorkflowFeatureConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Serviço para gerenciar feature flags das funcionalidades do workflow
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {
    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);
    
    private final WorkflowFeatureConfig featureConfig;
    
    /**
     * Verifica se as funcionalidades de controle estão habilitadas
     */
    public boolean isControlEnabled() {
        return featureConfig.isControlEnabled();
    }
    
    /**
     * Verifica se os timers configuráveis estão habilitados
     */
    public boolean isTimersEnabled() {
        return featureConfig.isTimersEnabled();
    }
    
    /**
     * Verifica se as search attributes estão habilitadas
     */
    public boolean isSearchAttributesEnabled() {
        return featureConfig.isSearchAttributesEnabled();
    }
    
    /**
     * Verifica se os child workflows estão habilitados
     */
    public boolean isChildWorkflowsEnabled() {
        return featureConfig.isChildWorkflowsEnabled();
    }
    
    /**
     * Verifica se as transferências recorrentes estão habilitadas
     */
    public boolean isRecurringTransfersEnabled() {
        return featureConfig.isRecurringTransfersEnabled();
    }
    
    /**
     * Verifica se os workflow updates estão habilitados
     */
    public boolean isWorkflowUpdatesEnabled() {
        return featureConfig.isWorkflowUpdatesEnabled();
    }
    
    /**
     * Habilita ou desabilita funcionalidades de controle em runtime
     */
    public void setControlEnabled(boolean enabled) {
        try {
            log.info("Changing control functionality status from {} to {}", 
                featureConfig.isControlEnabled(), enabled);
            featureConfig.setControlEnabled(enabled);
            log.info("Control functionality status changed successfully to: {}", enabled);
        } catch (Exception e) {
            log.error("Failed to change control functionality status to {}: {}", enabled, e.getMessage());
            throw new RuntimeException("Failed to update control feature flag", e);
        }
    }
    
    /**
     * Habilita ou desabilita timers em runtime
     */
    public void setTimersEnabled(boolean enabled) {
        try {
            log.info("Changing timers functionality status from {} to {}", 
                featureConfig.isTimersEnabled(), enabled);
            featureConfig.setTimersEnabled(enabled);
            log.info("Timers functionality status changed successfully to: {}", enabled);
        } catch (Exception e) {
            log.error("Failed to change timers functionality status to {}: {}", enabled, e.getMessage());
            throw new RuntimeException("Failed to update timers feature flag", e);
        }
    }
    
    /**
     * Retorna o status de todas as features
     */
    public WorkflowFeatureConfig getAllFeatureStatus() {
        try {
            return featureConfig;
        } catch (Exception e) {
            log.error("Failed to retrieve feature status: {}", e.getMessage());
            throw new RuntimeException("Failed to get feature configuration", e);
        }
    }
    
    /**
     * Desabilita todas as funcionalidades experimentais
     */
    public void disableExperimentalFeatures() {
        try {
            log.warn("Disabling all experimental features");
            boolean recurringWasEnabled = featureConfig.isRecurringTransfersEnabled();
            boolean updatesWasEnabled = featureConfig.isWorkflowUpdatesEnabled();
            
            featureConfig.setRecurringTransfersEnabled(false);
            featureConfig.setWorkflowUpdatesEnabled(false);
            
            log.info("Experimental features disabled successfully. Recurring: {} -> false, Updates: {} -> false", 
                recurringWasEnabled, updatesWasEnabled);
        } catch (Exception e) {
            log.error("Failed to disable experimental features: {}", e.getMessage());
            throw new RuntimeException("Failed to disable experimental features", e);
        }
    }
    
    /**
     * Habilita apenas funcionalidades básicas
     */
    public void enableOnlyBasicFeatures() {
        try {
            log.info("Enabling only basic features");
            
            // Capturar estado atual para auditoria
            boolean controlWas = featureConfig.isControlEnabled();
            boolean timersWas = featureConfig.isTimersEnabled();
            boolean searchWas = featureConfig.isSearchAttributesEnabled();
            boolean childWas = featureConfig.isChildWorkflowsEnabled();
            boolean recurringWas = featureConfig.isRecurringTransfersEnabled();
            boolean updatesWas = featureConfig.isWorkflowUpdatesEnabled();
            
            // Configurar apenas funcionalidades básicas
            featureConfig.setControlEnabled(true);
            featureConfig.setTimersEnabled(false);
            featureConfig.setSearchAttributesEnabled(true);
            featureConfig.setChildWorkflowsEnabled(false);
            featureConfig.setRecurringTransfersEnabled(false);
            featureConfig.setWorkflowUpdatesEnabled(false);
            
            log.info("Basic features configuration applied successfully. Changes: " +
                "Control: {} -> true, Timers: {} -> false, Search: {} -> true, " +
                "Child: {} -> false, Recurring: {} -> false, Updates: {} -> false",
                controlWas, timersWas, searchWas, childWas, recurringWas, updatesWas);
                
        } catch (Exception e) {
            log.error("Failed to enable only basic features: {}", e.getMessage());
            throw new RuntimeException("Failed to configure basic features", e);
        }
    }
    
    /**
     * Valida se uma configuração de feature é válida
     */
    public boolean isValidConfiguration() {
        try {
            // Verificar se pelo menos uma funcionalidade básica está habilitada
            boolean hasBasicFeatures = featureConfig.isControlEnabled() || 
                                     featureConfig.isSearchAttributesEnabled();
            
            if (!hasBasicFeatures) {
                log.warn("Invalid configuration: No basic features are enabled");
                return false;
            }
            
            log.debug("Feature configuration is valid");
            return true;
            
        } catch (Exception e) {
            log.error("Error validating feature configuration: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Retorna um resumo das features habilitadas
     */
    public String getFeatureSummary() {
        try {
            StringBuilder summary = new StringBuilder("Feature Status: ");
            summary.append("Control=").append(featureConfig.isControlEnabled())
                   .append(", Timers=").append(featureConfig.isTimersEnabled())
                   .append(", Search=").append(featureConfig.isSearchAttributesEnabled())
                   .append(", Child=").append(featureConfig.isChildWorkflowsEnabled())
                   .append(", Recurring=").append(featureConfig.isRecurringTransfersEnabled())
                   .append(", Updates=").append(featureConfig.isWorkflowUpdatesEnabled());
            
            return summary.toString();
            
        } catch (Exception e) {
            log.error("Failed to generate feature summary: {}", e.getMessage());
            return "Error generating feature summary";
        }
    }
    
    /**
     * Redefine todas as features para os valores padrão
     */
    public void resetToDefaults() {
        try {
            log.info("Resetting all features to default values");
            String beforeSummary = getFeatureSummary();
            
            featureConfig.setControlEnabled(true);
            featureConfig.setTimersEnabled(true);
            featureConfig.setSearchAttributesEnabled(true);
            featureConfig.setChildWorkflowsEnabled(false);
            featureConfig.setRecurringTransfersEnabled(false);
            featureConfig.setWorkflowUpdatesEnabled(false);
            
            String afterSummary = getFeatureSummary();
            log.info("Features reset to defaults. Before: [{}], After: [{}]", beforeSummary, afterSummary);
            
        } catch (Exception e) {
            log.error("Failed to reset features to defaults: {}", e.getMessage());
            throw new RuntimeException("Failed to reset feature configuration", e);
        }
    }
}