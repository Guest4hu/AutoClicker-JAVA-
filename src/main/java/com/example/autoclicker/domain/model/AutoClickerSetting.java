package com.example.autoclicker.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Objeto de valor que encapsula as configurações de execução do AutoClicker.
 * Contém regras de validação para garantir parâmetros consistentes antes do disparo do motor.
 */
public class AutoClickerSetting {

    private int interval;
    private int delay;
    private int clickCount;
    private boolean infiniteClick;
    private ClickType clickType;
    private MouseButton mouseButton;

    public AutoClickerSetting() {
    }

    public AutoClickerSetting(int interval, ClickType clickType, MouseButton mouseButton, int delay) {
        this.interval = interval;
        this.clickType = clickType;
        this.mouseButton = mouseButton;
        this.delay = delay;
    }

    /**
     * Factory method para criar configuração de cliques contínuos (modo infinito).
     */
    public static AutoClickerSetting infiniteClicks(int interval, ClickType clickType, MouseButton mouseButton, int delay) {
        AutoClickerSetting setting = new AutoClickerSetting(interval, clickType, mouseButton, delay);
        setting.setInfiniteClick(true);
        setting.setClickCount(0);
        return setting;
    }

    /**
     * Factory method para criar configuração de cliques limitados a uma quantidade determinada.
     */
    public static AutoClickerSetting limitedClicks(int interval, ClickType clickType, MouseButton mouseButton, int delay, int clickCount) {
        AutoClickerSetting setting = new AutoClickerSetting(interval, clickType, mouseButton, delay);
        setting.setInfiniteClick(false);
        setting.setClickCount(clickCount);
        return setting;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public boolean isInfiniteClick() {
        return infiniteClick;
    }

    public void setInfiniteClick(boolean infiniteClick) {
        this.infiniteClick = infiniteClick;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public void setClickType(ClickType clickType) {
        this.clickType = clickType;
    }

    public MouseButton getMouseButton() {
        return mouseButton;
    }

    public void setMouseButton(MouseButton mouseButton) {
        this.mouseButton = mouseButton;
    }

    /**
     * Valida as configurações e retorna a lista de mensagens de erro encontradas.
     * Retorna lista vazia se a configuração for plenamente válida.
     */
    public List<String> validateSetting() {
        List<String> errors = new ArrayList<>();

        if (interval <= 0) {
            errors.add("Intervalo entre cliques deve ser maior que 0.");
        }

        if (delay < 0) {
            errors.add("Atraso inicial deve ser maior ou igual a 0.");
        }

        if (!isInfiniteClick() && clickCount <= 0) {
            errors.add("Quantidade de cliques deve ser maior que 0 quando Infinito estiver desmarcado.");
        }

        if (isInfiniteClick() && clickCount != 0) {
            errors.add("Para cliques infinitos, a quantidade de cliques deve ficar vazia.");
        }

        if (Objects.isNull(clickType)) {
            errors.add("Tipo de clique e obrigatorio.");
        }

        if (Objects.isNull(mouseButton)) {
            errors.add("Botao do mouse e obrigatorio.");
        }

        return errors;
    }

    @Override
    public String toString() {
        return "AutoClickerSetting{" +
                "interval=" + interval +
                ", delay=" + delay +
                ", clickCount=" + clickCount +
                ", infiniteClick=" + infiniteClick +
                ", clickType=" + clickType +
                ", mouseButton=" + mouseButton +
                '}';
    }
}
