package com.example.autoclicker.enums;

public enum ClickType {
    SIMPLES(1, "Clique Simples"),
    DUPLO(2, "Clique Duplo");

    private final int clickMultiplier;
    private final String displayName;

    ClickType(int clickMultiplier, String displayName) {
        this.clickMultiplier = clickMultiplier;
        this.displayName = displayName;
    }

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