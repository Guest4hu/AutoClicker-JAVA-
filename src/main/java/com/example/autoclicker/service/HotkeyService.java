package com.example.autoclicker.service;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class HotkeyService {
    private final LinkedHashSet<Integer> hotKeyConfig = new LinkedHashSet<>();
    private final Set<Integer> currentlyPressedKeys = new HashSet<>();
    private volatile boolean capturing = false;
    private boolean started = false;
    private boolean stopKeyStarted = false;
    private boolean alreadyTriggered = false;

    public HotkeyService() {
        // Define F6 como atalho padrão
        hotKeyConfig.add(NativeKeyEvent.VC_F6);
    }

    public void resetDefaultHotkey() {
        hotKeyConfig.clear();
        hotKeyConfig.add(NativeKeyEvent.VC_F6);
    }

    public boolean isCapturing() {
        return capturing;
    }

    public void start(Runnable onHotkeyTriggered) {
        if (started) {
            return;
        }
        started = true;

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (capturing) {
                    return;
                }

                currentlyPressedKeys.add(e.getKeyCode());

                if (!hotKeyConfig.isEmpty() && currentlyPressedKeys.containsAll(hotKeyConfig)) {
                    if (!alreadyTriggered) {
                        alreadyTriggered = true;
                        if (onHotkeyTriggered != null) {
                            onHotkeyTriggered.run();
                        }
                    }
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                currentlyPressedKeys.remove(e.getKeyCode());
                if (!currentlyPressedKeys.containsAll(hotKeyConfig)) {
                    alreadyTriggered = false;
                }
            }
        });
    }

    public Set<Integer> getHotKeyConfig() {
        return hotKeyConfig;
    }

    /**
     * Registra um ouvinte global para a tecla ESC, que funciona mesmo com a aplicação
     * minimizada ou em segundo plano. Só é registrado uma vez.
     *
     * @param onStopKeyPressed ação executada (na thread do JNativeHook) ao pressionar ESC
     */
    public void registerStopKey(Runnable onStopKeyPressed) {
        if (stopKeyStarted) {
            return;
        }
        stopKeyStarted = true;

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE && onStopKeyPressed != null) {
                    onStopKeyPressed.run();
                }
            }
        });
    }

    public String getHotkeyText() {
        if (hotKeyConfig.isEmpty()) {
            return "Nenhum atalho configurado";
        }
        return hotKeyConfig.stream()
                .map(NativeKeyEvent::getKeyText)
                .collect(Collectors.joining(" + "));
    }

    public void captureHotkey(Runnable onFinished) {
        capturing = true;
        Set<Integer> comboTemporario = new LinkedHashSet<>();

        NativeKeyListener listener = new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                // Se a tecla for ESC (NativeKeyEvent.VC_ESCAPE), nós cancelamos!
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    GlobalScreen.removeNativeKeyListener(this);
                    comboTemporario.clear();
                    capturing = false;
                    if (onFinished != null) onFinished.run();
                    return;
                }

                // Se for outra tecla, adicionamos ao combo temporário:
                comboTemporario.add(e.getKeyCode());
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                // Quando soltar e tivermos teclas acumuladas:
                if (!comboTemporario.isEmpty()) {
                    hotKeyConfig.clear();
                    hotKeyConfig.addAll(comboTemporario);

                    // 1. Remove o listener para não ouvir mais nada
                    GlobalScreen.removeNativeKeyListener(this);
                    capturing = false;

                    // 2. Avisa a interface que acabou
                    if (onFinished != null) onFinished.run();
                }
            }
        };

        // Ativa o ouvinte temporário no sistema
        GlobalScreen.addNativeKeyListener(listener);
    }
}
