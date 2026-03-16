package com.example.temporal.transfer.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing workflow feature flags
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {
    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final WorkflowFeatureConfig featureConfig;

    /**
     * Checks if control features are enabled
     */
    public boolean isControlEnabled() {
        return featureConfig.isControlEnabled();
    }

    /**
     * Checks if timers are enabled
     */
    public boolean isTimersEnabled() {
        return featureConfig.isTimersEnabled();
    }

    /**
     * Checks if search attributes are enabled
     */
    public boolean isSearchAttributesEnabled() {
        return featureConfig.isSearchAttributesEnabled();
    }

    /**
     * Checks if child workflows are enabled
     */
    public boolean isChildWorkflowsEnabled() {
        return featureConfig.isChildWorkflowsEnabled();
    }

    /**
     * Checks if recurring transfers are enabled
     */
    public boolean isRecurringTransfersEnabled() {
        return featureConfig.isRecurringTransfersEnabled();
    }

    /**
     * Checks if workflow updates are enabled
     */
    public boolean isWorkflowUpdatesEnabled() {
        return featureConfig.isWorkflowUpdatesEnabled();
    }

    /**
     * Enables or disables control features at runtime
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
     * Enables or disables timers at runtime
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
     * Returns the status of all features
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
     * Disables all experimental features
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
     * Enables only basic features
     */
    public void enableOnlyBasicFeatures() {
        try {
            log.info("Enabling only basic features");

            boolean controlWas = featureConfig.isControlEnabled();
            boolean timersWas = featureConfig.isTimersEnabled();
            boolean searchWas = featureConfig.isSearchAttributesEnabled();
            boolean childWas = featureConfig.isChildWorkflowsEnabled();
            boolean recurringWas = featureConfig.isRecurringTransfersEnabled();
            boolean updatesWas = featureConfig.isWorkflowUpdatesEnabled();

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
     * Validates if a feature configuration is valid
     */
    public boolean isValidConfiguration() {
        try {
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
     * Returns a summary of enabled features
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
     * Resets all features to default values
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
