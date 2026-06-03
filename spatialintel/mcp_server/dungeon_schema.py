"""
Dungeon schema — Python mirror of the GDScript DungeonRoom / DungeonFloor classes.

Used by MCP tools to validate, inspect, and reason about dungeon floors before
passing them to Godot for construction.
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Optional


class RoomType(str, Enum):
    START = "start"
    EMPTY = "empty"
    COMBAT = "combat"
    PUZZLE = "puzzle"
    KEY = "key"
    BOSS = "boss"


VALID_KEY_COLORS = {"blue", "red", "gold", "silver", "purple"}


@dataclass
class DungeonRoom:
    room_id: int
    room_type: RoomType
    grid_position: tuple[int, int] = (0, 0)
    connections: list[int] = field(default_factory=list)
    key_spawn: Optional[str] = None   # key color placed in this room
    gate_key: Optional[str] = None    # key required to enter this room

    def is_locked(self) -> bool:
        return bool(self.gate_key)

    def has_key(self) -> bool:
        return bool(self.key_spawn)

    def to_dict(self) -> dict:
        return {
            "room_id": self.room_id,
            "room_type": self.room_type.value,
            "grid_position": list(self.grid_position),
            "connections": self.connections,
            "key_spawn": self.key_spawn,
            "gate_key": self.gate_key,
        }

    @classmethod
    def from_dict(cls, d: dict) -> DungeonRoom:
        return cls(
            room_id=d["room_id"],
            room_type=RoomType(d["room_type"]),
            grid_position=tuple(d.get("grid_position", [0, 0])),
            connections=d.get("connections", []),
            key_spawn=d.get("key_spawn") or None,
            gate_key=d.get("gate_key") or None,
        )


@dataclass
class DungeonFloor:
    floor_seed: int = 0
    rooms: list[DungeonRoom] = field(default_factory=list)

    # --- Lookups ---

    def get_room(self, room_id: int) -> Optional[DungeonRoom]:
        return next((r for r in self.rooms if r.room_id == room_id), None)

    def get_start_room(self) -> Optional[DungeonRoom]:
        return next((r for r in self.rooms if r.room_type == RoomType.START), None)

    def get_boss_room(self) -> Optional[DungeonRoom]:
        return next((r for r in self.rooms if r.room_type == RoomType.BOSS), None)

    def get_rooms_by_type(self, room_type: RoomType) -> list[DungeonRoom]:
        return [r for r in self.rooms if r.room_type == room_type]

    def get_key_rooms(self) -> list[DungeonRoom]:
        return [r for r in self.rooms if r.has_key()]

    # --- Validation ---

    def validate(self) -> list[str]:
        """Returns a list of error strings. Empty list means the floor is valid."""
        errors: list[str] = []
        room_ids = {r.room_id for r in self.rooms}

        if not self.get_start_room():
            errors.append("No START room defined")
        if not self.get_boss_room():
            errors.append("No BOSS room defined")

        spawned_keys: set[str] = set()

        for room in self.rooms:
            for conn_id in room.connections:
                if conn_id not in room_ids:
                    errors.append(
                        f"Room {room.room_id} connects to non-existent room {conn_id}"
                    )
            if room.key_spawn and room.key_spawn not in VALID_KEY_COLORS:
                errors.append(
                    f"Room {room.room_id} has invalid key color: '{room.key_spawn}'"
                )
            if room.gate_key and room.gate_key not in VALID_KEY_COLORS:
                errors.append(
                    f"Room {room.room_id} has invalid gate key: '{room.gate_key}'"
                )
            if room.key_spawn:
                spawned_keys.add(room.key_spawn)

        for room in self.rooms:
            if room.gate_key and room.gate_key not in spawned_keys:
                errors.append(
                    f"Room {room.room_id} requires '{room.gate_key}' key "
                    f"but it is not spawned anywhere"
                )

        reach_err = self._check_reachability()
        if reach_err:
            errors.append(reach_err)

        return errors

    def _check_reachability(self) -> Optional[str]:
        """Iterative BFS from start, collecting keys and re-expanding when a new key unlocks gates."""
        start = self.get_start_room()
        boss = self.get_boss_room()
        if not start or not boss:
            return None  # already caught in validate()

        collected: set[str] = set()
        visited: set[int] = set()

        # Repeat passes until no new rooms are reachable.
        # Each pass rebuilds the frontier from unvisited neighbours of visited rooms,
        # so newly collected keys re-unlock previously blocked gates automatically.
        changed = True
        while changed:
            changed = False
            # Seed frontier: unvisited neighbours of every visited room, plus start
            if not visited:
                frontier = [start.room_id]
            else:
                seen_in_frontier: set[int] = set()
                frontier = []
                for rid in list(visited):
                    room = self.get_room(rid)
                    if room:
                        for conn_id in room.connections:
                            if conn_id not in visited and conn_id not in seen_in_frontier:
                                frontier.append(conn_id)
                                seen_in_frontier.add(conn_id)

            while frontier:
                room_id = frontier.pop(0)
                if room_id in visited:
                    continue
                room = self.get_room(room_id)
                if room is None:
                    continue
                if room.gate_key and room.gate_key not in collected:
                    continue  # still locked

                visited.add(room_id)
                changed = True

                if room.key_spawn and room.key_spawn not in collected:
                    collected.add(room.key_spawn)

                for conn_id in room.connections:
                    if conn_id not in visited:
                        frontier.append(conn_id)

        if boss.room_id not in visited:
            return f"Boss room (id={boss.room_id}) is not reachable from start"
        return None

    # --- Summary ---

    def summarize(self) -> dict:
        type_counts = {t.value: 0 for t in RoomType}
        for room in self.rooms:
            type_counts[room.room_type.value] += 1
        return {
            "total_rooms": len(self.rooms),
            "room_types": type_counts,
            "keys_in_floor": [r.key_spawn for r in self.rooms if r.key_spawn],
            "locked_gates": [
                {"room_id": r.room_id, "requires": r.gate_key}
                for r in self.rooms if r.gate_key
            ],
            "validation_errors": self.validate(),
        }

    # --- Serialisation ---

    def to_dict(self) -> dict:
        return {
            "floor_seed": self.floor_seed,
            "rooms": [r.to_dict() for r in self.rooms],
        }

    @classmethod
    def from_dict(cls, d: dict) -> DungeonFloor:
        return cls(
            floor_seed=d.get("floor_seed", 0),
            rooms=[DungeonRoom.from_dict(r) for r in d.get("rooms", [])],
        )
