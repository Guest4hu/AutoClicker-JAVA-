package com.example.autoclicker.service;

import com.example.autoclicker.dto.WindowInfoDTO;

import java.util.List;

public interface WindowService {
    List<WindowInfoDTO> getOpenWindows();
}
