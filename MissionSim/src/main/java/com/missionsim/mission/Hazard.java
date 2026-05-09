package com.missionsim.mission;

import com.missionsim.map.Position;

// Represents a dangerous zone on the map that hurts agents each turn they stand in it.
public class Hazard {
    // The different kinds of hazards — right now they all behave the same (damage per turn),
    // but having a type means we could add special effects later (e.g. fire spreads, minefields explode once)
    public enum HazardType { FIRE, TOXIC, MINEFIELD, UNSTABLE_STRUCTURE }

    private final String name;
    private final Position position;
    private final HazardType type;
    private final int damagePerTurn; // how much HP an agent loses each turn they stay on this tile
    private boolean active;          // could be set to false if the hazard is cleared/extinguished

    public Hazard(String name, Position position, HazardType type, int damagePerTurn) {
        this.name = name;
        this.position = position;
        this.type = type;
        this.damagePerTurn = damagePerTurn;
        this.active = true; // all hazards start active
    }

    public String getName() { return name; }
    public Position getPosition() { return position; }
    public HazardType getType() { return type; }
    public int getDamagePerTurn() { return damagePerTurn; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }

    @Override
    public String toString() {
        return name + "(" + type + ")@" + position + " dmg:" + damagePerTurn;
    }
}
