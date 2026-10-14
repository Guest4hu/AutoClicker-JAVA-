package com.example.autoclicker.config;

import com.example.autoclicker.service.LinuxX11WindowService;
import com.example.autoclicker.service.WindowService;
import com.example.autoclicker.service.WindowsWindowService;

import java.util.List;

public class OperationalSystemConfig {
    public static WindowService create() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsWindowService();
        }
        if (os.contains("linux")) {
            return new LinuxX11WindowService();
        }
        return List::of;
    };
}
