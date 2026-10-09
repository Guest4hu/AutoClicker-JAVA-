package com.example.autoclicker.config;

import com.example.autoclicker.service.GlobalHotkeyListener;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JNativeHookConfig implements NativeKeyListener {
    private final GlobalHotkeyListener globalHotkeyListener;

    // Injentando a dependencia para o NativeHook
    public JNativeHookConfig(GlobalHotkeyListener globalHotkeyListener) {
        this.globalHotkeyListener = globalHotkeyListener;
    }

    public void startNativeHook( ){
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("Houve um problema ao registrar o hook nativo.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        // 3. Adiciona a sua classe como ouvinte global
        GlobalScreen.addNativeKeyListener(globalHotkeyListener);

    }
}
