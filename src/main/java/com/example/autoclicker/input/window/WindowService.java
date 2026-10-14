package com.example.autoclicker.input.window;

import com.example.autoclicker.dto.WindowInfoDTO;
import java.util.List;

/**
 * Contrato de serviço para inspeção e enumeração de janelas ativas do sistema operacional.
 */
public interface WindowService {

    /**
     * Retorna a lista de janelas visíveis e identificáveis no servidor gráfico do sistema operacional.
     */
    List<WindowInfoDTO> getOpenWindows();
}
