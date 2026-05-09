

## Design Goal

MissionSim is designed to demonstrate a clear separation between simulation coordination, agent decision-making, map/pathfinding logic, and mission state management.

## Key Design Decisions

### 1. Abstract Agent Base Class

The `Agent` class centralizes shared properties such as name, role, position, health, and priority. Subclasses implement the `decideAction(...)` method to provide specialized role behavior.

This supports polymorphism: the simulation engine can treat every unit as an `Agent` while allowing each role to behave differently.

### 2. Action Objects

Agents do not directly modify the world while deciding what to do. They return an `Action` object that describes the intended behavior.

This improves testability because decisions can be inspected before execution.

### 3. Simulation Engine as Coordinator

The `SimulationEngine` is responsible for turn order, logging, termination checks, and final result creation. It delegates decision-making to agents instead of embedding agent-specific logic inside the engine.

### 4. A* Pathfinding as a Separate Component

Pathfinding is isolated in `AStarPathfinder`, allowing the movement algorithm to evolve independently from agent logic.

### 5. JSON Mission Configuration

Mission data is loaded from JSON resources. This separates scenario content from Java source code and makes it easier to add new missions.

## Extension Points

- Add new `Agent` subclasses
- Add new `Action.Type` values
- Add new terrain or hazard types
- Add new mission JSON scenarios
- Replace rule-based decisions with utility scoring or behavior trees
- Add a UI or remote API wrapper around the simulation engine
