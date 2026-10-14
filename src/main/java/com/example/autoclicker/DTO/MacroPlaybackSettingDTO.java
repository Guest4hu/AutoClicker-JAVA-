package com.example.autoclicker.DTO;

/**
 * Configuração de reprodução de uma macro: quantas vezes ela deve ser executada
 * e quanto tempo esperar entre uma execução e a próxima.
 *
 * @param infinite       se true, a macro repete até ser interrompida manualmente
 * @param repeatCount    quantidade de execuções (ignorado quando infinite = true)
 * @param intervalMillis pausa, em milissegundos, entre o fim de uma execução e o início da próxima
 */
public record MacroPlaybackSettingDTO(boolean infinite, int repeatCount, long intervalMillis) {

    public MacroPlaybackSettingDTO {
        if (!infinite && repeatCount <= 0) {
            throw new IllegalArgumentException("Quantidade de execuções deve ser maior que 0.");
        }
        if (intervalMillis < 0) {
            throw new IllegalArgumentException("Intervalo entre execuções não pode ser negativo.");
        }
    }

    /** Executa a macro uma única vez (comportamento original). */
    public static MacroPlaybackSettingDTO once() {
        return new MacroPlaybackSettingDTO(false, 1, 0);
    }

    public static MacroPlaybackSettingDTO infiniteRuns(long intervalMillis) {
        return new MacroPlaybackSettingDTO(true, 0, intervalMillis);
    }

    public static MacroPlaybackSettingDTO limitedRuns(int repeatCount, long intervalMillis) {
        return new MacroPlaybackSettingDTO(false, repeatCount, intervalMillis);
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
