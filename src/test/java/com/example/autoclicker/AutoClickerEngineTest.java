package com.example.autoclicker;

import com.example.autoclicker.domain.engine.AutoClickerEngine;
import com.example.autoclicker.domain.model.AutoClickerSetting;
import com.example.autoclicker.domain.model.ClickType;
import com.example.autoclicker.domain.model.MouseButton;
import com.example.autoclicker.input.simulator.VirtualInputSimulator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AutoClickerEngineTest {

    @Test
    void testLimitedClicksExecutionWithDriftCompensation() throws Exception {
        int expectedClicks = 5;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger reportedClicks = new AtomicInteger(0);
        VirtualInputSimulator simulator = new VirtualInputSimulator();

        AutoClickerSetting setting = AutoClickerSetting.limitedClicks(
                10, // 10ms por clique
                ClickType.SIMPLES,
                MouseButton.ESQUERDO,
                0,
                expectedClicks
        );

        AutoClickerEngine engine = new AutoClickerEngine(simulator);
        engine.setSetting(setting);
        engine.setProgressListener(reportedClicks::set);
        engine.setFinishListener(latch::countDown);

        engine.startAutoClicker();

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "O ciclo de cliques com compensação de deriva deve terminar no prazo");
        assertEquals(expectedClicks, engine.getTotalClicks());
        assertEquals(expectedClicks, reportedClicks.get());
        assertEquals(expectedClicks, simulator.getMousePressCount());
        assertEquals(expectedClicks, simulator.getMouseReleaseCount());
        assertFalse(engine.isRunning());
    }

    @Test
    void testDoubleClickMultipliesPresses() throws Exception {
        int expectedActivations = 3;
        CountDownLatch latch = new CountDownLatch(1);
        VirtualInputSimulator simulator = new VirtualInputSimulator();

        AutoClickerSetting setting = AutoClickerSetting.limitedClicks(
                10,
                ClickType.DUPLO,
                MouseButton.ESQUERDO,
                0,
                expectedActivations
        );

        AutoClickerEngine engine = new AutoClickerEngine(simulator);
        engine.setSetting(setting);
        engine.setFinishListener(latch::countDown);

        engine.startAutoClicker();

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished);
        assertEquals(expectedActivations, engine.getTotalClicks());
        assertEquals(expectedActivations * 2, simulator.getMousePressCount());
        assertEquals(expectedActivations * 2, simulator.getMouseReleaseCount());
    }

    @Test
    void testStopAutoClickerTerminatesGracefully() throws Exception {
        VirtualInputSimulator simulator = new VirtualInputSimulator();
        AutoClickerSetting setting = AutoClickerSetting.infiniteClicks(
                10,
                ClickType.SIMPLES,
                MouseButton.ESQUERDO,
                0
        );

        AutoClickerEngine engine = new AutoClickerEngine(simulator);
        engine.setSetting(setting);

        CountDownLatch progressLatch = new CountDownLatch(3);
        engine.setProgressListener(clicks -> progressLatch.countDown());

        engine.startAutoClicker();
        assertTrue(progressLatch.await(1, TimeUnit.SECONDS), "Deve disparar ao menos 3 cliques");
        assertTrue(engine.isRunning());

        engine.stopAutoClicker();

        long deadline = System.currentTimeMillis() + 1000;
        while (engine.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }

        assertFalse(engine.isRunning(), "O motor deve ser interrompido após stopAutoClicker");
    }

    @Test
    void testStartWithoutSettingsThrowsException() {
        AutoClickerEngine engine = new AutoClickerEngine(new VirtualInputSimulator());
        assertThrows(IllegalStateException.class, engine::startAutoClicker);
    }
}
