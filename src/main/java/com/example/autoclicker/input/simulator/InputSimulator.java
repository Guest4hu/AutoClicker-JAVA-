package com.example.autoclicker.input.simulator;

/**
 * Interface de abstração para simulação de comandos de entrada física (mouse e teclado).
 * Desacopla a lógica dos motores de automação da implementação concreta baseada em AWT Robot,
 * viabilizando execução de testes unitários isolados e execução em ambientes headless.
 */
public interface InputSimulator {

    /**
     * Move o cursor do mouse para as coordenadas absolutas (x, y) na tela.
     *
     * @param x coordenada horizontal
     * @param y coordenada vertical
     */
    void mouseMove(int x, int y);

    /**
     * Pressiona um ou mais botões do mouse usando a máscara de botões.
     *
     * @param buttons máscara de bits do botão do mouse (ex.: InputEvent.BUTTON1_DOWN_MASK)
     */
    void mousePress(int buttons);

    /**
     * Solta um ou mais botões do mouse.
     *
     * @param buttons máscara de bits do botão do mouse
     */
    void mouseRelease(int buttons);

    /**
     * Pressiona uma tecla do teclado usando seu código de tecla.
     *
     * @param keycode código da tecla
     */
    void keyPress(int keycode);

    /**
     * Solta uma tecla do teclado.
     *
     * @param keycode código da tecla
     */
    void keyRelease(int keycode);
}
