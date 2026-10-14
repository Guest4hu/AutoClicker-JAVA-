package com.example.autoclicker;

import com.example.autoclicker.domain.model.ActionType;
import com.example.autoclicker.domain.model.MacroActionSetting;
import com.example.autoclicker.input.macro.MacroService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class MacroServiceTest {

    @Test
    void testInitialState() {
        MacroService service = new MacroService();
        assertFalse(service.isCapturing());
        assertTrue(service.getMacroActions().isEmpty());
    }

    @Test
    void testSetAndGetMacroActions() {
        MacroService service = new MacroService();
        MacroActionSetting action = new MacroActionSetting(ActionType.MOUSE, 1, 100, 10, 20);
        service.setMacroActions(List.of(action));

        List<MacroActionSetting> retrieved = service.getMacroActions();
        assertEquals(1, retrieved.size());
        assertEquals(action.getKey(), retrieved.get(0).getKey());

        // Modifying returned list must not affect service internal state
        retrieved.clear();
        assertEquals(1, service.getMacroActions().size());
    }

    @Test
    void testConcurrentAccessToMacroActions() throws InterruptedException, ExecutionException {
        MacroService service = new MacroService();
        int threadCount = 8;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            tasks.add(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    if (threadId % 2 == 0) {
                        service.setMacroActions(List.of(
                                new MacroActionSetting(ActionType.MOUSE, threadId, j, 0, 0)
                        ));
                    } else {
                        service.getMacroActions();
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        for (Future<Void> future : futures) {
            future.get(); // Will throw if any exception occurred during concurrency
        }
    }
}
