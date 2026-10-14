package com.example.autoclicker.domain.model;

import java.awt.event.InputEvent;

/**
 * Representa os botões do mouse suportados para emulação e disparo no AutoClicker.
 */
public enum MouseButton {
    ESQUERDO(InputEvent.BUTTON1_DOWN_MASK, "Botão Esquerdo (Primário)"),
    DIREITO(InputEvent.BUTTON3_DOWN_MASK, "Botão Direito (Secundário)"),
    MEIO(InputEvent.BUTTON2_DOWN_MASK, "Botão do Meio (Scroll)");

    private final int mask;
    private final String displayName;

    MouseButton(int mask, String displayName) {
        this.mask = mask;
        this.displayName = displayName;
    }

    /**
     * Retorna a máscara binária de evento do mouse (AWT InputEvent mask).
     */
    public int getMask() {
        return mask;
    }

    @Deprecated
    public int getLabel() {
        return getMask();
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
