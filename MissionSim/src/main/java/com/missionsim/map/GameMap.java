package com.missionsim.map;

import java.util.ArrayList;
import java.util.List;

// GameMap holds the 2D grid of tiles. Width and height are fixed after construction.
public class GameMap {
    private final int width;
    private final int height;
    // Note: the array is [height][width] — rows first, then columns.
    // So tiles[y][x] is correct. Mixing this up (tiles[x][y]) is a classic off-by-one bug.
    private final Tile[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        // Fill every tile with OPEN terrain so the map starts walkable everywhere
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                tiles[y][x] = new Tile(Terrain.OPEN);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Checks if a position is inside the grid before we try to access it
    public boolean inBounds(Position p) {
        return p.x >= 0 && p.x < width && p.y >= 0 && p.y < height;
    }

    // Always use this instead of accessing tiles[][] directly — it throws a clear error
    // if you go out of bounds instead of an ArrayIndexOutOfBoundsException
    public Tile getTile(Position p) {
        if (!inBounds(p)) throw new IllegalArgumentException("Out of bounds: " + p);
        return tiles[p.y][p.x]; // y is the row, x is the column
    }

    public void setTerrain(Position p, Terrain t) {
        getTile(p).setTerrain(t);
    }

    // Returns all neighbors that agents can actually walk onto (used by the A* pathfinder)
    public List<Position> getPassableNeighbors(Position p) {
        List<Position> result = new ArrayList<>();
        for (Position n : p.neighbors()) {
            if (inBounds(n) && getTile(n).getTerrain().isPassable())
                result.add(n);
        }
        return result;
    }

    // Prints a text version of the map to the console.
    // Each tile becomes a character (see Tile.toChar()), and living agents are shown as A, B, C...
    public void print(List<Position> agentPositions) {
        // Build a char grid first so we can stamp agent letters on top of the terrain
        char[][] display = new char[height][width];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                display[y][x] = tiles[y][x].toChar();

        // Overlay agent positions — first agent is 'A', second is 'B', etc.
        for (int i = 0; i < agentPositions.size(); i++) {
            Position p = agentPositions.get(i);
            if (inBounds(p)) display[p.y][p.x] = (char) ('A' + i);
        }

        // Print a column-number header (repeating "0123456789" as many times as needed)
        System.out.println("  " + "0123456789".repeat((width / 10) + 1).substring(0, width));
        for (int y = 0; y < height; y++) {
            System.out.printf("%2d %s%n", y, new String(display[y]));
        }
    }
}
