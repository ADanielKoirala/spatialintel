package com.missionsim.mission;

import com.missionsim.map.Position;

// A goal location on the map that agents must reach and secure to earn points.
// Most fields are final (they don't change), but 'complete' flips to true once secured.
public class Objective {
    private final String name;
    private final Position position;
    private final int missionPoints; // how many points completing this objective scores
    private boolean complete;        // starts false, set to true by Agent.applyAction

    public Objective(String name, Position position, int missionPoints) {
        this.name = name;
        this.position = position;
        this.missionPoints = missionPoints;
        this.complete = false;
    }

    // Called by the engine when an agent successfully secures this objective
    public void complete() { this.complete = true; }

    public String getName() { return name; }
    public Position getPosition() { return position; }
    public int getMissionPoints() { return missionPoints; }
    public boolean isComplete() { return complete; }

    // Shows something like "Alpha@(3,5) [PENDING]" or "Alpha@(3,5) [DONE]"
    @Override
    public String toString() {
        return name + "@" + position + (complete ? " [DONE]" : " [PENDING]");
    }
}
