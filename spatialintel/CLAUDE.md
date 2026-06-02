# SpatialIntel — Project Context for Claude

## What this is

A **spatial intelligence platform** that gives AI agents (Claude, Cursor, Codex) the ability to understand, inspect, and reason about 3D game worlds — starting with Godot 4.

The core insight: LLMs can read code but can't understand a game world. They see `player.position += velocity` but have no idea if the player is underground, if a navmesh is broken, or if a door is blocking progression. This project bridges that gap.

Product name for the MVP: **SceneSense**

## Target users (MVP)

Godot 4 developers who use AI coding assistants and want the AI to actually understand their scene, not just their scripts.

## Architecture

```
Godot 4 Editor Plugin (GDScript)
  — WebSocket server on port 9800
  — Inspects scene tree, nodes, spatial data
  — Runs spatial queries
  — Captures screenshots

        ↕ WebSocket (JSON commands)

Python MCP Server
  — Wraps Godot data as MCP tools
  — Claude / Cursor / Codex connects here
  — Exposes clean natural-language-friendly tools
```

## Tech stack

- **Godot 4** — GDScript plugin, WebSocketPeer, TCPServer
- **Python 3.11+** — MCP server using `mcp` SDK (FastMCP)
- **websockets** — Python WebSocket client connecting to Godot
- **MCP protocol** — so Claude/Cursor can call tools natively

## MVP feature scope

### Layer 1 — Scene Graph Export
Serialize the full scene tree: node names, types, positions, rotations, scale, scripts, mesh/collider/camera/light metadata.

### Layer 2 — Spatial Queries
- `find_nodes_near(origin, radius)` — objects within N units
- `find_missing_colliders()` — MeshInstance3D with no collision
- `find_overlapping_objects()` — physics bodies suspiciously close
- `find_nodes_by_type(type)` — e.g. all Camera3D, all CharacterBody3D
- `summarize_scene()` — node counts, physics bodies, lights, cameras

### Layer 3 — Visual (post-MVP)
Screenshot capture from editor/game viewport.

### Layer 4 — Runtime Simulation (post-MVP)
Spawn player, simulate input, record outcomes.

## Project structure to build

```
spatialintel/
├── godot_plugin/
│   └── addons/
│       └── scene_sense/
│           ├── plugin.cfg         # Godot plugin metadata
│           ├── plugin.gd          # EditorPlugin entry point
│           ├── ws_server.gd       # WebSocket server + command dispatcher
│           ├── scene_inspector.gd # Scene tree serialization
│           └── spatial_query.gd   # Spatial query functions
├── mcp_server/
│   ├── server.py                  # FastMCP server, exposes tools
│   ├── godot_bridge.py            # WebSocket client to Godot
│   └── requirements.txt
└── CLAUDE.md
```

## 7-day build plan

| Day | Task |
|-----|------|
| 1 | Godot plugin scaffold + TCPServer/WebSocket server in GDScript |
| 2 | Scene tree serialization — nodes, types, positions, metadata |
| 3 | Python WebSocket client + basic MCP server with `get_scene_tree` tool |
| 4 | Spatial query tools in GDScript + Python MCP wrappers |
| 5 | `find_missing_colliders`, `find_overlapping_objects`, `summarize_scene` |
| 6 | Screenshot capture from Godot viewport |
| 7 | End-to-end demo: broken 3D scene → Claude answers "why is gameplay broken?" |

## Demo to build toward

Create a small broken Godot 3D level:
- Player, door, enemy, wall
- Missing collider on bridge
- Camera clipping into floor
- Enemy outside navmesh

Then ask Claude: *"Inspect this scene and tell me why gameplay feels broken."*

Expected response:
```
Player is 18.2m from Door.
Wall_03 blocks direct path.
Door has no interaction script.
Enemy_01 is outside navmesh bounds.
Camera clips into FloorMesh at y=-0.3.
Bridge_02 has no CollisionShape — player will fall through.
```

## Broader vision (post-Godot)

Same architecture applies to: robotics simulators, digital twins, factory layouts, military training sims, autonomous vehicle environments, architectural tools. Godot is the beachhead.

## Environment notes

- Development moved from WSL2 to **Windows native** (Windows 10)
- Godot runs natively on Windows — no WSL networking issues
- Python MCP server runs on Windows too — everything on localhost
- WebSocket bridge: Godot plugin → `ws://localhost:9800` → Python server
- Git remote: `https://github.com/ADanielKoirala/spatialintel.git`

## Where to start next session

Build the files in this order:
1. `godot_plugin/addons/scene_sense/plugin.cfg`
2. `godot_plugin/addons/scene_sense/plugin.gd`
3. `godot_plugin/addons/scene_sense/ws_server.gd`
4. `godot_plugin/addons/scene_sense/scene_inspector.gd`
5. `godot_plugin/addons/scene_sense/spatial_query.gd`
6. `mcp_server/requirements.txt`
7. `mcp_server/godot_bridge.py`
8. `mcp_server/server.py`
