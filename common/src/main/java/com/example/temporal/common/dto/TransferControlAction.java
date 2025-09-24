package com.example.temporal.common.dto;

/**
 * Enum para ações de controle de transferência
 */
public enum TransferControlAction {
    PAUSE("Pausar transferência"),
    RESUME("Retomar transferência"),
    CANCEL("Cancelar transferência");
    
    private final String description;
    
    TransferControlAction(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}