package com.missionsim.map;

import java.util.Objects;

// Immutable (x, y) coordinate. Declared final so nothing can subclass it and
// accidentally break the equality contract.
public final class Position {
    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Manhattan distance = |dx| + |dy|. On a grid with no diagonal movement
    // this is the exact number of steps between two points (ignoring walls).
    // It's also used as the heuristic in A* because it never overestimates.
    public int manhattanDistance(Position other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    // Only 4 neighbors (up, down, left, right) — no diagonal movement in this game.
    // Some neighbors may be out of bounds; callers should check inBounds() before using them.
    public Position[] neighbors() {
        return new Position[]{
            new Position(x, y - 1), // up
            new Position(x, y + 1), // down
            new Position(x - 1, y), // left
            new Position(x + 1, y)  // right
        };
    }

    // We override equals + hashCode because Position is used as a key in HashMaps
    // (in the A* gScore and cameFrom maps). Without these, two Position(3,5) objects
    // would be treated as different keys even though they represent the same tile.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Position p)) return false;
        return x == p.x && y == p.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
