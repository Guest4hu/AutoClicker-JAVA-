package com.example.autoclicker;

import com.example.autoclicker.domain.model.ActionType;
import com.example.autoclicker.domain.model.MacroActionSetting;
import com.example.autoclicker.storage.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MacroActionSettingTest {

    private final ObjectMapper mapper = JacksonConfig.getObjectMapper();

    @Test
    void testConstructorsAndGettersSetters() {
        MacroActionSetting action = new MacroActionSetting(ActionType.MOUSE, 1, 100, 200, 300);
        action.setKeyReleased(150);
        action.setXAfter(350);
        action.setYAfter(250);

        assertEquals(ActionType.MOUSE, action.getActionType());
        assertEquals(1, action.getKey());
        assertEquals(100, action.getKeyPressed());
        assertEquals(150, action.getKeyReleased());
        assertEquals(300, action.getXBefore());
        assertEquals(200, action.getYBefore());
        assertEquals(350, action.getXAfter());
        assertEquals(250, action.getYAfter());
    }

    @Test
    void testToStringFormatting() {
        MacroActionSetting keyAction = new MacroActionSetting(ActionType.TECLADO, NativeKeyEvent.VC_ENTER, 1000);
        keyAction.setKeyReleased(1100);
        String keyStr = keyAction.toString();
        assertTrue(keyStr.contains("TECLADO"));
        assertTrue(keyStr.contains("100 ms"));

        MacroActionSetting mouseClick = new MacroActionSetting(ActionType.MOUSE, 1, 2000, 50, 50);
        mouseClick.setKeyReleased(2050);
        String mouseStr = mouseClick.toString();
        assertTrue(mouseStr.contains("MOUSE"));
        assertTrue(mouseStr.contains("Esquerdo"));

        MacroActionSetting dragAction = new MacroActionSetting(ActionType.MOUSE_ARRASTADO, 1, 3000, 50, 50);
        dragAction.setXAfter(100);
        dragAction.setYAfter(150);
        dragAction.setKeyReleased(3200);
        String dragStr = dragAction.toString();
        assertTrue(dragStr.contains("ARRASTAR"));
        assertTrue(dragStr.contains("(  50,   50) até ( 100,  150)"));
    }

    @Test
    void testJacksonSerializationAndBackwardCompatibility() throws Exception {
        // Testa serialização canônica
        MacroActionSetting action = new MacroActionSetting(ActionType.MOUSE, 1, 500, 10, 20);
        action.setXAfter(30);
        action.setYAfter(40);
        action.setKeyReleased(550);

        String json = mapper.writeValueAsString(action);
        assertTrue(json.contains("\"xBefore\" : 20"));
        assertTrue(json.contains("\"yBefore\" : 10"));
        assertTrue(json.contains("\"xAfter\" : 30"));
        assertTrue(json.contains("\"yAfter\" : 40"));

        // Testa desserialização de JSON legado com minúsculas duplicadas
        String legacyJson = """
        {
            "actionType": "MOUSE",
            "key": 1,
            "keyPressed": 1000,
            "keyReleased": 1200,
            "yBefore": 100,
            "xBefore": 200,
            "yAfter": 150,
            "xAfter": 250,
            "ybefore": 100,
            "xbefore": 200,
            "yafter": 150,
            "xafter": 250,
            "unknownFutureField": "ignored"
        }
        """;

        MacroActionSetting deserialized = mapper.readValue(legacyJson, MacroActionSetting.class);
        assertNotNull(deserialized);
        assertEquals(200, deserialized.getXBefore());
        assertEquals(100, deserialized.getYBefore());
        assertEquals(250, deserialized.getXAfter());
        assertEquals(150, deserialized.getYAfter());
    }
}
