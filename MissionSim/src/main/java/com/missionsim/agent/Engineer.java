package com.missionsim.agent;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.map.Terrain;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;

import java.util.List;

/**
 * Engineer: clears debris to open paths, then assists with objectives.
 */
public class Engineer extends Agent {

    // 90 HP (a bit less than Medic's 100), priority 2 (same as Medic, less than Scout's 3)
    public Engineer(String name, Position startPos) {
        super(name, AgentRole.ENGINEER, startPos, 90, 2);
    }

    // The Engineer's decision logic, checked in priority order each turn
    @Override
    public Action decideAction(GameMap map, Mission mission, List<Agent> allAgents) {
        // 1. Self-preservation first — get off any hazard tile before doing anything else
        if (map.inBounds(position) && map.getTile(position).hasHazard()) {
            Position safe = safeNeighbor(map);
            if (safe != null)
                return new Action(Action.Type.EVADE, safe, "hazard on tile");
        }

        // 2. If we're standing on debris right now, clear it immediately
        if (map.getTile(position).getTerrain() == Terrain.DEBRIS)
            return new Action(Action.Type.CLEAR_DEBRIS, position, "clearing debris at " + position);

        // 3. Look ahead toward the nearest objective — if there's debris blocking the way, go clear it
        Objective obj = nearestObjective(mission);
        if (obj != null) {
            Position debrisStep = debrisOnPathTo(obj.getPosition(), map);
            if (debrisStep != null) {
                // Move toward the debris tile (we'll clear it once we arrive next turn)
                Position step = stepToward(debrisStep, map);
                return new Action(Action.Type.MOVE, step, "heading to clear debris blocking path");
            }
            // No debris in the way — head to objective
            if (obj.getPosition().equals(position))
                return new Action(Action.Type.SECURE_OBJECTIVE, position, "securing objective " + obj.getName());
            Position step = stepToward(obj.getPosition(), map);
            return new Action(Action.Type.MOVE, step, "moving to objective " + obj.getName());
        }

        return new Action(Action.Type.WAIT, position, "no tasks");
    }

    // Scans up to 5 tiles in the general direction of dest to find the first debris tile.
    // We use Integer.signum() to get -1, 0, or +1 for each axis — basically a unit vector.
    // This is a rough estimate, not an actual path trace, but good enough for planning.
    private Position debrisOnPathTo(Position dest, GameMap map) {
        int cx = position.x, cy = position.y;
        int dx = Integer.signum(dest.x - cx);
        int dy = Integer.signum(dest.y - cy);
        for (int step = 0; step < 5; step++) {
            cx += dx; cy += dy;
            Position p = new Position(cx, cy);
            if (!map.inBounds(p)) break;
            if (map.getTile(p).getTerrain() == Terrain.DEBRIS) return p;
        }
        return null;
    }

    // Returns any neighboring tile that is passable and has no hazard, or null if we're trapped
    private Position safeNeighbor(GameMap map) {
        for (Position n : position.neighbors()) {
            if (map.inBounds(n) && map.getTile(n).getTerrain().isPassable() && !map.getTile(n).hasHazard())
                return n;
        }
        return null;
    }
}
