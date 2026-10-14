package com.example.autoclicker.domain.engine;

import com.example.autoclicker.domain.model.ActionType;
import com.example.autoclicker.domain.model.MacroActionSetting;
import com.example.autoclicker.dto.MacroPlaybackSettingDTO;
import com.example.autoclicker.dto.MacroSettingDTO;
import com.example.autoclicker.input.hotkey.NativeKeyConfig;
import com.example.autoclicker.input.simulator.InputSimulator;

import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Motor de domínio para reprodução de macros gravadas (sequências cronometradas de teclado e mouse).
 * Objeto de instância puro, desacoplado de singletons globais e de hardware AWT direto.
 */
public class MacroEngine extends AbstractEngine {

    private static final Logger LOGGER = Logger.getLogger(MacroEngine.class.getName());

    private MacroSettingDTO currentMacro;
    private MacroPlaybackSettingDTO currentPlayback = MacroPlaybackSettingDTO.once();
    private IntConsumer iterationListener;
    private int lastPressedMouseMask = 0;
    private int lastPressedAwtKey = 0;

    public MacroEngine() {
        super();
    }

    public MacroEngine(InputSimulator inputSimulator) {
        super(inputSimulator);
    }

    /**
     * Inicia a reprodução de uma macro em execução única (once).
     */
    public synchronized void startMacro(MacroSettingDTO macro) {
        startMacro(macro, MacroPlaybackSettingDTO.once());
    }

    /**
     * Inicia a reprodução de uma macro respeitando as configurações de repetição e intervalo.
     */
    public synchronized void startMacro(MacroSettingDTO macro, MacroPlaybackSettingDTO playbackSetting) {
        if (macro == null || macro.configs() == null || macro.configs().isEmpty()) {
            throw new IllegalStateException("Nenhuma ação gravada para executar.");
        }
        this.currentMacro = macro;
        this.currentPlayback = (playbackSetting != null) ? playbackSetting : MacroPlaybackSettingDTO.once();
        start("macro-clicker");
    }

    /**
     * Interrompe a execução ativa da macro.
     */
    public synchronized void stopMacro() {
        stop();
    }

    public boolean isMacroRunning() {
        return isRunning();
    }

    /**
     * Registra ouvinte notificado no início de cada execução da macro (ciclo de repetição).
     */
    public void setIterationListener(IntConsumer listener) {
        this.iterationListener = listener;
    }

    @Override
    protected void execute() {
        if (currentMacro == null || currentMacro.configs() == null) {
            return;
        }

        int completedRuns = 0;

        while (running && currentPlayback.hasNextRun(completedRuns)) {
            notifyIteration(completedRuns + 1);

            if (!playOnce()) {
                return;
            }
            completedRuns++;

            // Aguarda o intervalo configurado caso ainda haja uma próxima execução
            if (currentPlayback.hasNextRun(completedRuns)
                    && !sleepMillis(currentPlayback.intervalMillis())) {
                return;
            }
        }
    }

    /**
     * Reproduz uma única vez a sequência gravada de ações com fidelidade temporal.
     *
     * @return true se concluído normalmente; false se interrompido
     */
    private boolean playOnce() {
        long startTime = System.nanoTime();

        for (MacroActionSetting actionSetting : currentMacro.configs()) {
            if (!running) {
                return false;
            }

            long targetTime = startTime + (actionSetting.getKeyPressed() * 1_000_000L);
            if (!sleepUntil(targetTime) || !running) {
                return false;
            }

            if (actionSetting.getActionType() == ActionType.MOUSE || actionSetting.getActionType() == ActionType.MOUSE_ARRASTADO) {
                pressMouse(actionSetting);
            } else if (actionSetting.getActionType() == ActionType.TECLADO) {
                pressKey(actionSetting);
            }
        }
        return running;
    }

    private void notifyIteration(int runNumber) {
        if (iterationListener != null) {
            iterationListener.accept(runNumber);
        }
    }

    private void pressMouse(MacroActionSetting actionSetting) {
        int x1 = actionSetting.getXBefore();
        int y1 = actionSetting.getYBefore();
        int x2 = actionSetting.getXAfter();
        int y2 = actionSetting.getYAfter();
        int buttonMask = translateMouseButton(actionSetting.getKey() != null ? actionSetting.getKey() : 1);

        long duracaoMillis = actionSetting.getKeyReleased() - actionSetting.getKeyPressed();
        if (duracaoMillis <= 0) {
            duracaoMillis = 15;
        }

        inputSimulator.mouseMove(x1, y1);
        inputSimulator.mousePress(buttonMask);
        lastPressedMouseMask = buttonMask;

        boolean isDrag = (x1 != x2 || y1 != y2);

        if (isDrag) {
            int stepTimeMs = 10;
            int steps = (int) (duracaoMillis / stepTimeMs);
            if (steps <= 0) {
                steps = 1;
            }

            for (int i = 1; i <= steps && running; i++) {
                double progresso = (double) i / steps;
                int curX = (int) Math.round(x1 + (x2 - x1) * progresso);
                int curY = (int) Math.round(y1 + (y2 - y1) * progresso);

                inputSimulator.mouseMove(curX, curY);
                if (!sleepMillis(stepTimeMs)) {
                    break;
                }
            }
        } else {
            sleepMillis(duracaoMillis);
        }

        inputSimulator.mouseMove(x2, y2);
        try {
            inputSimulator.mouseRelease(buttonMask);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Erro ao liberar botão do mouse durante reprodução", e);
        }
        lastPressedMouseMask = 0;
    }

    private void pressKey(MacroActionSetting actionSetting) {
        if (actionSetting.getKey() == null) {
            return;
        }

        int awtKey = NativeKeyConfig.toAwtKeyCode(actionSetting.getKey());
        if (awtKey == KeyEvent.VK_UNDEFINED) {
            return;
        }

        long duracaoMillis = actionSetting.getKeyReleased() - actionSetting.getKeyPressed();
        if (duracaoMillis <= 0) {
            duracaoMillis = 15;
        }

        try {
            inputSimulator.keyPress(awtKey);
            lastPressedAwtKey = awtKey;
            sleepMillis(duracaoMillis);
        } finally {
            try {
                inputSimulator.keyRelease(awtKey);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Erro ao liberar tecla durante reprodução", e);
            }
            lastPressedAwtKey = 0;
        }
    }

    @Override
    protected void cleanUp() {
        if (lastPressedMouseMask != 0) {
            try {
                inputSimulator.mouseRelease(lastPressedMouseMask);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Erro ao liberar mouse durante limpeza", e);
            }
            lastPressedMouseMask = 0;
        }

        if (lastPressedAwtKey != 0) {
            try {
                inputSimulator.keyRelease(lastPressedAwtKey);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Erro ao liberar tecla durante limpeza", e);
            }
            lastPressedAwtKey = 0;
        }
    }
}
