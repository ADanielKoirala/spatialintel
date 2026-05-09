package com.missionsim.pathfinding;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;

import java.util.*;

/**
 * A* pathfinder using Manhattan distance heuristic.
 * Terrain move-cost is the edge weight; WALL tiles are impassable.
 */
public class AStarPathfinder {

    private final GameMap map;

    public AStarPathfinder(GameMap map) {
        this.map = map;
    }

    /**
     * Returns the shortest path from start to goal (inclusive of both endpoints),
     * or null if no path exists.
     */
    public List<Position> findPath(Position start, Position goal) {
        // Quick sanity checks before doing any work
        if (!map.inBounds(start) || !map.inBounds(goal)) return null;
        if (!map.getTile(goal).getTerrain().isPassable()) return null; // goal is a wall
        if (start.equals(goal)) return List.of(start); // already there

        // gScore[p] = total movement cost from start to p along the best known path
        Map<Position, Integer> gScore = new HashMap<>();
        // cameFrom[p] = the tile we came from to reach p — used to reconstruct the path at the end
        Map<Position, Position> cameFrom = new HashMap<>();
        // The open set: positions to explore next, ordered by f = g + h
        // f(p) = gScore(p) + manhattan distance to goal (the heuristic)
        PriorityQueue<Position> open = new PriorityQueue<>(
            Comparator.comparingInt(p -> gScore.getOrDefault(p, Integer.MAX_VALUE) + p.manhattanDistance(goal))
        );

        gScore.put(start, 0);
        open.add(start);

        while (!open.isEmpty()) {
            // Always process the most promising tile first (lowest f score)
            Position current = open.poll();

            if (current.equals(goal))
                return reconstructPath(cameFrom, goal); // found it — trace back the path

            for (Position neighbor : map.getPassableNeighbors(current)) {
                // tentative g = cost to reach current + cost to step onto neighbor
                int tentative = gScore.getOrDefault(current, Integer.MAX_VALUE)
                    + map.getTile(neighbor).getTerrain().moveCost;
                if (tentative < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    // Found a cheaper route to this neighbor — update and re-queue it
                    gScore.put(neighbor, tentative);
                    cameFrom.put(neighbor, current);
                    // PriorityQueue doesn't auto-update priority, so we remove and re-add
                    open.remove(neighbor);
                    open.add(neighbor);
                }
            }
        }

        return null; // exhausted all reachable tiles and never found the goal
    }

    // Walks backwards through the cameFrom map from goal to start,
    // building the path in reverse, then returns it front-to-back
    private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position goal) {
        LinkedList<Position> path = new LinkedList<>();
        Position current = goal;
        while (current != null) {
            path.addFirst(current); // prepend so we end up with start→goal order
            current = cameFrom.get(current); // step back toward start
        }
        return path;
    }
}
