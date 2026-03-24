package com.example.temporal.common.dto;

import lombok.Getter;

/**
 * Enum para ações de controle de transferência
 */
@Getter
public enum TransferControlAction {
    PAUSE("Pausar transferência"),
    RESUME("Retomar transferência"),
    CANCEL("Cancelar transferência");
    
    private final String description;
    
    TransferControlAction(String description) {
        this.description = description;
    }

}