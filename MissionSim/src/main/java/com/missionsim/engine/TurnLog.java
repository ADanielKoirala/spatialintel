package com.missionsim.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Keeps a running list of everything that happened during the simulation.
// Each entry is just a formatted string — simple but easy to print later.
public class TurnLog {
    private final List<String> entries = new ArrayList<>();

    // Logs an agent's action for a given turn.
    // %02d pads the turn number to at least 2 digits so "T01" lines up with "T10".
    // %-10s left-aligns the name in a 10-character column so all entries line up nicely.
    public void log(int turn, String agentName, String action) {
        entries.add(String.format("T%02d | %-10s | %s", turn, agentName, action));
    }

    // For system-level events (mission start, mission complete) that don't belong to any agent
    public void logEvent(int turn, String event) {
        entries.add(String.format("T%02d | %-10s | %s", turn, "SYSTEM", event));
    }

    // Returns an unmodifiable view so callers can't accidentally clear or add to the log
    public List<String> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void printAll() {
        entries.forEach(System.out::println);
    }
}
