package com.example.autoclicker.input.simulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulador de entrada puramente em memória para testes automatizados e ambientes headless.
 * Não interage com periféricos físicos do sistema operacional e registra chamadas para asserções em testes.
 */
public class VirtualInputSimulator implements InputSimulator {

    public enum EventType {
        MOUSE_MOVE,
        MOUSE_PRESS,
        MOUSE_RELEASE,
        KEY_PRESS,
        KEY_RELEASE
    }

    public record InputEventRecord(EventType type, int param1, int param2) {}

    private final List<InputEventRecord> events = new CopyOnWriteArrayList<>();
    private final AtomicInteger mousePressCount = new AtomicInteger(0);
    private final AtomicInteger mouseReleaseCount = new AtomicInteger(0);
    private final AtomicInteger keyPressCount = new AtomicInteger(0);
    private final AtomicInteger keyReleaseCount = new AtomicInteger(0);
    private volatile int currentX = 0;
    private volatile int currentY = 0;

    @Override
    public void mouseMove(int x, int y) {
        this.currentX = x;
        this.currentY = y;
        events.add(new InputEventRecord(EventType.MOUSE_MOVE, x, y));
    }

    @Override
    public void mousePress(int buttons) {
        mousePressCount.incrementAndGet();
        events.add(new InputEventRecord(EventType.MOUSE_PRESS, buttons, 0));
    }

    @Override
    public void mouseRelease(int buttons) {
        mouseReleaseCount.incrementAndGet();
        events.add(new InputEventRecord(EventType.MOUSE_RELEASE, buttons, 0));
    }

    @Override
    public void keyPress(int keycode) {
        keyPressCount.incrementAndGet();
        events.add(new InputEventRecord(EventType.KEY_PRESS, keycode, 0));
    }

    @Override
    public void keyRelease(int keycode) {
        keyReleaseCount.incrementAndGet();
        events.add(new InputEventRecord(EventType.KEY_RELEASE, keycode, 0));
    }

    public List<InputEventRecord> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public int getMousePressCount() {
        return mousePressCount.get();
    }

    public int getMouseReleaseCount() {
        return mouseReleaseCount.get();
    }

    public int getKeyPressCount() {
        return keyPressCount.get();
    }

    public int getKeyReleaseCount() {
        return keyReleaseCount.get();
    }

    public int getCurrentX() {
        return currentX;
    }

    public int getCurrentY() {
        return currentY;
    }

    public void clear() {
        events.clear();
        mousePressCount.set(0);
        mouseReleaseCount.set(0);
        keyPressCount.set(0);
        keyReleaseCount.set(0);
    }
}
