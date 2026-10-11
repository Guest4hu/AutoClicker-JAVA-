package com.example.autoclicker.service;

import com.example.autoclicker.domain.MacroActionSetting;
import com.example.autoclicker.enums.ActionType;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MacroService {

    private List<MacroActionSetting> macroActions = new ArrayList<>();
    private NativeKeyListener currentKeyListener;
    private NativeMouseListener currentMouseListener;
    private volatile boolean capturing = false;

    public synchronized void captureMacro(Runnable onFinished) {
        // Se já estiver capturando, desregistra listeners anteriores
        stopCapture();

        macroActions.clear();
        capturing = true;
        final long currentTimeMilli = System.currentTimeMillis();
        final List<MacroActionSetting> macroActionsTemp = new ArrayList<>();

        NativeKeyListener keyListener = new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                // Se a tecla for ESC, encerra e salva as ações gravadas
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    long now = System.currentTimeMillis() - currentTimeMilli;
                    // Fecha qualquer ação que tenha ficado sem release
                    for (MacroActionSetting a : macroActionsTemp) {
                        if (a.getKeyReleased() <= 0) {
                            a.setKeyReleased(now);
                        }
                    }

                    macroActions = new ArrayList<>(macroActionsTemp);
                    stopCapture();

                    if (onFinished != null) {
                        onFinished.run();
                    }
                    return;
                }

                macroActionsTemp.add(
                        new MacroActionSetting(
                                ActionType.TECLADO,
                                e.getKeyCode(),
                                (System.currentTimeMillis() - currentTimeMilli)
                        )
                );
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                long now = System.currentTimeMillis() - currentTimeMilli;
                // Procura de trás para frente a ação de teclado correspondente que ainda não foi solta
                for (int i = macroActionsTemp.size() - 1; i >= 0; i--) {
                    MacroActionSetting a = macroActionsTemp.get(i);
                    if (a.getActionType() == ActionType.TECLADO
                            && Objects.equals(a.getKey(), e.getKeyCode())
                            && a.getKeyReleased() <= 0) {
                        a.setKeyReleased(now);
                        break;
                    }
                }
            }
        };

        NativeMouseListener mouseListener = new NativeMouseListener() {
            @Override
            public void nativeMousePressed(NativeMouseEvent nativeEvent) {
                macroActionsTemp.add(new MacroActionSetting(
                        ActionType.MOUSE,
                        nativeEvent.getButton(),
                        (System.currentTimeMillis() - currentTimeMilli),
                        nativeEvent.getY(),
                        nativeEvent.getX()
                ));
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
                long now = System.currentTimeMillis() - currentTimeMilli;
                // Procura de trás para frente a ação de mouse correspondente ao botão solto
                for (int i = macroActionsTemp.size() - 1; i >= 0; i--) {
                    MacroActionSetting a = macroActionsTemp.get(i);
                    if (a.getActionType() == ActionType.MOUSE
                            && Objects.equals(a.getKey(), nativeEvent.getButton())
                            && a.getKeyReleased() <= 0) {
                        a.setKeyReleased(now);
                        a.setYAfter(nativeEvent.getY());
                        a.setXAfter(nativeEvent.getX());
                        break;
                    }
                }
            }
        };

        this.currentKeyListener = keyListener;
        this.currentMouseListener = mouseListener;
        GlobalScreen.addNativeKeyListener(keyListener);
        GlobalScreen.addNativeMouseListener(mouseListener);
    }

    public synchronized void stopCapture() {
        capturing = false;
        if (currentKeyListener != null) {
            try {
                GlobalScreen.removeNativeKeyListener(currentKeyListener);
            } catch (Exception ignored) {}
            currentKeyListener = null;
        }
        if (currentMouseListener != null) {
            try {
                GlobalScreen.removeNativeMouseListener(currentMouseListener);
            } catch (Exception ignored) {}
            currentMouseListener = null;
        }
    }

    public boolean isCapturing() {
        return capturing;
    }

    public List<MacroActionSetting> getMacroActions() {
        return macroActions;
    }

    public void setMacroActions(List<MacroActionSetting> macroActions) {
        this.macroActions = macroActions;
    }

    @Override
    public String toString() {
        return "MacroService{" +
                "totalAcoes=" + macroActions.size() +
                ", gravando=" + capturing +
                '}';
    }
}
