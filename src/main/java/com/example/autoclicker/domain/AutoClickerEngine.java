package com.example.autoclicker.domain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AutoClickerEngine extends AbstractEngine {

    private static final AutoClickerEngine INSTANCE = new AutoClickerEngine();

    private static AutoClickerSetting setting;
    private static Consumer<Integer> progressListener;
    private static final AtomicInteger totalClicksCounter = new AtomicInteger(0);

    public static AutoClickerEngine getInstance() {
        return INSTANCE;
    }

    // --- Métodos Estáticos (Fachada para compatibilidade com o Controller) ---

    public static void setProgressListener(Consumer<Integer> listener) {
        progressListener = listener;
    }

    @Override
    public void setFinishListener(Runnable listener) {
        super.setFinishListener(listener);
        if (this != INSTANCE) {
            INSTANCE.setFinishListener(listener);
        }
    }

    public static int getTotalClicks() {
        return totalClicksCounter.get();
    }

    public static synchronized void startAutoClicker() {
        if (setting == null) {
            throw new IllegalStateException("Configurações do AutoClicker não definidas.");
        }
        totalClicksCounter.set(0);
        INSTANCE.start("auto-clicker-worker");
    }

    public static synchronized void stopAutoClicker() {
        INSTANCE.stop();
    }

    public static AutoClickerSetting getSetting() {
        return setting;
    }

    public static synchronized void setSetting(AutoClickerSetting newSetting) {
        setting = newSetting;
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() || INSTANCE.running;
    }

    // --- Implementação do Template Method ---

    @Override
    protected void execute() {
        if (setting == null) {
            return;
        }

        if (setting.getDelay() > 0 && !sleepMillis(setting.getDelay())) {
            return;
        }

        if (setting.isInfiniteClick()) {
            autoClickerInfinite();
        } else {
            autoClickerCounts();
        }
    }

    @Override
    protected void cleanUp() {
        releaseMouseSafely();
    }

    // --- Lógica Interna de Automação ---

    private void autoClickerInfinite() {
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

    private void autoClickerCounts() {
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
    }

    private void clickOnce() {
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

    private static void releaseMouseSafely() {
        if (setting != null && robot != null && setting.getMouseButton() != null) {
            try {
                robot.mouseRelease(setting.getMouseButton().getLabel());
            } catch (Exception ignored) {
            }
        }
    }
}
