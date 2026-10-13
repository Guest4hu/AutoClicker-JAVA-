package com.example.autoclicker.domain;

/**
 * Configuração de reprodução de uma macro: quantas vezes ela deve ser executada
 * e quanto tempo esperar entre uma execução e a próxima.
 *
 * @param infinite       se true, a macro repete até ser interrompida manualmente
 * @param repeatCount    quantidade de execuções (ignorado quando infinite = true)
 * @param intervalMillis pausa, em milissegundos, entre o fim de uma execução e o início da próxima
 */
public record MacroPlaybackSetting(boolean infinite, int repeatCount, long intervalMillis) {

    public MacroPlaybackSetting {
        if (!infinite && repeatCount <= 0) {
            throw new IllegalArgumentException("Quantidade de execuções deve ser maior que 0.");
        }
        if (intervalMillis < 0) {
            throw new IllegalArgumentException("Intervalo entre execuções não pode ser negativo.");
        }
    }

    /** Executa a macro uma única vez (comportamento original). */
    public static MacroPlaybackSetting once() {
        return new MacroPlaybackSetting(false, 1, 0);
    }

    public static MacroPlaybackSetting infiniteRuns(long intervalMillis) {
        return new MacroPlaybackSetting(true, 0, intervalMillis);
    }

    public static MacroPlaybackSetting limitedRuns(int repeatCount, long intervalMillis) {
        return new MacroPlaybackSetting(false, repeatCount, intervalMillis);
    }

    /**
     * Indica se ainda há execuções a fazer.
     *
     * @param completedRuns quantidade de execuções já concluídas
     */
    public boolean hasNextRun(int completedRuns) {
        return infinite || completedRuns < repeatCount;
    }
}
