package com.example.autoclicker.enums;

import java.awt.event.InputEvent;

public enum MouseButton {
    ESQUERDO(InputEvent.BUTTON1_DOWN_MASK, "Botão Esquerdo (Primário)"),
    DIREITO(InputEvent.BUTTON3_DOWN_MASK, "Botão Direito (Secundário)"),
    MEIO(InputEvent.BUTTON2_DOWN_MASK, "Botão do Meio (Scroll)");

    private final int label;
    private final String displayName;

    MouseButton(int label, String displayName) {
        this.label = label;
        this.displayName = displayName;
    }

    public int getLabel() {
        return label;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}