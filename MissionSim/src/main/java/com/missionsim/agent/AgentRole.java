package com.missionsim.agent;

// Simple enum for the three roles in the game.
// Having this as an enum (instead of a String) means we can't typo a role name
// and the compiler will catch it if we forget a case in a switch.
public enum AgentRole {
    MEDIC,
    SCOUT,
    ENGINEER
}
