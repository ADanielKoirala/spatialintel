package com.missionsim.engine;

import com.missionsim.agent.Agent;
import com.missionsim.agent.Scout;
import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.mission.Mission;
import com.missionsim.mission.MissionResult;
import com.missionsim.mission.Objective;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Integration tests — these run the full engine loop and check the final result.
// verbose=false keeps the console clean while tests run.
class SimulationEngineTest {

    @Test
    void scoutCompletesNearbyObjective() {
        // Small 5x5 map. Scout starts at (0,2) and objective is at (3,2) — only 3 steps away.
        // With 20 turns the Scout should easily reach and secure it.
        GameMap map = new GameMap(5, 5);
        Objective obj = new Objective("Alpha", new Position(3, 2), 50);
        Mission mission = new Mission("Test", "brief", map, List.of(obj), List.of(), 20);

        Scout scout = new Scout("Fast", new Position(0, 2));
        SimulationEngine engine = new SimulationEngine(mission, List.of(scout), false);
        MissionResult result = engine.run();

        assertEquals(MissionResult.Outcome.SUCCESS, result.getOutcome());
        assertEquals(50, result.getPointsScored()); // all 50 points scored
    }

    @Test
    void missionFailsWhenAllAgentsDie() {
        // Cover the entire 5x5 map with hazards so the Scout takes 10 damage every turn.
        // We pre-drain the Scout to 10 HP (100 - 70 damage), so one hazard tick kills them.
        GameMap map = new GameMap(5, 5);
        // Surround start with hazards
        for (int x = 0; x < 5; x++)
            for (int y = 0; y < 5; y++)
                map.getTile(new Position(x, y)).setHasHazard(true);

        Objective obj = new Objective("Unreachable", new Position(4, 4), 50);
        Mission mission = new Mission("Death Test", "brief", map, List.of(obj), List.of(), 20);

        Scout scout = new Scout("Doomed", new Position(2, 2));
        // Drain health manually so hazard ticks kill quickly
        scout.takeDamage(70); // Scout now has 10 HP out of 80 — one tick away from death
        SimulationEngine engine = new SimulationEngine(mission, List.of(scout), false);
        MissionResult result = engine.run();

        // Outcome should be PARTIAL or FAILURE (either way, NOT a success)
        assertNotEquals(MissionResult.Outcome.SUCCESS, result.getOutcome());
    }

    @Test
    void missionTimesOutWithPartialScore() {
        // Two objectives: one close, one impossibly far. Turn limit is only 4.
        // The Scout should reach the near one but not the far one → PARTIAL outcome.
        GameMap map = new GameMap(20, 20);
        // Objectives far away — can't all be reached in 2 turns
        Objective obj1 = new Objective("Near",  new Position(2,  0), 20);
        Objective obj2 = new Objective("Far",   new Position(19, 19), 80);
        Mission mission = new Mission("Timeout Test", "brief", map,
            List.of(obj1, obj2), List.of(), 4); // only 4 turns!

        Scout scout = new Scout("Fast", new Position(0, 0));
        SimulationEngine engine = new SimulationEngine(mission, List.of(scout), false);
        MissionResult result = engine.run();

        // Near obj should be reachable; far one won't be
        assertEquals(MissionResult.Outcome.PARTIAL, result.getOutcome());
        assertEquals(20, result.getPointsScored()); // only the near objective's 20 points
    }

    @Test
    void turnLogIsPopulated() {
        // Even a trivial 3-tile mission should produce some log entries.
        // This makes sure the engine is actually recording events, not just running silently.
        GameMap map = new GameMap(3, 1); // a single-row 3-wide map
        Objective obj = new Objective("End", new Position(2, 0), 10);
        Mission mission = new Mission("Log Test", "brief", map, List.of(obj), List.of(), 10);

        Scout scout = new Scout("Fast", new Position(0, 0));
        SimulationEngine engine = new SimulationEngine(mission, List.of(scout), false);
        engine.run();

        assertFalse(engine.getTurnLog().getEntries().isEmpty()); // at least one entry was logged
    }
}
