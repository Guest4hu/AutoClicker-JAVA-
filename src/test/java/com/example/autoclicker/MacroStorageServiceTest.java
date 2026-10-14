package com.example.autoclicker;

import com.example.autoclicker.domain.model.ActionType;
import com.example.autoclicker.domain.model.MacroActionSetting;
import com.example.autoclicker.dto.MacroSettingDTO;
import com.example.autoclicker.storage.MacroStorageService;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MacroStorageServiceTest {

    @TempDir
    Path tempDir;

    private MacroStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new MacroStorageService(tempDir);
    }

    @Test
    void testSaveAndLoadMacro() throws IOException {
        MacroActionSetting action1 = new MacroActionSetting(ActionType.MOUSE, 1, 0, 10, 20);
        action1.setKeyReleased(50);
        action1.setXAfter(25);
        action1.setYAfter(15);

        MacroActionSetting action2 = new MacroActionSetting(ActionType.TECLADO, NativeKeyEvent.VC_ENTER, 100);
        action2.setKeyReleased(150);

        LocalDateTime now = LocalDateTime.now();
        MacroSettingDTO original = new MacroSettingDTO(
                "MyMacro",
                "Test description",
                now,
                List.of(action1, action2)
        );

        storageService.save(original);

        assertTrue(storageService.existeTalArquivo("mymacro"));
        assertTrue(storageService.existeTalArquivo("mymacro.json"));

        MacroSettingDTO loaded = storageService.load("mymacro");
        assertNotNull(loaded);
        assertEquals("MyMacro", loaded.name());
        assertEquals("Test description", loaded.description());
        assertEquals(2, loaded.configs().size());

        MacroActionSetting loadedAction1 = loaded.configs().get(0);
        assertEquals(ActionType.MOUSE, loadedAction1.getActionType());
        assertEquals(1, loadedAction1.getKey());
        assertEquals(0, loadedAction1.getKeyPressed());
        assertEquals(50, loadedAction1.getKeyReleased());
        assertEquals(20, loadedAction1.getXBefore());
        assertEquals(10, loadedAction1.getYBefore());
        assertEquals(25, loadedAction1.getXAfter());
        assertEquals(15, loadedAction1.getYAfter());
    }

    @Test
    void testSaveDuplicateThrowsException() throws IOException {
        MacroActionSetting action = new MacroActionSetting(ActionType.MOUSE, 1, 0, 0, 0);
        MacroSettingDTO macro = new MacroSettingDTO("Duplicate", "Desc", LocalDateTime.now(), List.of(action));

        storageService.save(macro);

        assertThrows(FileSystemException.class, () -> storageService.save(macro));
    }

    @Test
    void testListAllSettings() throws IOException {
        assertTrue(storageService.listAllSettings().isEmpty());

        MacroActionSetting action = new MacroActionSetting(ActionType.MOUSE, 1, 0, 0, 0);
        storageService.save(new MacroSettingDTO("Alpha", "Desc", LocalDateTime.now(), List.of(action)));
        storageService.save(new MacroSettingDTO("Beta", "Desc", LocalDateTime.now(), List.of(action)));

        List<String> settings = storageService.listAllSettings();
        assertEquals(2, settings.size());
        assertEquals(List.of("alpha", "beta"), settings);
    }

    @Test
    void testLoadExistingPreRefactorJsonFiles() throws IOException {
        MacroStorageService realStorage = new MacroStorageService();

        if (realStorage.existeTalArquivo("dadada")) {
            MacroSettingDTO dto = realStorage.load("dadada");
            assertNotNull(dto);
            assertEquals("dadada", dto.name());
            assertFalse(dto.configs().isEmpty());
            MacroActionSetting first = dto.configs().get(0);
            assertEquals(1080, first.getXBefore());
            assertEquals(233, first.getYBefore());
        }

        if (realStorage.existeTalArquivo("laele")) {
            MacroSettingDTO dto = realStorage.load("laele");
            assertNotNull(dto);
            assertEquals("Laele", dto.name());
            assertFalse(dto.configs().isEmpty());
            MacroActionSetting first = dto.configs().get(0);
            assertEquals(545, first.getXBefore());
            assertEquals(954, first.getYBefore());
        }

        if (realStorage.existeTalArquivo("testemacrooff")) {
            MacroSettingDTO dto = realStorage.load("testemacrooff");
            assertNotNull(dto);
            assertEquals("TEsteMacroOff", dto.name());
            assertFalse(dto.configs().isEmpty());
            MacroActionSetting first = dto.configs().get(0);
            assertEquals(1044, first.getXBefore());
            assertEquals(217, first.getYBefore());
        }
    }
}
