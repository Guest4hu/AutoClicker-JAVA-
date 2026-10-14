package com.example.autoclicker.domain.engine;

import com.example.autoclicker.domain.model.AutoClickerSetting;
import com.example.autoclicker.input.simulator.InputSimulator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Motor de domínio para execução de sequências de cliques automatizados (finitos ou contínuos).
 * Objeto de instância puro, sem dependências de estado global ou singletons.
 */
public class AutoClickerEngine extends AbstractEngine {

    private static final Logger LOGGER = Logger.getLogger(AutoClickerEngine.class.getName());

    private AutoClickerSetting setting;
    private Consumer<Integer> progressListener;
    private final AtomicInteger totalClicksCounter = new AtomicInteger(0);
    private volatile boolean isMousePressed = false;

    public AutoClickerEngine() {
        super();
    }

    public AutoClickerEngine(InputSimulator inputSimulator) {
        super(inputSimulator);
    }

    public AutoClickerSetting getSetting() {
        return setting;
    }

    public synchronized void setSetting(AutoClickerSetting setting) {
        this.setting = setting;
    }

    public void setProgressListener(Consumer<Integer> progressListener) {
        this.progressListener = progressListener;
    }

    public int getTotalClicks() {
        return totalClicksCounter.get();
    }

    /**
     * Inicia o ciclo de cliques com base nas configurações vigentes.
     *
     * @throws IllegalStateException se as configurações não tiverem sido definidas
     */
    public synchronized void startAutoClicker() {
        if (setting == null) {
            throw new IllegalStateException("Configurações do AutoClicker não definidas.");
        }
        totalClicksCounter.set(0);
        start("auto-clicker-worker");
    }

    /**
     * Solicita a interrupção da execução do AutoClicker e libera o mouse se pressionado.
     */
    public synchronized void stopAutoClicker() {
        stop();
    }

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

    /**
     * Loop unificado de cliques com compensação de deriva temporal e proteção contra travamento/lag spike.
     */
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

                // Proteção contra lag spike: reseta a janela temporal se atrasar mais de um ciclo
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

        long defaultHoldNanos = 5_000_000L; // 5ms de acionamento ideal
        long intervalNanos = (long) setting.getInterval() * 1_000_000L;
        long maxHoldNanos = Math.max(100_000L, intervalNanos / 2);
        long holdNanos = Math.min(defaultHoldNanos, maxHoldNanos);

        for (int i = 0; i < totalClicks && running; i++) {
            inputSimulator.mousePress(mouseMask);
            isMousePressed = true;
            sleepUntil(System.nanoTime() + holdNanos);
            inputSimulator.mouseRelease(mouseMask);
            isMousePressed = false;

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
                LOGGER.log(Level.FINE, "Erro ao liberar botão do mouse na limpeza", e);
            } finally {
                isMousePressed = false;
            }
        }
    }
}
