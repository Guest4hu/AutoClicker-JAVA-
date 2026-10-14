package com.example.autoclicker;

import com.example.autoclicker.dto.MacroPlaybackSettingDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MacroPlaybackSettingDTOTest {

    @Test
    void testOnce() {
        MacroPlaybackSettingDTO dto = MacroPlaybackSettingDTO.once();
        assertFalse(dto.infinite());
        assertEquals(1, dto.repeatCount());
        assertEquals(0, dto.intervalMillis());
        assertTrue(dto.hasNextRun(0));
        assertFalse(dto.hasNextRun(1));
    }

    @Test
    void testLimitedRuns() {
        MacroPlaybackSettingDTO dto = MacroPlaybackSettingDTO.limitedRuns(3, 100);
        assertFalse(dto.infinite());
        assertEquals(3, dto.repeatCount());
        assertEquals(100, dto.intervalMillis());

        assertTrue(dto.hasNextRun(0));
        assertTrue(dto.hasNextRun(1));
        assertTrue(dto.hasNextRun(2));
        assertFalse(dto.hasNextRun(3));
    }

    @Test
    void testInfiniteRuns() {
        MacroPlaybackSettingDTO dto = MacroPlaybackSettingDTO.infiniteRuns(50);
        assertTrue(dto.infinite());
        assertEquals(50, dto.intervalMillis());

        assertTrue(dto.hasNextRun(0));
        assertTrue(dto.hasNextRun(1000));
    }

    @Test
    void testValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                MacroPlaybackSettingDTO.limitedRuns(0, 10)
        );

        assertThrows(IllegalArgumentException.class, () ->
                MacroPlaybackSettingDTO.limitedRuns(5, -1)
        );
    }
}
