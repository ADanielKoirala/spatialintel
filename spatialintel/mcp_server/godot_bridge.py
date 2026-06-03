import asyncio
import json

import websockets
from websockets.exceptions import ConnectionClosed, WebSocketException

GODOT_WS_URL = "ws://localhost:9800"


class GodotBridge:
    def __init__(self, url: str = GODOT_WS_URL):
        self.url = url
        self._ws = None
        self._lock = asyncio.Lock()

    async def _ensure_connected(self) -> None:
        if self._ws is None or self._ws.closed:
            self._ws = await websockets.connect(self.url)

    async def send_command(self, command: str, args: dict | None = None) -> dict:
        async with self._lock:
            try:
                await self._ensure_connected()
                payload = {"command": command, "args": args or {}}
                await self._ws.send(json.dumps(payload))
                raw = await asyncio.wait_for(self._ws.recv(), timeout=10.0)
                return json.loads(raw)
            except (ConnectionClosed, WebSocketException, OSError) as e:
                self._ws = None
                return {"error": f"Could not reach Godot — is the SceneSense plugin active? ({e})"}
            except asyncio.TimeoutError:
                return {"error": "Godot did not respond within 10 seconds"}

    # Convenience wrappers used by server.py

    async def get_scene_tree(self) -> dict:
        return await self.send_command("get_scene_tree")

    async def find_nodes_near(self, origin: dict, radius: float) -> dict:
        return await self.send_command("find_nodes_near", {"origin": origin, "radius": radius})

    async def find_missing_colliders(self) -> dict:
        return await self.send_command("find_missing_colliders")

    async def find_overlapping_objects(self) -> dict:
        return await self.send_command("find_overlapping_objects")

    async def find_nodes_by_type(self, type_name: str) -> dict:
        return await self.send_command("find_nodes_by_type", {"type": type_name})

    async def summarize_scene(self) -> dict:
        return await self.send_command("summarize_scene")

    async def analyze_scene(self) -> dict:
        return await self.send_command("analyze_scene")

    # Write operations

    async def create_node(self, node_type: str, name: str, parent: str = ".") -> dict:
        return await self.send_command("create_node", {"type": node_type, "name": name, "parent": parent})

    async def set_property(self, node_path: str, property: str, value) -> dict:
        return await self.send_command("set_property", {"node": node_path, "property": property, "value": value})

    async def delete_node(self, node_path: str) -> dict:
        return await self.send_command("delete_node", {"node": node_path})

    async def move_node(self, node_path: str, new_parent: str) -> dict:
        return await self.send_command("move_node", {"node": node_path, "new_parent": new_parent})
