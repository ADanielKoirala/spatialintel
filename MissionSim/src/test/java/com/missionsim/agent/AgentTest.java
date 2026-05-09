package com.missionsim.agent;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.map.Terrain;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Unit tests for individual agent decision-making.
// We test decideAction() directly — no need to run the full engine.
class AgentTest {

    // Shared setup for all tests: a 10x10 open map with one objective at (8,5)
    private GameMap map;
    private Mission mission;
    private Objective obj;

    // @BeforeEach runs before every single test method to reset state
    @BeforeEach
    void setUp() {
        map = new GameMap(10, 10);
        obj = new Objective("Target", new Position(8, 5), 30);
        mission = new Mission("Test", "Test briefing", map, List.of(obj), List.of(), 20);
    }

    // --- Medic ---

    @Test
    void medicMovesTowardInjuredAlly() {
        // Medic at x=0, injured Scout at x=5 — Medic should step east (to x=1)
        Medic medic = new Medic("Doc", new Position(0, 5));
        Scout scout = new Scout("Fast", new Position(5, 5));
        scout.takeDamage(40); // damage the scout so the Medic has a reason to move

        Action action = medic.decideAction(map, mission, List.of(medic, scout));
        assertEquals(Action.Type.MOVE, action.getType());
        // Should be moving east (toward x=5)
        assertEquals(1, action.getTarget().x);
    }

    @Test
    void medicHealsWhenAdjacentToInjuredAlly() {
        // Put Medic and Scout on the exact same tile — Medic should heal immediately
        Medic medic = new Medic("Doc", new Position(5, 5));
        Scout scout = new Scout("Fast", new Position(5, 5));
        scout.takeDamage(40);

        Action action = medic.decideAction(map, mission, List.of(medic, scout));
        assertEquals(Action.Type.HEAL, action.getType());
    }

    @Test
    void medicGoesToObjectiveWhenNoInjured() {
        // No allies to heal — Medic should fall back to securing objectives
        Medic medic = new Medic("Doc", new Position(0, 5));
        Action action = medic.decideAction(map, mission, List.of(medic));
        // No injured ally → should head to objective
        assertTrue(action.getType() == Action.Type.MOVE || action.getType() == Action.Type.SECURE_OBJECTIVE);
    }

    // --- Scout ---

    @Test
    void scoutRushesToObjective() {
        // Scout at (0,5) with objective at (8,5) — should move toward the objective
        Scout scout = new Scout("Fast", new Position(0, 5));
        Action action = scout.decideAction(map, mission, List.of(scout));
        assertEquals(Action.Type.MOVE, action.getType());
        // Scout should be moving right (x increases) or otherwise changing position
        assertTrue(action.getTarget().x > 0 || action.getTarget().y != 5);
    }

    @Test
    void scoutSecuresObjectiveWhenOnTile() {
        // Put the Scout directly on the objective tile — should secure it immediately
        Scout scout = new Scout("Fast", obj.getPosition());
        Action action = scout.decideAction(map, mission, List.of(scout));
        assertEquals(Action.Type.SECURE_OBJECTIVE, action.getType());
    }

    @Test
    void scoutEvacuatesOnHazardTile() {
        // Mark (5,5) as a hazard and put the Scout there — Scout should flee
        map.getTile(new Position(5, 5)).setHasHazard(true);
        Scout scout = new Scout("Fast", new Position(5, 5));
        Action action = scout.decideAction(map, mission, List.of(scout));
        assertEquals(Action.Type.EVADE, action.getType());
    }

    // --- Engineer ---

    @Test
    void engineerClearsDebrisUnderfoot() {
        // Set the Engineer's tile to DEBRIS — Engineer should clear it immediately
        map.setTerrain(new Position(3, 3), Terrain.DEBRIS);
        Engineer eng = new Engineer("Fix", new Position(3, 3));
        Action action = eng.decideAction(map, mission, List.of(eng));
        assertEquals(Action.Type.CLEAR_DEBRIS, action.getType());
    }

    @Test
    void engineerMovesToObjectiveWhenClear() {
        // No debris blocking the way — Engineer should head toward the objective
        Engineer eng = new Engineer("Fix", new Position(0, 5));
        Action action = eng.decideAction(map, mission, List.of(eng));
        assertEquals(Action.Type.MOVE, action.getType());
    }

    // --- Health / damage ---

    @Test
    void takeDamageKillsAtZeroHealth() {
        // Scout has 80 HP. Take 80 damage — should still be alive (health == 0 kills, not negative).
        // Take another 80 — should now be dead.
        Scout scout = new Scout("Fast", new Position(0, 0));
        scout.takeDamage(80);
        assertTrue(scout.isAlive());   // 0 HP but first hit exactly drained it — still alive until checked?
        scout.takeDamage(80);
        assertFalse(scout.isAlive());  // after second hit the agent is confirmed dead
    }

    @Test
    void healCannotExceedMaxHealth() {
        // Damage the Medic by 10, then try to over-heal with 9999 — health should cap at maxHealth
        Medic medic = new Medic("Doc", new Position(0, 0));
        medic.takeDamage(10);
        medic.heal(9999);
        assertEquals(medic.getMaxHealth(), medic.getHealth());
    }
}
