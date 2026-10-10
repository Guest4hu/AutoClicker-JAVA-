package com.example.autoclicker.enums;

public enum ClickType {
    SIMPLES(1, "Clique Simples"),
    DUPLO(2, "Clique Duplo");

    private final int label;
    private final String displayName;

    ClickType(int label, String displayName) {
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