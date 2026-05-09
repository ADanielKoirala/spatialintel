package com.missionsim.agent;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;

import java.util.List;

/**
 * Scout: fast mover focused on reaching and securing objectives; avoids hazards.
 */
public class Scout extends Agent {

    // 80 HP (lowest — Scout is fragile but fast), priority 3 (highest in the game)
    public Scout(String name, Position startPos) {
        super(name, AgentRole.SCOUT, startPos, 80, 3);
    }

    // Decision priority: evade hazard → retreat if critically hurt → rush objectives
    @Override
    public Action decideAction(GameMap map, Mission mission, List<Agent> allAgents) {
        // 1. Get off hazard tiles immediately
        if (map.inBounds(position) && map.getTile(position).hasHazard()) {
            Position safe = safeNeighbor(map);
            if (safe != null)
                return new Action(Action.Type.EVADE, safe, "hazard evasion");
        }

        // 2. If health drops below 25% (critically wounded), run to the nearest Medic.
        // Scouts are too squishy to fight through objectives at low HP.
        if (health < maxHealth * 0.25) {
            Agent medic = nearestMedic(allAgents);
            if (medic != null) {
                Position step = stepToward(medic.getPosition(), map);
                return new Action(Action.Type.MOVE, step, "retreating to medic");
            }
            // No medic alive — brave it out and keep going
        }

        // 3. Rush the nearest incomplete objective (this is the Scout's main job)
        Objective obj = nearestObjective(mission);
        if (obj != null) {
            if (obj.getPosition().equals(position))
                return new Action(Action.Type.SECURE_OBJECTIVE, position, "securing objective " + obj.getName());
            Position step = stepToward(obj.getPosition(), map);
            return new Action(Action.Type.MOVE, step, "rushing objective " + obj.getName());
        }

        return new Action(Action.Type.WAIT, position, "all objectives done");
    }

    // Find the closest living Medic on the team so the Scout can retreat to them
    private Agent nearestMedic(List<Agent> agents) {
        Agent nearest = null;
        int best = Integer.MAX_VALUE;
        for (Agent a : agents) {
            if (a.getRole() != AgentRole.MEDIC || !a.isAlive()) continue;
            int d = position.manhattanDistance(a.getPosition());
            if (d < best) { best = d; nearest = a; }
        }
        return nearest;
    }

    // Returns any passable, hazard-free neighboring tile to step onto
    private Position safeNeighbor(GameMap map) {
        for (Position n : position.neighbors()) {
            if (map.inBounds(n) && map.getTile(n).getTerrain().isPassable() && !map.getTile(n).hasHazard())
                return n;
        }
        return null;
    }
}
