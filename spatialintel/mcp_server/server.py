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


if __name__ == "__main__":
    mcp.run()
