package com.example.autoclicker.domain;


import java.awt.AWTException;
import java.awt.Robot;
import java.util.List;

public class AutoClickerEngine {
    private static AutoClickerSetting setting;
    private static Robot robot;
    private static Thread workerThread;
    private static volatile boolean running;

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void startAutoClicker() {
        if (setting == null) {
            throw new IllegalStateException("Configuracoes do AutoClicker nao definidas.");
        }
        if (running) {
            return;
        }

        running = true;
        workerThread = new Thread(() -> {
            try {
                if (setting.getDelay() > 0 && !sleepMillis(setting.getDelay())) {
                    return;
                }

                if (setting.isInfiniteClick()) {
                    autoClickerInfinite();
                } else {
                    autoClickerCounts();
                }
            } finally {
                running = false;
            }
        }, "auto-clicker-worker");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    private static void autoClickerInfinite() {
        while (running) {
            clickOnce();
            if (!sleepMillis(setting.getInterval())) {
                return;
            }
        }
    }

    private static void autoClickerCounts() {
        int remainingClicks = setting.getClickCount();

        while (running && remainingClicks > 0) {
            clickOnce();
            remainingClicks--;

            if (remainingClicks > 0 && !sleepMillis(setting.getInterval())) {
                return;
            }
        }

        running = false;
    }

    private static void clickOnce() {
        int totalClicks = setting.getClickType().getLabel();
        int mouseMask = setting.getMouseButton().getLabel();

        for (int i = 0; i < totalClicks && running; i++) {
            robot.mousePress(mouseMask);
            robot.mouseRelease(mouseMask);
        }
    }

    private static boolean sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
            return running;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return false;
        }
    }

    public static synchronized void stopAutoClicker() {
        running = false;

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    public static AutoClickerSetting getSetting() {
        return setting;
    }

    public static synchronized void setSetting(AutoClickerSetting setting) {
        AutoClickerEngine.setting = setting;
    }

    public static boolean isRunning() {
        return running;
    }
}
