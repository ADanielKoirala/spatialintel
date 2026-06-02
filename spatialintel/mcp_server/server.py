import json

from mcp.server.fastmcp import FastMCP

from godot_bridge import GodotBridge

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


if __name__ == "__main__":
    mcp.run()
