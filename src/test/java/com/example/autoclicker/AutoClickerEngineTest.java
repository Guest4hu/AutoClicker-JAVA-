package com.example.autoclicker;

import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.AutoClickerSetting;
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

        AutoClickerSetting setting = AutoClickerSetting.limitedClicks(
                10, // 10ms por clique
                AutoClickerSetting.ClickType.SIMPLES,
                AutoClickerSetting.MouseButton.ESQUERDO,
                0,
                expectedClicks
        );

        AutoClickerEngine.setSetting(setting);
        AutoClickerEngine.setProgressListener(reportedClicks::set);
        AutoClickerEngine.setFinishListener(latch::countDown);

        AutoClickerEngine.startAutoClicker();

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "O ciclo de cliques com compensação de deriva deve terminar no prazo");
        assertEquals(expectedClicks, AutoClickerEngine.getTotalClicks());
        assertEquals(expectedClicks, reportedClicks.get());
        assertFalse(AutoClickerEngine.isRunning());
    }
}
