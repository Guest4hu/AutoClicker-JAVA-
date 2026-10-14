package com.example.autoclicker;

import com.example.autoclicker.dto.WindowInfoDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WindowInfoDTOTest {

    @Test
    void testToStringWithTitle() {
        WindowInfoDTO dto = new WindowInfoDTO(0x1a2b, "Terminal");
        assertEquals("Terminal [0x1a2b]", dto.toString());
    }

    @Test
    void testToStringWithoutTitle() {
        WindowInfoDTO dto = new WindowInfoDTO(0x1a2b, "");
        assertEquals("Janela sem título [0x1a2b]", dto.toString());

        WindowInfoDTO dtoNull = new WindowInfoDTO(0x1a2b, null);
        assertEquals("Janela sem título [0x1a2b]", dtoNull.toString());
    }
}
