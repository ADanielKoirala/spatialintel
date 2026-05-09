package com.missionsim;

import com.missionsim.config.MissionConfigLoader;
import com.missionsim.config.MissionConfigLoader.LoadedMission;
import com.missionsim.engine.SimulationEngine;
import com.missionsim.mission.MissionResult;
import com.missionsim.nlp.MissionBriefParser;

import java.io.InputStream;

public class Main {

    public static void main(String[] args) throws Exception {
        // First command-line arg is the mission file path; fall back to the default mission
        String missionFile = args.length > 0 ? args[0] : "/missions/mission01.json";
        // verbose is true by default; pass --quiet as the second arg to suppress map output
        boolean verbose    = args.length < 2 || !args[1].equals("--quiet");

        // getResourceAsStream looks inside the JAR's classpath, so the JSON file
        // doesn't need to be a separate file on disk after we package the project
        InputStream is = Main.class.getResourceAsStream(missionFile);
        if (is == null) {
            System.err.println("Mission file not found: " + missionFile);
            System.exit(1);
        }

        // Load the mission config and agents from the JSON file
        MissionConfigLoader loader = new MissionConfigLoader();
        LoadedMission loaded = loader.load(is);

        // Parse and display the mission briefing so we can see what the objectives are
        MissionBriefParser nlp = new MissionBriefParser();
        // 'var' here is just type inference — Java figures out the type is List<Directive>
        var directives = nlp.parse(loaded.mission().getBriefing());
        System.out.println("MISSION: " + loaded.mission().getName());
        System.out.println("BRIEFING: " + loaded.mission().getBriefing());
        nlp.printDirectives(directives);

        // Hand everything off to the engine and run the simulation
        SimulationEngine engine = new SimulationEngine(loaded.mission(), loaded.agents(), verbose);
        MissionResult result = engine.run();
        result.printReport();
    }
}
