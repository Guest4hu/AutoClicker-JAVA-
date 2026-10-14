package com.example.autoclicker.service;

import com.example.autoclicker.DTO.WindowInfoDTO;

import java.util.List;

public class WindowsWindowService implements WindowService {
    @Override
    public List<WindowInfoDTO> getOpenWindows() {
        // Reservado para implementação futura com User32 (EnumWindows)
        return List.of();
    }
}
