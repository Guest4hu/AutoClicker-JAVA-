package com.example.autoclicker;

import com.example.autoclicker.domain.engine.MacroEngine;
import com.example.autoclicker.domain.model.ActionType;
import com.example.autoclicker.domain.model.MacroActionSetting;
import com.example.autoclicker.dto.MacroPlaybackSettingDTO;
import com.example.autoclicker.dto.MacroSettingDTO;
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
    private MacroEngine engine;

    @BeforeEach
    void setUp() {
        simulator = new VirtualInputSimulator();
        engine = new MacroEngine(simulator);
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.stopMacro();
        }
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
        engine.setFinishListener(latch::countDown);

        engine.startMacro(macro);

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "A macro simples deve finalizar dentro do tempo limite");
        assertFalse(engine.isMacroRunning());

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

        engine.setIterationListener(iterationCount::set);
        engine.setFinishListener(finishLatch::countDown);

        MacroPlaybackSettingDTO playback = MacroPlaybackSettingDTO.limitedRuns(targetRuns, 5);
        engine.startMacro(macro, playback);

        boolean finished = finishLatch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "A macro repetida deve finalizar dentro do tempo limite");
        assertFalse(engine.isMacroRunning());

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
        engine.setIterationListener(run -> firstRunLatch.countDown());

        MacroPlaybackSettingDTO playback = MacroPlaybackSettingDTO.infiniteRuns(5);
        engine.startMacro(macro, playback);

        assertTrue(firstRunLatch.await(1, TimeUnit.SECONDS), "Primeira iteração deve começar");
        assertTrue(engine.isMacroRunning());

        engine.stopMacro();

        long deadline = System.currentTimeMillis() + 1000;
        while (engine.isMacroRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertFalse(engine.isMacroRunning(), "A macro deve parar após stopMacro");
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

    @Test
    void testMouseDragMacroExecution() throws Exception {
        MacroActionSetting dragAction = new MacroActionSetting(ActionType.MOUSE_ARRASTADO, 1, 0, 10, 10);
        dragAction.setXAfter(100);
        dragAction.setYAfter(100);
        dragAction.setKeyReleased(20);

        MacroSettingDTO macro = new MacroSettingDTO(
                "DragMacro",
                "Description",
                LocalDateTime.now(),
                List.of(dragAction)
        );

        CountDownLatch latch = new CountDownLatch(1);
        engine.setFinishListener(latch::countDown);

        engine.startMacro(macro);

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "A macro de arrastar deve finalizar");
        assertEquals(1, simulator.getMousePressCount());
        assertEquals(1, simulator.getMouseReleaseCount());
        assertEquals(100, simulator.getCurrentX());
        assertEquals(100, simulator.getCurrentY());
    }
}
