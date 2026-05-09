package com.missionsim.mission;

import com.missionsim.map.GameMap;
import com.missionsim.map.Position;

import java.util.Collections;
import java.util.List;

// Mission bundles together everything needed for one simulation run:
// the map, the list of objectives to complete, hazards to avoid, and a turn limit.
public class Mission {
    private final String name;
    private final String briefing;
    private final GameMap map;
    private final List<Objective> objectives;
    private final List<Hazard> hazards;
    private final int maxTurns;

    public Mission(String name, String briefing, GameMap map,
                   List<Objective> objectives, List<Hazard> hazards, int maxTurns) {
        this.name = name;
        this.briefing = briefing;
        this.map = map;
        this.objectives = objectives;
        this.hazards = hazards;
        this.maxTurns = maxTurns;

        // Tell the map tiles which ones hold objectives and hazards so the printer
        // knows to draw 'O' and '!' on those spots
        for (Objective obj : objectives) {
            if (map.inBounds(obj.getPosition())) {
                map.getTile(obj.getPosition()).setHasObjective(true);
                map.getTile(obj.getPosition()).setLabel(obj.getName());
            }
        }
        for (Hazard h : hazards) {
            if (map.inBounds(h.getPosition()) && h.isActive()) {
                map.getTile(h.getPosition()).setHasHazard(true);
            }
        }
    }

    // Finds the first incomplete objective at position p — returns null if none there
    public Objective objectiveAt(Position p) {
        return objectives.stream()
            .filter(o -> !o.isComplete() && o.getPosition().equals(p))
            .findFirst().orElse(null);
    }

    // True only if every single objective in the list is marked complete
    public boolean allObjectivesComplete() {
        return objectives.stream().allMatch(Objective::isComplete);
    }

    // Sums up the points from objectives that have been completed so far
    public int completedObjectivePoints() {
        return objectives.stream()
            .filter(Objective::isComplete)
            .mapToInt(Objective::getMissionPoints)
            .sum();
    }

    // Total possible points if all objectives were completed
    public int totalObjectivePoints() {
        return objectives.stream().mapToInt(Objective::getMissionPoints).sum();
    }

    public String getName() { return name; }
    public String getBriefing() { return briefing; }
    public GameMap getMap() { return map; }
    // Returning unmodifiable views means outside code can't accidentally add/remove objectives
    public List<Objective> getObjectives() { return Collections.unmodifiableList(objectives); }
    public List<Hazard> getHazards() { return Collections.unmodifiableList(hazards); }
    public int getMaxTurns() { return maxTurns; }
}
