package com.example.autoclicker.input.window;

import java.util.List;

/**
 * Factory Method para instanciação da implementação apropriada de {@link WindowService}
 * com base no sistema operacional identificado em tempo de execução.
 */
public class WindowServiceFactory {

    private WindowServiceFactory() {}

    public static WindowService create() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsWindowService();
        }
        if (os.contains("linux")) {
            return new LinuxX11WindowService();
        }
        return List::of;
    }
}
