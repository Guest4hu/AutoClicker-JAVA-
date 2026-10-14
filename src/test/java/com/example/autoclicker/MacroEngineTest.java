package com.example.autoclicker;

import com.example.autoclicker.DTO.MacroPlaybackSettingDTO;
import com.example.autoclicker.DTO.MacroSettingDTO;
import com.example.autoclicker.domain.MacroActionSetting;
import com.example.autoclicker.domain.MacroEngine;
import com.example.autoclicker.enums.ActionType;
import com.example.autoclicker.input.simulator.VirtualInputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MacroEngineTest {

    private VirtualInputSimulator simulator;

    @BeforeEach
    void setUp() {
        simulator = new VirtualInputSimulator();
        MacroEngine.setSimulator(simulator);
    }

    @AfterEach
    void tearDown() {
        MacroEngine.stopMacro();
    }

    @Test
    void testSingleRunMacroExecution() throws Exception {
        MacroActionSetting mouseAction = new MacroActionSetting(ActionType.MOUSE, 1, 0, 100, 200);
        mouseAction.setKeyReleased(5);

        MacroActionSetting keyAction = new MacroActionSetting(ActionType.TECLADO, NativeKeyEvent.VC_A, 10);
        keyAction.setKeyReleased(15);

        MacroSettingDTO macro = new MacroSettingDTO(
                "TestMacro",
                "Description",
                LocalDateTime.now(),
                List.of(mouseAction, keyAction)
        );

        CountDownLatch latch = new CountDownLatch(1);
        MacroEngine.getInstance().setFinishListener(latch::countDown);

        MacroEngine.startMacro(macro);

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "A macro simples deve finalizar dentro do tempo limite");
        assertFalse(MacroEngine.isMacroRunning());

        assertEquals(1, simulator.getMousePressCount());
        assertEquals(1, simulator.getMouseReleaseCount());
        assertEquals(1, simulator.getKeyPressCount());
        assertEquals(1, simulator.getKeyReleaseCount());
        assertEquals(200, simulator.getCurrentX());
        assertEquals(100, simulator.getCurrentY());
    }

    @Test
    void testMultipleRepetitionsMacroExecution() throws Exception {
        MacroActionSetting mouseAction = new MacroActionSetting(ActionType.MOUSE, 1, 0, 50, 50);
        mouseAction.setKeyReleased(2);

        MacroSettingDTO macro = new MacroSettingDTO(
                "RepeatMacro",
                "Description",
                LocalDateTime.now(),
                List.of(mouseAction)
        );

        int targetRuns = 3;
        AtomicInteger iterationCount = new AtomicInteger(0);
        CountDownLatch finishLatch = new CountDownLatch(1);

        MacroEngine.getInstance().setIterationListener(iterationCount::set);
        MacroEngine.getInstance().setFinishListener(finishLatch::countDown);

        MacroPlaybackSettingDTO playback = MacroPlaybackSettingDTO.limitedRuns(targetRuns, 5);
        MacroEngine.startMacro(macro, playback);

        boolean finished = finishLatch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "A macro repetida deve finalizar dentro do tempo limite");
        assertFalse(MacroEngine.isMacroRunning());

        assertEquals(targetRuns, iterationCount.get());
        assertEquals(targetRuns, simulator.getMousePressCount());
        assertEquals(targetRuns, simulator.getMouseReleaseCount());
    }

    @Test
    void testMacroInterruptStopsGracefully() throws Exception {
        MacroActionSetting mouseAction = new MacroActionSetting(ActionType.MOUSE, 1, 0, 10, 10);
        mouseAction.setKeyReleased(10);

        MacroSettingDTO macro = new MacroSettingDTO(
                "InfiniteMacro",
                "Description",
                LocalDateTime.now(),
                List.of(mouseAction)
        );

        CountDownLatch firstRunLatch = new CountDownLatch(1);
        MacroEngine.getInstance().setIterationListener(run -> firstRunLatch.countDown());

        MacroPlaybackSettingDTO playback = MacroPlaybackSettingDTO.infiniteRuns(5);
        MacroEngine.startMacro(macro, playback);

        assertTrue(firstRunLatch.await(1, TimeUnit.SECONDS), "Primeira iteração deve começar");
        assertTrue(MacroEngine.isMacroRunning());

        MacroEngine.stopMacro();

        // Aguarda parar
        long deadline = System.currentTimeMillis() + 1000;
        while (MacroEngine.isMacroRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertFalse(MacroEngine.isMacroRunning(), "A macro deve parar após stopMacro");
    }

    @Test
    void testInvalidMacroThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new MacroSettingDTO("", "desc", LocalDateTime.now(), List.of())
        );

        assertThrows(IllegalArgumentException.class, () ->
                new MacroSettingDTO("Name", "desc", LocalDateTime.now(), null)
        );
    }
}
