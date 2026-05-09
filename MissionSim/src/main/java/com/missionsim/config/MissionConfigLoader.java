package com.missionsim.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.missionsim.agent.Agent;
import com.missionsim.agent.Engineer;
import com.missionsim.agent.Medic;
import com.missionsim.agent.Scout;
import com.missionsim.map.GameMap;
import com.missionsim.map.Position;
import com.missionsim.map.Terrain;
import com.missionsim.mission.Hazard;
import com.missionsim.mission.Mission;
import com.missionsim.mission.Objective;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

// Reads a JSON mission file and turns it into real Java objects the engine can use.
// Jackson (ObjectMapper) handles all the JSON parsing for us — no manual string splitting needed.
public class MissionConfigLoader {

    // ObjectMapper is thread-safe and reusable, so we create one instance for the class
    private final ObjectMapper mapper = new ObjectMapper();

    // A record is basically a compact way to bundle two related objects together.
    // Java 16+ feature — the compiler auto-generates constructor, getters, equals, etc.
    public record LoadedMission(Mission mission, List<Agent> agents) {}

    public LoadedMission load(InputStream json) throws IOException {
        // readTree() parses the whole JSON into a navigable tree of JsonNode objects
        JsonNode root = mapper.readTree(json);

        // root.get() gets a top-level field; root.at("/map/width") drills into nested objects
        String name = root.get("name").asText();
        String briefing = root.get("briefing").asText();
        int maxTurns = root.get("maxTurns").asInt();

        int width  = root.at("/map/width").asInt();
        int height = root.at("/map/height").asInt();
        GameMap map = new GameMap(width, height);

        // All terrain types share the same loading logic, so I extracted a helper
        applyTerrain(root, "walls",  map, Terrain.WALL);
        applyTerrain(root, "debris", map, Terrain.DEBRIS);
        applyTerrain(root, "water",  map, Terrain.WATER);

        // Build the list of hazards from the JSON array
        List<Hazard> hazards = new ArrayList<>();
        for (JsonNode h : root.withArray("hazards")) {
            Position p = new Position(h.get("x").asInt(), h.get("y").asInt());
            hazards.add(new Hazard(
                h.get("name").asText(),
                p,
                // valueOf() converts the string in JSON ("FIRE", "TOXIC", etc.) to the enum constant
                Hazard.HazardType.valueOf(h.get("type").asText()),
                h.get("damagePerTurn").asInt()
            ));
        }

        // Build the list of objectives similarly
        List<Objective> objectives = new ArrayList<>();
        for (JsonNode o : root.withArray("objectives")) {
            objectives.add(new Objective(
                o.get("name").asText(),
                new Position(o.get("x").asInt(), o.get("y").asInt()),
                o.get("points").asInt()
            ));
        }

        // Build agents — the role string in JSON determines which subclass to create
        List<Agent> agents = new ArrayList<>();
        for (JsonNode a : root.withArray("agents")) {
            String agentName = a.get("name").asText();
            String role = a.get("role").asText();
            Position pos = new Position(a.get("x").asInt(), a.get("y").asInt());
            // Java 14+ switch expression — cleaner than if/else chains, and the
            // compiler will warn us if we forget a case
            agents.add(switch (role) {
                case "MEDIC"    -> new Medic(agentName, pos);
                case "SCOUT"    -> new Scout(agentName, pos);
                case "ENGINEER" -> new Engineer(agentName, pos);
                default -> throw new IllegalArgumentException("Unknown role: " + role);
            });
        }

        Mission mission = new Mission(name, briefing, map, objectives, hazards, maxTurns);
        return new LoadedMission(mission, agents);
    }

    // Reads an array of {x, y} objects from the JSON and stamps each position with the given terrain type
    private void applyTerrain(JsonNode root, String field, GameMap map, Terrain terrain) {
        for (JsonNode node : root.withArray(field)) {
            Position p = new Position(node.get("x").asInt(), node.get("y").asInt());
            if (map.inBounds(p)) map.setTerrain(p, terrain);
        }
    }
}
