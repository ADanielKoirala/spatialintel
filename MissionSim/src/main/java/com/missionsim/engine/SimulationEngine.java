package com.missionsim.engine;

import com.missionsim.agent.Action;
import com.missionsim.agent.Agent;
import com.missionsim.mission.Mission;
import com.missionsim.mission.MissionResult;

import java.util.List;
import java.util.stream.Collectors;

// The SimulationEngine runs the main game loop — each turn every agent decides what
// to do, applies it, and then we check if the mission is over.
public class SimulationEngine {

    private final Mission mission;
    private final List<Agent> agents;
    private final TurnLog log;
    // verbose=true prints the map and agent status after every turn, useful for debugging
    private final boolean verbose;

    public SimulationEngine(Mission mission, List<Agent> agents, boolean verbose) {
        this.mission = mission;
        this.agents  = agents;
        this.log     = new TurnLog();
        this.verbose = verbose;
    }

    public MissionResult run() {
        System.out.println("\n=== MISSION START: " + mission.getName() + " ===\n");
        if (verbose) mission.getMap().print(agentPositions()); // show the map before turn 1

        int turn = 0;
        while (turn < mission.getMaxTurns()) {
            turn++;
            log.logEvent(turn, "--- Turn " + turn + " ---");

            // Each agent takes their action in sequence (not simultaneous)
            for (Agent agent : agents) {
                if (!agent.isAlive()) continue; // skip dead agents

                Action action = agent.decideAction(mission.getMap(), mission, agents);
                agent.applyAction(action, mission.getMap(), mission, agents);
                // log the full agent state + what they did
                log.log(turn, agent.getName(), agent + " -> " + action);
            }

            // After all agents have moved, sync the map so completed objectives
            // show the right character when we print. We do this after the whole round
            // so agents in the same turn all see the same pre-turn state.
            mission.getObjectives().forEach(obj -> {
                if (obj.isComplete())
                    mission.getMap().getTile(obj.getPosition()).setObjectiveComplete(true);
            });

            if (verbose) {
                System.out.println("\n-- Turn " + turn + " --");
                mission.getMap().print(agentPositions());
                printAgentStatus();
            }

            // Early exit: all objectives done
            if (mission.allObjectivesComplete()) {
                log.logEvent(turn, "All objectives secured — mission complete!");
                break;
            }

            // Early exit: everyone is dead
            if (agents.stream().noneMatch(Agent::isAlive)) {
                log.logEvent(turn, "All agents KIA — mission failed.");
                break;
            }
        }

        // If we got here by turn timeout (not a break), turnsElapsed == maxTurns
        return buildResult(turn);
    }

    // Figures out SUCCESS / PARTIAL / FAILURE and packages up the final report
    private MissionResult buildResult(int turnsElapsed) {
        int scored = mission.completedObjectivePoints();
        int total  = mission.totalObjectivePoints();
        // collect only the agents still standing
        List<Agent> survivors = agents.stream().filter(Agent::isAlive).collect(Collectors.toList());

        MissionResult.Outcome outcome;
        if (mission.allObjectivesComplete()) {
            outcome = MissionResult.Outcome.SUCCESS;
        } else if (scored > 0) {
            // got some objectives but not all — partial credit
            outcome = MissionResult.Outcome.PARTIAL;
        } else {
            outcome = MissionResult.Outcome.FAILURE;
        }

        return new MissionResult(outcome, turnsElapsed, scored, total, survivors, log.getEntries());
    }

    // Extracts just the positions of living agents for the map printer
    private List<com.missionsim.map.Position> agentPositions() {
        return agents.stream()
            .filter(Agent::isAlive)
            .map(Agent::getPosition)
            .collect(Collectors.toList());
    }

    // Prints a one-line status for each agent (used after each turn in verbose mode)
    private void printAgentStatus() {
        for (Agent a : agents) {
            System.out.printf("  %-10s hp:%3d/%3d @%s %s%n",
                a.getName(), a.getHealth(), a.getMaxHealth(),
                a.getPosition(), a.isAlive() ? "" : "[KIA]");
        }
    }

    public TurnLog getTurnLog() { return log; }
}
