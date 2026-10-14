package com.example.autoclicker.domain;

import com.example.autoclicker.input.simulator.InputSimulator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AutoClickerEngine extends AbstractEngine {

    private static final Logger LOGGER = Logger.getLogger(AutoClickerEngine.class.getName());
    private static final AutoClickerEngine INSTANCE = new AutoClickerEngine();

    private static AutoClickerSetting setting;
    private static Consumer<Integer> progressListener;
    private static final AtomicInteger totalClicksCounter = new AtomicInteger(0);
    private volatile boolean isMousePressed = false;

    public AutoClickerEngine() {
        super();
    }

    public AutoClickerEngine(InputSimulator inputSimulator) {
        super(inputSimulator);
    }

    public static AutoClickerEngine getInstance() {
        return INSTANCE;
    }

    public static void setSimulator(InputSimulator simulator) {
        INSTANCE.setInputSimulator(simulator);
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

        runClickLoop();
    }

    @Override
    protected void cleanUp() {
        releaseMouseSafely();
    }

    // --- Lógica Interna de Automação ---

    private void runClickLoop() {
        boolean infinite = setting.isInfiniteClick();
        int remainingClicks = setting.getClickCount();
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        long targetTimeNanos = System.nanoTime();

        while (running && (infinite || remainingClicks > 0)) {
            clickOnce();
            if (!infinite) {
                remainingClicks--;
            }

            boolean hasNext = running && (infinite || remainingClicks > 0);
            if (hasNext) {
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
    }

    private void clickOnce() {
        int totalClicks = setting.getClickType().getClickMultiplier();
        int mouseMask = setting.getMouseButton().getMask();

        // 5 milissegundos em nanossegundos (padrão ideal para jogos registrarem o clique)
        long defaultHoldNanos = 5_000_000L;
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        // Trava de segurança: nunca segurar por mais da metade do tempo do intervalo total
        long maxHoldNanos = Math.max(100_000L, intervalNanos / 2);
        long holdNanos = Math.min(defaultHoldNanos, maxHoldNanos);

        for (int i = 0; i < totalClicks && running; i++) {
            inputSimulator.mousePress(mouseMask);
            isMousePressed = true;
            sleepUntil(System.nanoTime() + holdNanos);
            inputSimulator.mouseRelease(mouseMask);
            isMousePressed = false;

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

    private void releaseMouseSafely() {
        if (isMousePressed && setting != null && inputSimulator != null && setting.getMouseButton() != null) {
            try {
                inputSimulator.mouseRelease(setting.getMouseButton().getMask());
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Erro ao liberar mouse durante limpeza", e);
            } finally {
                isMousePressed = false;
            }
        }
    }
}
