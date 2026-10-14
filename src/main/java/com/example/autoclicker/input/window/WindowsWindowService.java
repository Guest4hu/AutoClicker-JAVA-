package com.example.autoclicker.input.window;

import com.example.autoclicker.dto.WindowInfoDTO;
import java.util.List;

/**
 * Implementação para ecossistema Microsoft Windows.
 * Reservada para integração futura via User32 (EnumWindows / FFM).
 */
public class WindowsWindowService implements WindowService {

    @Override
    public List<WindowInfoDTO> getOpenWindows() {
        return List.of();
    }
}
