package com.example.autoclicker.domain;

import com.example.autoclicker.enums.ActionType;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class MacroActionSetting {
    private ActionType actionType;
    private Integer key;
    private long keyPressed;
    private long keyReleased;
    private int yBefore;
    private int xBefore;
    private int yAfter;
    private int xAfter;

    public MacroActionSetting(ActionType actionType, Integer key, long milliseconds) {
        this.actionType = actionType;
        this.key = key;
        this.keyPressed = milliseconds;
    }

    public MacroActionSetting(ActionType actionType, Integer key, long milliseconds, int y, int x) {
        this.actionType = actionType;
        this.key = key;
        this.keyPressed = milliseconds;
        this.yBefore = y;
        this.xBefore = x;
        this.yAfter = y;
        this.xAfter = x;
    }

    public MacroActionSetting() {
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public long getKeyPressed() {
        return keyPressed;
    }

    public void setKeyPressed(long keyPressed) {
        this.keyPressed = keyPressed;
    }

    public long getKeyReleased() {
        return keyReleased;
    }

    public void setKeyReleased(long keyReleased) {
        this.keyReleased = keyReleased;
    }

    // --- Coordenadas Iniciais ---

    public int getYBefore() {
        return yBefore;
    }

    public void setYBefore(int yBefore) {
        this.yBefore = yBefore;
    }

    public int getXBefore() {
        return xBefore;
    }

    public void setXBefore(int xBefore) {
        this.xBefore = xBefore;
    }

    // --- Coordenadas Finais ---

    public int getYAfter() {
        return yAfter;
    }

    public void setYAfter(int yAfter) {
        this.yAfter = yAfter;
    }

    public int getXAfter() {
        return xAfter;
    }

    public void setXAfter(int xAfter) {
        this.xAfter = xAfter;
    }

    @Override
    public String toString() {
        long duracao = (keyReleased > keyPressed) ? (keyReleased - keyPressed) : 0;
        double inicioSegundos = keyPressed / 1000.0;

        if (actionType == ActionType.TECLADO) {
            String nomeTecla = (key != null) ? com.example.autoclicker.service.HotkeyService.formatKeyText(key) : "Desconhecida";
            return String.format("[+%6.3fs] TECLADO  | Tecla: %-12s | Duração: %4d ms", inicioSegundos, "[" + nomeTecla + "]", duracao);
        }

        if (actionType == ActionType.MOUSE) {
            String botao = switch (key != null ? key : 0) {
                case 1 -> "Esquerdo";
                case 2 -> "Direito";
                case 3 -> "Meio/Scroll";
                default -> "Botão " + key;
            };

            boolean isDrag = (xBefore != xAfter || yBefore != yAfter);
            if (isDrag) {
                return String.format("[+%6.3fs] ARRASTAR | %-12s de (%4d, %4d) até (%4d, %4d) | Duração: %4d ms",
                        inicioSegundos, botao, xBefore, yBefore, xAfter, yAfter, duracao);
            }

            return String.format("[+%6.3fs] MOUSE    | %-12s em (%4d, %4d) | Duração: %4d ms", inicioSegundos, botao, xBefore, yBefore, duracao);
        }

        return String.format("[+%6.3fs] AÇÃO     | Posição: (%4d, %4d)", inicioSegundos, xBefore, yBefore);
    }
}
