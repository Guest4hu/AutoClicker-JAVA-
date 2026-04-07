package com.example.autoclicker.service;

import javafx.scene.input.KeyCode;
import java.util.LinkedHashSet;
import java.util.Set;

public class GlobalHotkeyListener {
    private Runnable callback;
    private volatile boolean running = false;
    private Set<KeyCode> hotkeyCombo;
    private final Set<KeyCode> currentPressedKeys = new LinkedHashSet<>();

    public GlobalHotkeyListener() {
    }

    public void start(Set<KeyCode> hotkeyCombo, Runnable onHotkeyPressed) {
        this.hotkeyCombo = hotkeyCombo;
        this.callback = onHotkeyPressed;
        this.running = true;
    }

    public void stop() {
        this.running = false;
        this.currentPressedKeys.clear();
    }

    public boolean isRunning() {
        return running;
    }

    public void handleKeyPressed(KeyCode keyCode) {
        if (!running || hotkeyCombo.isEmpty()) return;

        currentPressedKeys.add(keyCode);

        if (currentPressedKeys.containsAll(hotkeyCombo) && callback != null) {
            callback.run();
        }
    }

    public void handleKeyReleased(KeyCode keyCode) {
        currentPressedKeys.remove(keyCode);
    }
}

