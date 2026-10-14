package com.example.autoclicker.domain;

import com.example.autoclicker.dto.MacroPlaybackSettingDTO;
import com.example.autoclicker.dto.MacroSettingDTO;
import com.example.autoclicker.config.NativeKeyConfig;
import com.example.autoclicker.enums.ActionType;

import com.example.autoclicker.input.simulator.InputSimulator;
import java.awt.event.KeyEvent;
import java.util.function.IntConsumer;

public class MacroEngine extends AbstractEngine {

    private static final MacroEngine INSTANCE = new MacroEngine();

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

    public static MacroEngine getInstance() {
        return INSTANCE;
    }

    public static void setSimulator(InputSimulator simulator) {
        INSTANCE.setInputSimulator(simulator);
    }

    // --- Métodos Estáticos (Fachada para facilidade de uso) ---

    public static synchronized void startMacro(MacroSettingDTO macroActionDto) {
        INSTANCE.startWith(macroActionDto, MacroPlaybackSettingDTO.once());
    }

    public static synchronized void startMacro(MacroSettingDTO macroActionDto, MacroPlaybackSettingDTO playbackSetting) {
        INSTANCE.startWith(macroActionDto, playbackSetting);
    }

    public static synchronized void stopMacro() {
        INSTANCE.stop();
    }

    public void setFinishListener(Runnable listener) {
        INSTANCE.finishListener = listener;
    }

    /**
     * Registra um ouvinte notificado no início de cada execução da macro.
     * Recebe o número da execução atual (começando em 1).
     */
    public void setIterationListener(IntConsumer listener) {
        INSTANCE.iterationListener = listener;
    }

    public static boolean isMacroRunning() {
        return INSTANCE.isRunning();
    }

    // --- Métodos de Instância ---

    public synchronized void startWith(MacroSettingDTO macroActionDto) {
        startWith(macroActionDto, MacroPlaybackSettingDTO.once());
    }

    public synchronized void startWith(MacroSettingDTO macroActionDto, MacroPlaybackSettingDTO playbackSetting) {
        if (macroActionDto == null || macroActionDto.configs() == null || macroActionDto.configs().isEmpty()) {
            throw new IllegalStateException("Nenhuma ação gravada para executar.");
        }
        this.currentMacro = macroActionDto;
        this.currentPlayback = (playbackSetting != null) ? playbackSetting : MacroPlaybackSettingDTO.once();
        start("macro-clicker");
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

            // Só espera o intervalo se ainda houver uma próxima execução
            if (currentPlayback.hasNextRun(completedRuns)
                    && !sleepMillis(currentPlayback.intervalMillis())) {
                return;
            }
        }
    }

    /**
     * Reproduz uma única vez todas as ações da macro, respeitando os tempos gravados.
     *
     * @return true se a execução terminou normalmente; false se foi interrompida
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

            if (actionSetting.getActionType() == ActionType.MOUSE) {
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
        } catch (Exception ignored) {
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
            } catch (Exception ignored) {
            }
            lastPressedAwtKey = 0;
        }
    }

    @Override
    protected void cleanUp() {
        if (lastPressedMouseMask != 0) {
            try {
                inputSimulator.mouseRelease(lastPressedMouseMask);
            } catch (Exception ignored) {
            }
            lastPressedMouseMask = 0;
        }

        if (lastPressedAwtKey != 0) {
            try {
                inputSimulator.keyRelease(lastPressedAwtKey);
            } catch (Exception ignored) {
            }
            lastPressedAwtKey = 0;
        }
    }
}
