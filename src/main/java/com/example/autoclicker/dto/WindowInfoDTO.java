package com.example.autoclicker.dto;

/**
 * Representa uma janela ativa no sistema operacional.
 *
 * @param id    identificador numérico único da janela (Window ID no X11)
 * @param title título visível da janela
 */
public record WindowInfoDTO(long id, String title) {

    @Override
    public String toString() {
        if (title == null || title.isBlank()) {
            return String.format("Janela sem título [0x%x]", id);
        }
        return String.format("%s [0x%x]", title, id);
    }
}
