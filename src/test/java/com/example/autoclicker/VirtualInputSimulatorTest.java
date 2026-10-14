package com.example.autoclicker;

import com.example.autoclicker.input.simulator.EventType;
import com.example.autoclicker.input.simulator.VirtualInputSimulator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VirtualInputSimulatorTest {

    @Test
    void testEventRecordingAndCounters() {
        VirtualInputSimulator simulator = new VirtualInputSimulator();

        assertEquals(0, simulator.getMousePressCount());
        assertEquals(0, simulator.getMouseReleaseCount());
        assertEquals(0, simulator.getKeyPressCount());
        assertEquals(0, simulator.getKeyReleaseCount());
        assertEquals(0, simulator.getCurrentX());
        assertEquals(0, simulator.getCurrentY());

        simulator.mouseMove(100, 200);
        assertEquals(100, simulator.getCurrentX());
        assertEquals(200, simulator.getCurrentY());

        simulator.mousePress(16);
        simulator.mouseRelease(16);
        assertEquals(1, simulator.getMousePressCount());
        assertEquals(1, simulator.getMouseReleaseCount());

        simulator.keyPress(65);
        simulator.keyRelease(65);
        assertEquals(1, simulator.getKeyPressCount());
        assertEquals(1, simulator.getKeyReleaseCount());

        List<VirtualInputSimulator.InputEventRecord> events = simulator.getEvents();
        assertEquals(5, events.size());
        assertEquals(EventType.MOUSE_MOVE, events.get(0).type());
        assertEquals(EventType.MOUSE_PRESS, events.get(1).type());
        assertEquals(EventType.MOUSE_RELEASE, events.get(2).type());
        assertEquals(EventType.KEY_PRESS, events.get(3).type());
        assertEquals(EventType.KEY_RELEASE, events.get(4).type());

        simulator.clear();
        assertEquals(0, simulator.getEvents().size());
        assertEquals(0, simulator.getMousePressCount());
        assertEquals(0, simulator.getMouseReleaseCount());
        assertEquals(0, simulator.getKeyPressCount());
        assertEquals(0, simulator.getKeyReleaseCount());
    }
}
