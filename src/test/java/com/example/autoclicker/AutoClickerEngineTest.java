package com.example.autoclicker;

import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.AutoClickerSetting;
import com.example.autoclicker.enums.ClickType;
import com.example.autoclicker.enums.MouseButton;
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

        AutoClickerEngine.setSimulator(simulator);
        AutoClickerEngine.setSetting(setting);
        AutoClickerEngine.setProgressListener(reportedClicks::set);
        AutoClickerEngine.getInstance().setFinishListener(latch::countDown);

        AutoClickerEngine.startAutoClicker();

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "O ciclo de cliques com compensação de deriva deve terminar no prazo");
        assertEquals(expectedClicks, AutoClickerEngine.getTotalClicks());
        assertEquals(expectedClicks, reportedClicks.get());
        assertEquals(expectedClicks, simulator.getMousePressCount());
        assertEquals(expectedClicks, simulator.getMouseReleaseCount());
        assertFalse(AutoClickerEngine.getInstance().isRunning());
    }
}
