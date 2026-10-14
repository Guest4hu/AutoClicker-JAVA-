package com.example.autoclicker;

import com.example.autoclicker.input.hotkey.HotkeyService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HotkeyServiceTest {

    @Test
    void testDefaultHotkeyIsF6() {
        HotkeyService service = new HotkeyService();
        assertTrue(service.getHotKeyConfig().contains(NativeKeyEvent.VC_F6));
        assertTrue(service.getHotkeyText().contains("F6"));
    }

    @Test
    void testResetDefaultHotkey() {
        HotkeyService service = new HotkeyService();
        service.getHotKeyConfig().clear();
        service.getHotKeyConfig().add(NativeKeyEvent.VC_F10);
        assertTrue(service.getHotkeyText().contains("F10"));

        service.resetDefaultHotkey();
        assertTrue(service.getHotKeyConfig().contains(NativeKeyEvent.VC_F6));
    }

    @Test
    void testFallbackKeyTextFormatting() {
        assertEquals("F1", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_F1));
        assertEquals("F12", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_F12));
        assertEquals("Esc", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_ESCAPE));
        assertEquals("Enter", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_ENTER));
        assertEquals("Space", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_SPACE));
        assertEquals("Tab", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_TAB));
        assertEquals("Ctrl", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_CONTROL));
        assertEquals("Alt", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_ALT));
        assertEquals("Shift", HotkeyService.getFallbackKeyText(NativeKeyEvent.VC_SHIFT));
    }
}
