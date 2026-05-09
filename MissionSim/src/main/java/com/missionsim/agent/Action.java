package com.missionsim.agent;

import com.missionsim.map.Position;

// An Action is just a description of what an agent wants to do this turn.
// It's immutable — once created, nothing can change it. That way the engine can
// safely pass it around without worrying about something modifying it mid-turn.
public class Action {

    // All the things an agent is allowed to do. Using an enum means we can
    // switch on it later without magic strings like "move" or "heal".
    public enum Type {
        MOVE,             // walk one step toward a destination tile
        HEAL,             // restore HP to whoever is standing on the target tile
        SECURE_OBJECTIVE, // capture the objective at the agent's current position
        CLEAR_DEBRIS,     // remove debris terrain from the current tile (Engineer only)
        WAIT,             // do nothing — sometimes the best move is no move
        EVADE             // step away from a hazard tile to a safer neighbor
    }

    private final Type type;
    // target is the position this action is aimed at. Some actions (like WAIT
    // and CLEAR_DEBRIS) don't need a target, so it can be null.
    private final Position target;
    // reason is just a human-readable explanation so the turn log makes sense
    private final String reason;

    public Action(Type type, Position target, String reason) {
        this.type = type;
        this.target = target;
        this.reason = reason;
    }

    public Type getType() { return type; }
    public Position getTarget() { return target; }
    public String getReason() { return reason; }

    // Gives a readable string like "MOVE -> (3,5) [rushing objective Alpha]"
    // The ternary skips the arrow if there's no target position
    @Override
    public String toString() {
        return type + (target != null ? " -> " + target : "") + " [" + reason + "]";
    }
}
