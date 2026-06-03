import base64
import json
import os
import tempfile

from mcp.server.fastmcp import FastMCP

from godot_bridge import GodotBridge
from dungeon_schema import DungeonFloor
import dungeon_generator

mcp = FastMCP("SceneSense")
bridge = GodotBridge()


def _fmt(result: dict) -> str:
    return json.dumps(result, indent=2)


@mcp.tool()
async def get_scene_tree() -> str:
    """Get the full serialized scene tree of the currently open Godot 4 scene,
    including node names, types, positions, rotations, scale, and scripts."""
    return _fmt(await bridge.get_scene_tree())


@mcp.tool()
async def find_nodes_near(x: float, y: float, z: float, radius: float = 10.0) -> str:
    """Find all nodes within a given radius of a 3D position in the scene.

    Args:
        x: World-space X coordinate of the origin point.
        y: World-space Y coordinate of the origin point.
        z: World-space Z coordinate of the origin point.
        radius: Search radius in Godot world units (default 10).
    """
    return _fmt(await bridge.find_nodes_near({"x": x, "y": y, "z": z}, radius))


@mcp.tool()
async def find_missing_colliders() -> str:
    """Find all MeshInstance3D nodes that have no associated CollisionShape3D.
    These will cause players or objects to fall through the mesh at runtime."""
    return _fmt(await bridge.find_missing_colliders())


@mcp.tool()
async def find_overlapping_objects() -> str:
    """Find pairs of Node3D objects whose positions are suspiciously close (< 0.5 units).
    Useful for detecting duplicate objects or physics bodies clipping into each other."""
    return _fmt(await bridge.find_overlapping_objects())


@mcp.tool()
async def find_nodes_by_type(type_name: str) -> str:
    """Find all nodes of a specific Godot class in the scene.

    Args:
        type_name: Godot class name, e.g. Camera3D, CharacterBody3D, Light3D, Area3D.
    """
    return _fmt(await bridge.find_nodes_by_type(type_name))


@mcp.tool()
async def summarize_scene() -> str:
    """Get a high-level summary of the scene: total node count, mesh count,
    collision shapes, lights, cameras, physics bodies, and scripted nodes."""
    return _fmt(await bridge.summarize_scene())


@mcp.tool()
async def analyze_scene() -> str:
    """Run a full diagnostic analysis of the current Godot 4 scene.

    Collects scene summary, missing colliders, and overlapping objects in a single
    round-trip, then classifies each finding by severity and returns a structured
    QA report with an overall risk level.

    Risk levels: None / Low / Medium / High
    Severity levels per issue: info / low / medium / high
    """
    raw = await bridge.analyze_scene()

    if "error" in raw:
        return _fmt({"error": raw["error"]})

    data = raw.get("result", {})
    summary: dict = data.get("summary", {})
    missing: list = data.get("missing_colliders", [])
    overlapping: list = data.get("overlapping_objects", [])

    issues: list[dict] = []

    # Missing colliders — gameplay-breaking: objects fall through mesh
    for item in missing:
        issues.append({
            "severity": "high",
            "category": "collision",
            "node": item["name"],
            "path": item["path"],
            "message": f"{item['name']} is a MeshInstance3D with no CollisionShape3D — objects will fall through it at runtime",
        })

    # No Camera3D — scene will render black
    if summary.get("camera_count", 0) == 0:
        issues.append({
            "severity": "high",
            "category": "rendering",
            "message": "No Camera3D found in scene — nothing will render at runtime",
        })

    # Overlapping physics bodies — probable duplicate or physics explosion
    for item in overlapping:
        issues.append({
            "severity": "medium",
            "category": "physics",
            "node_a": item["node_a"],
            "node_b": item["node_b"],
            "distance": item["distance"],
            "message": (
                f"{item['node_a']} and {item['node_b']} are {item['distance']} units apart "
                f"— physics bodies this close may cause jitter or tunnelling"
            ),
        })

    # No lights — might be intentional but worth flagging
    if summary.get("light_count", 0) == 0 and summary.get("total_nodes", 0) > 1:
        issues.append({
            "severity": "low",
            "category": "rendering",
            "message": "No Light3D found — scene will rely entirely on ambient/environment lighting",
        })

    # Determine risk level
    high = sum(1 for i in issues if i["severity"] == "high")
    medium = sum(1 for i in issues if i["severity"] == "medium")

    if high > 0:
        risk = "High"
    elif medium > 0:
        risk = "Medium"
    elif issues:
        risk = "Low"
    else:
        risk = "None"

    return _fmt({
        "risk_level": risk,
        "issue_count": len(issues),
        "issues": issues,
        "scene_summary": summary,
    })


_SCREENSHOT_PATH = os.path.join(tempfile.gettempdir(), "scenesense_screenshot.png")


@mcp.tool()
async def take_screenshot() -> str:
    """Capture the Godot 3D editor viewport as a PNG screenshot.

    Saves the image to a temp file and returns the file path.
    Use the Read tool on the returned path to view the screenshot.
    """
    raw = await bridge.screenshot()

    if "error" in raw:
        return _fmt({"error": raw["error"]})

    result = raw.get("result", {})
    b64 = result.get("b64", "")
    if not b64:
        return _fmt({"error": "no image data received from Godot"})

    img_bytes = base64.b64decode(b64)
    with open(_SCREENSHOT_PATH, "wb") as f:
        f.write(img_bytes)

    return _fmt({
        "saved_to": _SCREENSHOT_PATH,
        "width": result.get("width"),
        "height": result.get("height"),
        "size_kb": round(len(img_bytes) / 1024, 1),
    })


# ---------------------------------------------------------------------------
# Dungeon schema tools
# ---------------------------------------------------------------------------

@mcp.tool()
async def validate_dungeon(dungeon_json: str) -> str:
    """Validate a dungeon floor description against the DungeonFloor schema.

    Checks: START and BOSS rooms exist, all connections reference real rooms,
    every gate key is spawned somewhere, and the boss is reachable from start
    (BFS respecting locked gates and key collection order).

    Args:
        dungeon_json: JSON string representing a DungeonFloor — must have
                      "floor_seed" (int) and "rooms" (array). Each room needs:
                      room_id, room_type (start/empty/combat/puzzle/key/boss),
                      connections (array of room_id ints),
                      key_spawn (color string or null),
                      gate_key (color string or null).
                      Valid key colors: blue, red, gold, silver, purple.
    """
    try:
        data = json.loads(dungeon_json)
    except json.JSONDecodeError as e:
        return _fmt({"valid": False, "errors": [f"Invalid JSON: {e}"]})

    try:
        floor = DungeonFloor.from_dict(data)
    except Exception as e:
        return _fmt({"valid": False, "errors": [f"Schema parse error: {e}"]})

    errors = floor.validate()
    summary = floor.summarize()

    return _fmt({
        "valid": len(errors) == 0,
        "errors": errors,
        "summary": {
            "total_rooms": summary["total_rooms"],
            "room_types": summary["room_types"],
            "keys_in_floor": summary["keys_in_floor"],
            "locked_gates": summary["locked_gates"],
        },
    })


@mcp.tool()
async def generate_dungeon(
    room_count: int = 12,
    key_count: int = 2,
    seed: int = 0,
) -> str:
    """Generate a valid dungeon floor and return it as JSON.

    The generator guarantees solvability by construction: a critical path
    (START → rooms → BOSS) is built first, then keys and locked gates are
    placed so every key always appears before its gate. Branch rooms are
    added for optional exploration and dead ends.

    Args:
        room_count: Total number of rooms (minimum 3, default 12).
        key_count:  Number of key/gate pairs 0–5 (default 2).
                    More keys = more routing decisions.
        seed:       RNG seed for reproducibility (default 0 = random-ish).
    """
    floor = dungeon_generator.generate(room_count=room_count, key_count=key_count, seed=seed)
    errors = floor.validate()
    summary = floor.summarize()

    return _fmt({
        "valid": len(errors) == 0,
        "errors": errors,
        "floor": floor.to_dict(),
        "summary": {
            "total_rooms": summary["total_rooms"],
            "room_types": summary["room_types"],
            "keys_in_floor": summary["keys_in_floor"],
            "locked_gates": summary["locked_gates"],
        },
    })


@mcp.tool()
async def build_dungeon_in_godot(dungeon_json: str) -> str:
    """Instantiate a dungeon floor as nodes in the currently open Godot scene.

    Each room becomes a Node3D named Room_<id> with a colour-coded floor mesh.
    Room metadata (type, key_spawn, gate_key) is stored on the node.
    Key rooms get a glowing sphere marker; gated rooms get a pillar marker.
    All rooms are placed under a single "Dungeon" container node.
    Any existing "Dungeon" container is replaced. Undoable with Ctrl+Z.

    Args:
        dungeon_json: JSON string — either the direct floor object
                      ({"floor_seed":…, "rooms":[…]}) or the full output
                      from generate_dungeon ({"floor":{…}, …}).
    """
    try:
        data = json.loads(dungeon_json)
    except json.JSONDecodeError as e:
        return _fmt({"error": f"Invalid JSON: {e}"})

    floor_data = data.get("floor", data)
    return _fmt(await bridge.build_dungeon(floor_data))


# ---------------------------------------------------------------------------
# Write tools
# ---------------------------------------------------------------------------

@mcp.tool()
async def create_node(node_type: str, node_name: str, parent_path: str = ".") -> str:
    """Create a new node in the currently open Godot scene. The operation is undoable (Ctrl+Z).

    Args:
        node_type: Any valid Godot class name, e.g. Node3D, StaticBody3D, MeshInstance3D,
                   CollisionShape3D, Area3D, CharacterBody3D, DirectionalLight3D, Camera3D.
        node_name: Name to give the new node.
        parent_path: Scene-relative path to the parent (default "." = scene root).
    """
    return _fmt(await bridge.create_node(node_type, node_name, parent_path))


@mcp.tool()
async def set_property(node_path: str, property: str, value: str) -> str:
    """Set a property on a node in the scene. The operation is undoable (Ctrl+Z).

    Common properties: position, rotation_degrees, scale, visible, name.
    Pass Vector3 values as JSON objects: {"x": 1, "y": 2, "z": 3}
    Pass Color values as JSON objects: {"r": 1, "g": 0, "b": 0, "a": 1}

    Args:
        node_path: Scene-relative path to the node, e.g. "Player" or "Floor/MeshInstance3D".
        property: Property name exactly as Godot names it.
        value: JSON-encoded value — number, boolean, string, or object for Vector3/Color.
    """
    import json as _json
    try:
        parsed = _json.loads(value)
    except Exception:
        parsed = value
    return _fmt(await bridge.set_property(node_path, property, parsed))


@mcp.tool()
async def delete_node(node_path: str) -> str:
    """Delete a node from the scene. The operation is undoable (Ctrl+Z).

    Args:
        node_path: Scene-relative path to the node to delete, e.g. "Enemy_02".
    """
    return _fmt(await bridge.delete_node(node_path))


@mcp.tool()
async def move_node(node_path: str, new_parent_path: str) -> str:
    """Reparent a node to a different parent in the scene. The operation is undoable (Ctrl+Z).

    Args:
        node_path: Scene-relative path to the node to move.
        new_parent_path: Scene-relative path to the new parent node.
    """
    return _fmt(await bridge.move_node(node_path, new_parent_path))


if __name__ == "__main__":
    mcp.run()
