package com.example.autoclicker.service;

import com.example.autoclicker.domain.MacroActionConfig;
import com.example.autoclicker.enums.ActionType;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class MacroService {

    List<MacroActionConfig> macroActions = new ArrayList<>();
    NativeMouseListener mouseListener = new NativeMouseListener() {
    };
    public void captureMacro(Runnable onFinished) {

        macroActions.clear();
        long currentTimeMilli = System.currentTimeMillis();
        List<MacroActionConfig> macroActionsTemp = new ArrayList<>();
        NativeKeyListener keyListener = new NativeKeyListener() {

            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                // Se a tecla for ESC (NativeKeyEvent.VC_ESCAPE), nós cancelamos!
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    macroActions = macroActionsTemp;

                    GlobalScreen.removeNativeKeyListener(this);
                    GlobalScreen.removeNativeMouseListener(mouseListener);

                    if (onFinished != null) onFinished.run();
                    return;
                }
                macroActionsTemp.add(
                        new MacroActionConfig(
                                ActionType.TECLADO,
                                e.getKeyCode(),
                                (System.currentTimeMillis() - currentTimeMilli)
                        )
                );
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) {
                // Quando soltar e tivermos teclas acumuladas:
                macroActionsTemp.getLast().setKeyReleased((System.currentTimeMillis() - currentTimeMilli));
            }
        };

        NativeMouseListener mouseListener = new NativeMouseListener() {
            @Override
            public void nativeMousePressed(NativeMouseEvent nativeEvent) {
                macroActionsTemp.add(new MacroActionConfig(
                        ActionType.MOUSE,
                        nativeEvent.getButton(),
                        (System.currentTimeMillis() - currentTimeMilli),
                        nativeEvent.getY(),
                        nativeEvent.getX()
                ));
            }

            @Override
            public void nativeMouseReleased(NativeMouseEvent nativeEvent) {
                macroActionsTemp.getLast().setKeyReleased((System.currentTimeMillis() - currentTimeMilli));
            }
        };
        this.mouseListener = mouseListener;
        GlobalScreen.addNativeKeyListener(keyListener);
        GlobalScreen.addNativeMouseListener(mouseListener);
    }
}
