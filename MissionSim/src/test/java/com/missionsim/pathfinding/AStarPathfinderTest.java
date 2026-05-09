package com.missionsim.pathfinding;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.map.Terrain;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests for the A* pathfinder — we check both that it finds paths when they exist
// and that it correctly returns null when no path is possible.
class AStarPathfinderTest {

    @Test
    void straightPath() {
        // A 1-row map of width 5: the only possible path is straight across.
        // Path from (0,0) to (4,0) should have exactly 5 tiles (including both endpoints).
        GameMap map = new GameMap(5, 1);
        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(new Position(0, 0), new Position(4, 0));
        assertNotNull(path);
        assertEquals(5, path.size()); // (0,0) (1,0) (2,0) (3,0) (4,0)
        assertEquals(new Position(0, 0), path.get(0));    // starts at origin
        assertEquals(new Position(4, 0), path.getLast()); // ends at goal
    }

    @Test
    void sameStartAndGoal() {
        // If we're already at the goal, we should get back a single-tile path (just our position)
        GameMap map = new GameMap(5, 5);
        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(new Position(2, 2), new Position(2, 2));
        assertNotNull(path);
        assertEquals(1, path.size()); // just the current tile
    }

    @Test
    void wallBlocksDirectRoute() {
        // Build a vertical wall at x=2 across the whole map, then open a gap at the bottom (y=4).
        // The path from (0,0) to (4,0) must go all the way down to y=4 to get through the gap.
        GameMap map = new GameMap(5, 5);
        // Vertical wall at x=2
        for (int y = 0; y < 5; y++) map.setTerrain(new Position(2, y), Terrain.WALL);
        // Leave a gap at y=4
        map.setTerrain(new Position(2, 4), Terrain.OPEN);

        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(new Position(0, 0), new Position(4, 0));
        assertNotNull(path, "Path should exist via the gap at y=4");
        // Double-check the path never steps through a wall tile
        for (Position p : path)
            assertNotEquals(Terrain.WALL, map.getTile(p).getTerrain(),
                "Path should not step through WALL at " + p);
    }

    @Test
    void noPathWhenFullyBlocked() {
        // Same vertical wall as above but this time we do NOT open any gap.
        // There should be no path from left side to right side.
        GameMap map = new GameMap(5, 5);
        for (int y = 0; y < 5; y++) map.setTerrain(new Position(2, y), Terrain.WALL);
        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(new Position(0, 0), new Position(4, 0));
        assertNull(path, "No path should exist when wall is complete");
    }

    @Test
    void prefersLowerCostTerrain() {
        // Top row is all DEBRIS (move cost 3). Middle and bottom rows are OPEN (cost 1).
        // A path from (0,1) to (4,1) should stay on the middle row and never touch debris.
        // This proves A* uses terrain cost, not just hop count.
        GameMap map = new GameMap(5, 3);
        for (int x = 0; x < 5; x++) map.setTerrain(new Position(x, 0), Terrain.DEBRIS);

        AStarPathfinder pf = new AStarPathfinder(map);
        List<Position> path = pf.findPath(new Position(0, 1), new Position(4, 1));
        assertNotNull(path);
        // All steps should stay on the middle row (OPEN), not the debris top row
        for (Position p : path) assertEquals(1, p.y, "Path should avoid debris row");
    }

    @Test
    void outOfBoundsGoalReturnsNull() {
        // Asking for a path to a tile outside the map should return null immediately
        GameMap map = new GameMap(5, 5);
        AStarPathfinder pf = new AStarPathfinder(map);
        assertNull(pf.findPath(new Position(0, 0), new Position(99, 99)));
    }

    @Test
    void impassableGoalReturnsNull() {
        // If the goal tile itself is a WALL, there's no point searching — return null right away
        GameMap map = new GameMap(5, 5);
        map.setTerrain(new Position(4, 4), Terrain.WALL);
        AStarPathfinder pf = new AStarPathfinder(map);
        assertNull(pf.findPath(new Position(0, 0), new Position(4, 4)));
    }
}
