package com.example.autoclicker;

import com.example.autoclicker.input.window.LinuxX11WindowService;
import com.example.autoclicker.input.window.WindowService;
import com.example.autoclicker.input.window.WindowServiceFactory;
import com.example.autoclicker.input.window.WindowsWindowService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowServiceFactoryTest {

    @Test
    void testFactoryCreatesAppropriateService() {
        WindowService service = WindowServiceFactory.create();
        assertNotNull(service, "Factory não deve retornar serviço nulo");

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            assertTrue(service instanceof LinuxX11WindowService);
        } else if (os.contains("win")) {
            assertTrue(service instanceof WindowsWindowService);
        }
    }

    @Test
    void testOpenWindowsCallDoesNotThrow() {
        WindowService service = WindowServiceFactory.create();
        assertDoesNotThrow(service::getOpenWindows);
    }
}
