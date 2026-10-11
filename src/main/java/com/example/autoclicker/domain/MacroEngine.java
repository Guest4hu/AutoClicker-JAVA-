package com.example.autoclicker.domain;

import com.example.autoclicker.DTO.MacroSettingDTO;
import com.example.autoclicker.config.NativeKeyConfig;
import com.example.autoclicker.enums.ActionType;

import java.awt.event.KeyEvent;

public class MacroEngine extends AbstractEngine {

    private static final MacroEngine INSTANCE = new MacroEngine();

    private MacroSettingDTO currentMacro;
    private int lastPressedMouseMask = 0;
    private int lastPressedAwtKey = 0;

    public static MacroEngine getInstance() {
        return INSTANCE;
    }

    // --- Métodos Estáticos (Fachada para facilidade de uso) ---

    public static synchronized void startMacro(MacroSettingDTO macroActionDto) {
        INSTANCE.startWith(macroActionDto);
    }

    public static synchronized void stopMacro() {
        INSTANCE.stop();
    }

    public void setFinishListener(Runnable listener) {
        INSTANCE.finishListener = listener;
    }

    public static boolean isMacroRunning() {
        return INSTANCE.isRunning();
    }

    // --- Métodos de Instância ---

    public synchronized void startWith(MacroSettingDTO macroActionDto) {
        if (macroActionDto == null || macroActionDto.configs() == null || macroActionDto.configs().isEmpty()) {
            throw new IllegalStateException("Nenhuma ação gravada para executar.");
        }
        this.currentMacro = macroActionDto;
        start("macro-clicker");
    }

    @Override
    protected void execute() {
        if (currentMacro == null || currentMacro.configs() == null) {
            return;
        }

        long startTime = System.nanoTime();

        for (MacroActionSetting actionSetting : currentMacro.configs()) {
            if (!running) {
                break;
            }

            long targetTime = startTime + (actionSetting.getKeyPressed() * 1_000_000L);
            if (!sleepUntil(targetTime)) {
                break;
            }

            if (!running) {
                break;
            }

            if (actionSetting.getActionType() == ActionType.MOUSE) {
                pressMouse(actionSetting);
            } else if (actionSetting.getActionType() == ActionType.TECLADO) {
                pressKey(actionSetting);
            }
        }
    }

    private void pressMouse(MacroActionSetting actionSetting) {
        int x1 = actionSetting.getxBefore();
        int y1 = actionSetting.getyBefore();
        int x2 = actionSetting.getxAfter();
        int y2 = actionSetting.getyAfter();
        int buttonMask = translateMouseButton(actionSetting.getKey() != null ? actionSetting.getKey() : 1);

        long duracaoMillis = actionSetting.getKeyReleased() - actionSetting.getKeyPressed();
        if (duracaoMillis <= 0) {
            duracaoMillis = 15;
        }

        robot.mouseMove(x1, y1);
        robot.mousePress(buttonMask);
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

                robot.mouseMove(curX, curY);
                if (!sleepMillis(stepTimeMs)) {
                    break;
                }
            }
        } else {
            sleepMillis(duracaoMillis);
        }

        robot.mouseMove(x2, y2);
        try {
            robot.mouseRelease(buttonMask);
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
            robot.keyPress(awtKey);
            lastPressedAwtKey = awtKey;
            sleepMillis(duracaoMillis);
        } finally {
            try {
                robot.keyRelease(awtKey);
            } catch (Exception ignored) {
            }
            lastPressedAwtKey = 0;
        }
    }

    @Override
    protected void cleanUp() {
        if (lastPressedMouseMask != 0) {
            try {
                robot.mouseRelease(lastPressedMouseMask);
            } catch (Exception ignored) {
            }
            lastPressedMouseMask = 0;
        }

        if (lastPressedAwtKey != 0) {
            try {
                robot.keyRelease(lastPressedAwtKey);
            } catch (Exception ignored) {
            }
            lastPressedAwtKey = 0;
        }
    }
}
