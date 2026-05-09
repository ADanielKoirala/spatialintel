package com.missionsim.map;

// Each terrain type carries a movement cost used by the A* pathfinder.
// Higher cost means the agent "prefers" to avoid that terrain type when a cheaper path exists.
public enum Terrain {
    OPEN(1),              // normal ground — cheapest to move through
    DEBRIS(3),            // rubble — passable but slow (3x the cost of open ground)
    WATER(5),             // very slow to cross
    WALL(Integer.MAX_VALUE), // completely impassable — A* will never route through this
    HAZARD(2);            // treated as terrain but agents also take damage from the Tile's hasHazard flag

    /** Movement cost; MAX_VALUE means impassable. */
    public final int moveCost;

    Terrain(int moveCost) {
        this.moveCost = moveCost;
    }

    // A tile is passable if its move cost is anything other than MAX_VALUE.
    // Using MAX_VALUE as the sentinel for "impassable" is a common trick because
    // any arithmetic on it (like adding another cost) would overflow — but we check
    // isPassable() before doing any math, so it's safe.
    public boolean isPassable() {
        return moveCost != Integer.MAX_VALUE;
    }
}
