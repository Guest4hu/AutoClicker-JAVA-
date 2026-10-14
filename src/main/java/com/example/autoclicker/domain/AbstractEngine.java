package com.example.autoclicker.domain;

import com.example.autoclicker.input.simulator.AwtRobotInputSimulator;
import com.example.autoclicker.input.simulator.InputSimulator;
import com.example.autoclicker.input.simulator.VirtualInputSimulator;

import java.awt.GraphicsEnvironment;
import java.awt.event.InputEvent;

/**
 * Classe base abstrata para os motores de automação do sistema (AutoClicker e Macro).
 * Aplica o padrão de projeto Template Method para unificar o ciclo de vida da thread de execução,
 * controle do estado 'running', temporizações de alta precisão e segurança contra travamento de periféricos.
 */
public abstract class AbstractEngine {

    protected InputSimulator inputSimulator;
    protected volatile boolean running;
    protected Thread workerThread;
    protected Runnable finishListener;

    public AbstractEngine() {
        this(createDefaultSimulator());
    }

    public AbstractEngine(InputSimulator inputSimulator) {
        this.inputSimulator = inputSimulator != null ? inputSimulator : createDefaultSimulator();
    }

    public static InputSimulator createDefaultSimulator() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return new VirtualInputSimulator();
            }
            return new AwtRobotInputSimulator();
        } catch (Throwable t) {
            return new VirtualInputSimulator();
        }
    }

    public InputSimulator getInputSimulator() {
        return inputSimulator;
    }

    public void setInputSimulator(InputSimulator inputSimulator) {
        this.inputSimulator = inputSimulator;
    }

    public void setFinishListener(Runnable listener) {
        this.finishListener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Template Method que orquestra a inicialização e o ciclo de vida completo da execução em segundo plano.
     *
     * @param threadName Nome identificador da thread de trabalho
     */
    public synchronized void start(String threadName) {
        if (running) {
            return;
        }

        if (inputSimulator == null) {
            inputSimulator = createDefaultSimulator();
        }

        running = true;
        workerThread = new Thread(() -> {
            try {
                execute();
            } finally {
                running = false;
                cleanUp();
                if (finishListener != null) {
                    finishListener.run();
                }
            }
        }, threadName);

        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Interrompe com segurança a thread de execução e invoca a limpeza de periféricos.
     */
    public synchronized void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        cleanUp();
    }

    /**
     * Método abstrato contendo a lógica central de execução de cada motor.
     */
    protected abstract void execute();

    /**
     * Método abstrato chamado no encerramento ou interrupção para garantir que teclas ou botões
     * do mouse não permaneçam pressionados no sistema operacional.
     */
    protected abstract void cleanUp();

    /**
     * Pausa a execução com altíssima precisão até um momento futuro específico (em nanossegundos).
     * Combina Thread.sleep para intervalos longos com spin-wait para os milissegundos finais,
     * mitigando variações do escalonador do sistema operacional.
     *
     * @param targetTimeNanos timestamp em nanossegundos no futuro
     * @return true se o tempo foi atingido com sucesso; false se a execução foi interrompida
     */
    protected boolean sleepUntil(long targetTimeNanos) {
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

    /**
     * Pausa a execução por uma duração informada em milissegundos.
     *
     * @param millis duração em milissegundos
     * @return true se a pausa foi completada; false se foi interrompida
     */
    protected boolean sleepMillis(long millis) {
        if (millis <= 0) {
            return running;
        }
        long target = System.nanoTime() + (millis * 1_000_000L);
        return sleepUntil(target);
    }

    /**
     * Converte o código numérico do botão do mouse para a máscara de bits correspondente no AWT Robot.
     *
     * @param buttonMouse Código do botão (1 = Esquerdo, 2 = Direito, 3 = Meio)
     * @return Máscara de bits de InputEvent
     */
    protected int translateMouseButton(int buttonMouse) {
        return switch (buttonMouse) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK; // Botão Esquerdo
            case 2 -> InputEvent.BUTTON3_DOWN_MASK; // Botão Direito (no AWT é BUTTON3)
            case 3 -> InputEvent.BUTTON2_DOWN_MASK; // Botão do Meio / Scroll
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }
}
