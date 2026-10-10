package com.example.autoclicker;

import com.example.autoclicker.domain.AutoClickerSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutoClickerSettingTest {

    @Test
    void testValidInfiniteSetting() {
        AutoClickerSetting setting = AutoClickerSetting.infiniteClicks(
                50,
                AutoClickerSetting.ClickType.SIMPLES,
                AutoClickerSetting.MouseButton.ESQUERDO,
                0
        );

        List<String> errors = setting.validateSetting();
        assertTrue(errors.isEmpty(), "Configuração infinita válida não deve ter erros");
        assertTrue(setting.isInfiniteClick());
        assertEquals(50, setting.getInterval());
        assertEquals("Clique Simples", setting.getClickType().toString());
        assertEquals("Botão Esquerdo (Primário)", setting.getMouseButton().toString());
    }

    @Test
    void testValidLimitedSetting() {
        AutoClickerSetting setting = AutoClickerSetting.limitedClicks(
                100,
                AutoClickerSetting.ClickType.DUPLO,
                AutoClickerSetting.MouseButton.DIREITO,
                500,
                200
        );

        List<String> errors = setting.validateSetting();
        assertTrue(errors.isEmpty(), "Configuração limitada válida não deve ter erros");
        assertFalse(setting.isInfiniteClick());
        assertEquals(200, setting.getClickCount());
        assertEquals(500, setting.getDelay());
    }

    @Test
    void testInvalidSettings() {
        AutoClickerSetting invalid = AutoClickerSetting.limitedClicks(
                0,
                AutoClickerSetting.ClickType.SIMPLES,
                AutoClickerSetting.MouseButton.ESQUERDO,
                -10,
                0
        );

        List<String> errors = invalid.validateSetting();
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Intervalo")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Atraso")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Quantidade")));
    }
}
