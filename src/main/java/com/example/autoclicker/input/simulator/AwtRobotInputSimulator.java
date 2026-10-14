package com.example.autoclicker.input.simulator;

import java.awt.AWTException;
import java.awt.Robot;
import java.util.Objects;

/**
 * Implementação de {@link InputSimulator} delegando operações ao {@link Robot} da biblioteca AWT.
 * Utilizada em produção para interação real com o subsistema gráfico do sistema operacional.
 */
public class AwtRobotInputSimulator implements InputSimulator {

    private final Robot robot;

    public AwtRobotInputSimulator() {
        try {
            this.robot = new Robot();
        } catch (AWTException | java.awt.HeadlessException e) {
            throw new IllegalStateException("Falha ao inicializar o Robot do AWT: " + e.getMessage(), e);
        }
    }

    public AwtRobotInputSimulator(Robot robot) {
        this.robot = Objects.requireNonNull(robot, "Robot não pode ser nulo");
    }

    @Override
    public void mouseMove(int x, int y) {
        robot.mouseMove(x, y);
    }

    @Override
    public void mousePress(int buttons) {
        robot.mousePress(buttons);
    }

    @Override
    public void mouseRelease(int buttons) {
        robot.mouseRelease(buttons);
    }

    @Override
    public void keyPress(int keycode) {
        robot.keyPress(keycode);
    }

    @Override
    public void keyRelease(int keycode) {
        robot.keyRelease(keycode);
    }
}
