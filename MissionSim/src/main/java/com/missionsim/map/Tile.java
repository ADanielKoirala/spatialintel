package com.missionsim.map;

// A Tile is one cell on the map. It knows its terrain type and any special flags
// (whether there's an objective or hazard here, and whether the objective is done).
public class Tile {
    private Terrain terrain;
    private boolean hasObjective;      // true if an objective is placed on this tile
    private boolean hasHazard;         // true if a hazard (fire, toxic, etc.) is here
    private boolean objectiveComplete; // changes to true once an agent secures it
    private String label;              // optional display name for the objective

    public Tile(Terrain terrain) {
        this.terrain = terrain;
        // all booleans default to false in Java, so no need to initialize them
    }

    public Terrain getTerrain() { return terrain; }
    public void setTerrain(Terrain terrain) { this.terrain = terrain; }

    public boolean hasObjective() { return hasObjective; }
    public void setHasObjective(boolean hasObjective) { this.hasObjective = hasObjective; }

    public boolean isObjectiveComplete() { return objectiveComplete; }
    public void setObjectiveComplete(boolean objectiveComplete) { this.objectiveComplete = objectiveComplete; }

    public boolean hasHazard() { return hasHazard; }
    public void setHasHazard(boolean hasHazard) { this.hasHazard = hasHazard; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    // Returns a single character representing this tile for the ASCII map printout.
    // The order of checks matters — hazard shows '!' even on debris, for example.
    public char toChar() {
        if (!terrain.isPassable()) return '#';    // wall — blocked
        if (hasHazard) return '!';                // danger zone
        if (hasObjective && !objectiveComplete) return 'O'; // pending objective
        if (hasObjective) return 'o';             // completed objective (lowercase = done)
        return switch (terrain) {
            case WATER -> '~';
            case DEBRIS -> '%';
            default -> '.';   // normal open ground
        };
    }
}
