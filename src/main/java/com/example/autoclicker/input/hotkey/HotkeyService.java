package com.example.autoclicker.input.hotkey;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço de gerenciamento e escuta de teclas de atalho globais do sistema operacional via JNativeHook.
 * Garante concorrência thread-safe para detecção simultânea de teclas e formatação textual resiliente.
 */
public class HotkeyService {

    private final Set<Integer> hotKeyConfig = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<Integer> currentlyPressedKeys = ConcurrentHashMap.newKeySet();
    private volatile boolean capturing = false;
    private boolean started = false;
    private boolean stopKeyStarted = false;
    private boolean alreadyTriggered = false;

    public HotkeyService() {
        // Define F6 como atalho padrão inicial
        hotKeyConfig.add(NativeKeyEvent.VC_F6);
    }

    public void resetDefaultHotkey() {
        hotKeyConfig.clear();
        hotKeyConfig.add(NativeKeyEvent.VC_F6);
    }

    public boolean isCapturing() {
        return capturing;
    }

    /**
     * Registra o ouvinte nativo global para disparo do atalho configurado.
     * O callback é executado na thread de eventos do JNativeHook.
     */
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
     * Registra um ouvinte global para a tecla ESC, funcionando com a aplicação focada ou em background.
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

    /**
     * Retorna a representação textual formatada do atalho atualmente configurado (ex.: "Ctrl + Shift + F6").
     */
    public String getHotkeyText() {
        if (hotKeyConfig.isEmpty()) {
            return "Nenhum atalho configurado";
        }
        synchronized (hotKeyConfig) {
            return hotKeyConfig.stream()
                    .map(HotkeyService::formatKeyText)
                    .collect(Collectors.joining(" + "));
        }
    }

    /**
     * Formata o nome de uma tecla com fallback seguro para ambientes headless ou sem AWT Toolkit.
     */
    public static String formatKeyText(int keyCode) {
        try {
            return NativeKeyEvent.getKeyText(keyCode);
        } catch (Throwable t) {
            return getFallbackKeyText(keyCode);
        }
    }

    public static String getFallbackKeyText(int keyCode) {
        int awt = NativeKeyConfig.toAwtKeyCode(keyCode);
        if (awt >= KeyEvent.VK_F1 && awt <= KeyEvent.VK_F24) {
            return "F" + (awt - KeyEvent.VK_F1 + 1);
        }
        return switch (keyCode) {
            case NativeKeyEvent.VC_ESCAPE -> "Esc";
            case NativeKeyEvent.VC_ENTER -> "Enter";
            case NativeKeyEvent.VC_SPACE -> "Space";
            case NativeKeyEvent.VC_TAB -> "Tab";
            case NativeKeyEvent.VC_BACKSPACE -> "Backspace";
            case NativeKeyEvent.VC_CONTROL -> "Ctrl";
            case NativeKeyEvent.VC_ALT -> "Alt";
            case NativeKeyEvent.VC_SHIFT -> "Shift";
            case NativeKeyEvent.VC_META -> "Meta";
            case NativeKeyEvent.VC_UP -> "Up";
            case NativeKeyEvent.VC_DOWN -> "Down";
            case NativeKeyEvent.VC_LEFT -> "Left";
            case NativeKeyEvent.VC_RIGHT -> "Right";
            default -> {
                if (awt != KeyEvent.VK_UNDEFINED) {
                    char c = (char) awt;
                    if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                        yield String.valueOf(c);
                    }
                }
                yield "Key-" + keyCode;
            }
        };
    }

    /**
     * Ativa modo interativo de captura para gravação de nova combinação de teclas.
     */
    public void captureHotkey(Runnable onFinished) {
        capturing = true;
        Set<Integer> comboTemporario = new LinkedHashSet<>();

        NativeKeyListener listener = new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    GlobalScreen.removeNativeKeyListener(this);
                    comboTemporario.clear();
                    capturing = false;
                    if (onFinished != null) onFinished.run();
                    return;
                }
                comboTemporario.add(e.getKeyCode());
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                if (!comboTemporario.isEmpty()) {
                    hotKeyConfig.clear();
                    hotKeyConfig.addAll(comboTemporario);

                    GlobalScreen.removeNativeKeyListener(this);
                    capturing = false;

                    if (onFinished != null) onFinished.run();
                }
            }
        };

        GlobalScreen.addNativeKeyListener(listener);
    }
}
