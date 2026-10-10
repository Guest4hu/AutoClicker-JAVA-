package com.example.autoclicker.domain;


import java.awt.AWTException;
import java.awt.Robot;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AutoClickerEngine {
    private static AutoClickerSetting setting;
    private static Robot robot;
    private static Thread workerThread;
    private static volatile boolean running;
    private static Consumer<Integer> progressListener;
    private static Runnable finishListener;
    private static final AtomicInteger totalClicksCounter = new AtomicInteger(0);

    static {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setProgressListener(Consumer<Integer> listener) {
        progressListener = listener;
    }

    public static void setFinishListener(Runnable listener) {
        finishListener = listener;
    }

    public static int getTotalClicks() {
        return totalClicksCounter.get();
    }

    public static synchronized void startAutoClicker() {
        if (setting == null) {
            throw new IllegalStateException("Configurações do AutoClicker não definidas.");
        }
        if (running) {
            return;
        }

        totalClicksCounter.set(0);
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
                releaseMouseSafely();
            }
        }, "auto-clicker-worker");

        workerThread.setDaemon(true);
        workerThread.start();
    }

    private static void autoClickerInfinite() {
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        long targetTimeNanos = System.nanoTime();

        while (running) {
            clickOnce();
            if (!running) {
                return;
            }

            targetTimeNanos += intervalNanos;
            long now = System.nanoTime();

            // Proteção contra lag spike: se atrasou mais de 1 ciclo, reseta o alvo
            if (targetTimeNanos < now - intervalNanos) {
                targetTimeNanos = now;
            }

            if (!sleepUntil(targetTimeNanos)) {
                return;
            }
        }
    }

    private static void autoClickerCounts() {
        int remainingClicks = setting.getClickCount();
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        long targetTimeNanos = System.nanoTime();

        while (running && remainingClicks > 0) {
            clickOnce();
            remainingClicks--;

            if (remainingClicks > 0 && running) {
                targetTimeNanos += intervalNanos;
                long now = System.nanoTime();

                if (targetTimeNanos < now - intervalNanos) {
                    targetTimeNanos = now;
                }

                if (!sleepUntil(targetTimeNanos)) {
                    return;
                }
            }
        }

        running = false;
        if (finishListener != null) {
            finishListener.run();
        }
    }

    private static void clickOnce() {
        int totalClicks = setting.getClickType().getLabel();
        int mouseMask = setting.getMouseButton().getLabel();

        // 5 milissegundos em nanossegundos (padrão ideal para jogos registrarem o clique)
        long defaultHoldNanos = 5_000_000L;
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        // Trava de segurança: nunca segurar por mais da metade do tempo do intervalo total
        long maxHoldNanos = Math.max(100_000L, intervalNanos / 2);
        long holdNanos = Math.min(defaultHoldNanos, maxHoldNanos);

        for (int i = 0; i < totalClicks && running; i++) {
            robot.mousePress(mouseMask);
            sleepUntil(System.nanoTime() + holdNanos);
            robot.mouseRelease(mouseMask);

            // Se for clique duplo, dá uma pequena folga antes do segundo clique
            if (i < totalClicks - 1 && running) {
                sleepUntil(System.nanoTime() + holdNanos);
            }
        }

        int count = totalClicksCounter.incrementAndGet();
        if (progressListener != null) {
            progressListener.accept(count);
        }
    }

    private static boolean sleepUntil(long targetTimeNanos) {
        final long SPIN_THRESHOLD_NANOS = 2_000_000L; // 2 ms

        while (running) {
            long remaining = targetTimeNanos - System.nanoTime();
            if (remaining <= 0) {
                return true;
            }

            if (remaining > SPIN_THRESHOLD_NANOS) {
                long sleepMillis = (remaining - SPIN_THRESHOLD_NANOS) / 1_000_000L;
                if (sleepMillis > 0) {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        return false;
                    }
                } else {
                    Thread.yield();
                }
            } else {
                Thread.onSpinWait();
            }
        }
        return running;
    }

    private static boolean sleepMillis(int millis) {
        if (millis <= 0) {
            return running;
        }
        long target = System.nanoTime() + (long) millis * 1_000_000L;
        return sleepUntil(target);
    }

    public static synchronized void stopAutoClicker() {
        running = false;

        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        releaseMouseSafely();
    }

    private static void releaseMouseSafely() {
        if (setting != null && robot != null && setting.getMouseButton() != null) {
            try {
                robot.mouseRelease(setting.getMouseButton().getLabel());
            } catch (Exception ignored) {
            }
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
