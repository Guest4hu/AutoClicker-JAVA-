package com.example.autoclicker.dto;

import com.example.autoclicker.domain.model.MacroActionSetting;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO imutável para transporte e persistência de dados de uma macro gravada.
 */
public record MacroSettingDTO(
        String name,
        String description,
        LocalDateTime createdAt,
        List<MacroActionSetting> configs
) {
    public MacroSettingDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nome não pode ficar em branco");
        }
        if (configs == null || configs.isEmpty()) {
            throw new IllegalArgumentException("Está faltando uma configuração");
        }
    }
}
