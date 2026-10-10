package com.example.autoclicker.domain;

import com.example.autoclicker.enums.ActionType;

public class MacroActionConfig {
    private ActionType actionType;
    private Integer key;
    private long keyPressed;
    private long keyReleased;
    private int y;
    private int x;

    public MacroActionConfig(ActionType actionType, Integer key, long milliseconds) {
        this.actionType = actionType;
        this.key = key;
        this.keyPressed = milliseconds;
    }

    public MacroActionConfig(ActionType actionType, Integer key, long milliseconds, int y, int x) {
        this.actionType = actionType;
        this.key = key;
        this.keyPressed = milliseconds;
        this.y = y;
        this.x = x;
    }

    public MacroActionConfig() {

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

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }
}
