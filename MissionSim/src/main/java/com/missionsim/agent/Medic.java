package com.missionsim.agent;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;

import java.util.List;

/**
 * Medic: prioritises healing injured allies; falls back to securing objectives.
 */
public class Medic extends Agent {

    // 100 HP (highest of all roles), priority 2
    public Medic(String name, Position startPos) {
        super(name, AgentRole.MEDIC, startPos, 100, 2);
    }

    // Decision priority: evade hazard → heal allies → self-heal → secure objectives
    @Override
    public Action decideAction(GameMap map, Mission mission, List<Agent> allAgents) {
        // 1. Self-preservation first — a dead medic can't heal anyone
        if (map.inBounds(position) && map.getTile(position).hasHazard()) {
            Position safe = safeNeighbor(map);
            if (safe != null)
                return new Action(Action.Type.EVADE, safe, "hazard on current tile");
        }

        // 2. Look for the nearest hurt teammate
        Agent injured = nearestInjured(allAgents);
        if (injured != null) {
            // If we're on the same tile, heal immediately; otherwise walk toward them
            if (injured.getPosition().equals(position))
                return new Action(Action.Type.HEAL, position, "ally needs healing");
            Position step = stepToward(injured.getPosition(), map);
            return new Action(Action.Type.MOVE, step, "moving to heal " + injured.getName());
        }

        // 3. If no allies need healing but we're hurt ourselves, do a slow self-patch.
        // Medics apply 10 HP per turn to themselves — slower than the 20 HP they give allies.
        if (isLowHealth()) {
            heal(10);
            return new Action(Action.Type.WAIT, position, "self-treating wounds");
        }

        // 4. Everyone is healthy — help out with objectives
        Objective obj = nearestObjective(mission);
        if (obj != null) {
            if (obj.getPosition().equals(position))
                return new Action(Action.Type.SECURE_OBJECTIVE, position, "securing objective");
            Position step = stepToward(obj.getPosition(), map);
            return new Action(Action.Type.MOVE, step, "heading to objective " + obj.getName());
        }

        return new Action(Action.Type.WAIT, position, "no tasks remaining");
    }

    // Returns any safe neighbor tile to flee to, or null if surrounded
    private Position safeNeighbor(GameMap map) {
        for (Position n : position.neighbors()) {
            if (map.inBounds(n) && map.getTile(n).getTerrain().isPassable() && !map.getTile(n).hasHazard())
                return n;
        }
        return null;
    }
}
