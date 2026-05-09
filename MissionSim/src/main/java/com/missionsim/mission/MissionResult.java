package com.missionsim.mission;

import com.missionsim.agent.Agent;

import java.util.List;

// Holds everything we know about how the mission ended — outcome, score, survivors, full log.
public class MissionResult {

    // Three possible endings:
    // SUCCESS  = all objectives done
    // PARTIAL  = some objectives done but not all (usually a timeout)
    // FAILURE  = no objectives completed (or everyone died)
    public enum Outcome { SUCCESS, PARTIAL, FAILURE }

    private final Outcome outcome;
    private final int turnsElapsed;
    private final int pointsScored;
    private final int totalPoints;
    private final List<Agent> survivors; // agents still alive at the end
    private final List<String> log;      // turn-by-turn event strings from TurnLog

    public MissionResult(Outcome outcome, int turnsElapsed, int pointsScored,
                         int totalPoints, List<Agent> survivors, List<String> log) {
        this.outcome = outcome;
        this.turnsElapsed = turnsElapsed;
        this.pointsScored = pointsScored;
        this.totalPoints = totalPoints;
        this.survivors = survivors;
        this.log = log;
    }

    // Prints the full after-action report — outcome, score, survivors, then the whole turn log
    public void printReport() {
        System.out.println("\n========================================");
        System.out.println("         MISSION AFTER-ACTION REPORT");
        System.out.println("========================================");
        System.out.printf("Outcome    : %s%n", outcome);
        System.out.printf("Turns      : %d%n", turnsElapsed);
        System.out.printf("Score      : %d / %d points%n", pointsScored, totalPoints);
        System.out.printf("Survivors  : %d%n", survivors.size());
        // Print each survivor's name and remaining HP
        survivors.forEach(a -> System.out.printf("  - %s (hp %d/%d)%n",
            a.getName(), a.getHealth(), a.getMaxHealth()));
        System.out.println("\n--- Turn Log ---");
        log.forEach(System.out::println);
        System.out.println("========================================\n");
    }

    public Outcome getOutcome() { return outcome; }
    public int getPointsScored() { return pointsScored; }
    public int getTurnsElapsed() { return turnsElapsed; }
    public List<Agent> getSurvivors() { return survivors; }
}
