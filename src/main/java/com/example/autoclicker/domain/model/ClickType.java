package com.example.autoclicker.domain.model;

/**
 * Define a modalidade de clique efetuada pelo AutoClicker (simples ou duplo).
 */
public enum ClickType {
    SIMPLES(1, "Clique Simples"),
    DUPLO(2, "Clique Duplo");

    private final int clickMultiplier;
    private final String displayName;

    ClickType(int clickMultiplier, String displayName) {
        this.clickMultiplier = clickMultiplier;
        this.displayName = displayName;
    }

    /**
     * Retorna a quantidade de cliques disparados por acionamento.
     */
    public int getClickMultiplier() {
        return clickMultiplier;
    }

    @Deprecated
    public int getLabel() {
        return getClickMultiplier();
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
