package com.missionsim.agent;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;
import com.missionsim.pathfinding.AStarPathfinder;

import java.util.List;

// Agent is abstract because every real agent (Medic, Scout, Engineer) has different
// behavior. The shared stuff (health, movement, damage) lives here so we don't
// repeat it in each subclass.
public abstract class Agent {
    protected final String name;
    protected final AgentRole role;
    protected Position position;   // changes every turn as the agent moves
    protected int health;
    protected final int maxHealth; // set once at construction, never changes
    protected boolean alive;
    protected int priority;   // higher = more important to the mission

    protected Agent(String name, AgentRole role, Position startPos, int maxHealth, int priority) {
        this.name = name;
        this.role = role;
        this.position = startPos;
        this.maxHealth = maxHealth;
        this.health = maxHealth;  // start at full health
        this.alive = true;
        this.priority = priority;
    }

    // Each subclass must implement this — it's the "brain" of the agent.
    // It returns an Action but doesn't actually move anything; applyAction does that.
    public abstract Action decideAction(GameMap map, Mission mission, List<Agent> allAgents);

    /** Move one step along the shortest A* path toward dest. Returns the next position. */
    protected Position stepToward(Position dest, GameMap map) {
        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(position, dest);
        // path.get(0) is our current position, so get(1) is the first step forward
        if (path == null || path.size() < 2) return position; // already there or no path
        return path.get(1);
    }

    /** Nearest incomplete objective, or null if none remain. */
    protected Objective nearestObjective(Mission mission) {
        Objective nearest = null;
        int best = Integer.MAX_VALUE;
        for (Objective obj : mission.getObjectives()) {
            if (obj.isComplete()) continue; // skip already done objectives
            int d = position.manhattanDistance(obj.getPosition());
            if (d < best) { best = d; nearest = obj; }
        }
        return nearest;
    }

    /** Nearest injured ally (health < max), or null. */
    protected Agent nearestInjured(List<Agent> agents) {
        Agent nearest = null;
        int best = Integer.MAX_VALUE;
        for (Agent a : agents) {
            // skip self, dead agents, and fully healthy agents
            if (a == this || !a.alive || a.health >= a.maxHealth) continue;
            int d = position.manhattanDistance(a.position);
            if (d < best) { best = d; nearest = a; }
        }
        return nearest;
    }

    // This is where the action the agent decided on actually gets applied to game state.
    // It runs AFTER decideAction, once per agent per turn.
    public void applyAction(Action action, GameMap map, Mission mission, List<Agent> allAgents) {
        switch (action.getType()) {
            case MOVE -> position = action.getTarget();
            case HEAL -> {
                // find the agent standing on the target tile and heal them
                Agent target = agentAt(action.getTarget(), allAgents);
                if (target != null) target.heal(20);
            }
            case SECURE_OBJECTIVE -> {
                // mark the objective at our current position as done
                Objective obj = mission.objectiveAt(position);
                if (obj != null) obj.complete();
            }
            // clearing debris just turns that tile back to open ground
            case CLEAR_DEBRIS -> map.setTerrain(position, com.missionsim.map.Terrain.OPEN);
            case EVADE -> position = action.getTarget();
            case WAIT -> { /* do nothing */ }
        }

        // After every action, check if we ended up on a hazard tile and take damage.
        // This applies even after moving, so agents can't just casually walk through fire.
        if (map.inBounds(position) && map.getTile(position).hasHazard()) {
            takeDamage(10);
        }
    }

    // Helper to find an agent standing on a specific position
    private Agent agentAt(Position p, List<Agent> agents) {
        return agents.stream().filter(a -> a.position.equals(p)).findFirst().orElse(null);
    }

    public void takeDamage(int dmg) {
        // clamp to 0 so health never goes negative
        health = Math.max(0, health - dmg);
        if (health == 0) alive = false;
    }

    public void heal(int amount) {
        // clamp to maxHealth so we can't heal above the cap
        health = Math.min(maxHealth, health + amount);
        alive = true; // healing can bring someone back from 0 HP (the Medic revive mechanic)
    }

    // "low health" means below 40% — used by Medics to decide when to self-treat
    public boolean isLowHealth() { return health < maxHealth * 0.4; }

    public String getName() { return name; }
    public AgentRole getRole() { return role; }
    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public boolean isAlive() { return alive; }
    public int getPriority() { return priority; }

    // Readable summary like "[Doc/MEDIC hp:80/100 @(3,5)]" — handy for turn logs
    @Override
    public String toString() {
        return String.format("[%s/%s hp:%d/%d @%s]", name, role, health, maxHealth, position);
    }
}
