"""
Dungeon floor generator.

Strategy: guaranteed-solvable by construction.
  1. Build a linear critical path (START → middle rooms → BOSS).
  2. Place gates and their keys on the critical path so every key always
     appears strictly before its gate — the player can never be locked out.
  3. Hang branch rooms off non-gated critical path nodes for exploration.

The result always passes DungeonFloor.validate().
"""
from __future__ import annotations

import random
from typing import Optional

from dungeon_schema import DungeonFloor, DungeonRoom, RoomType, VALID_KEY_COLORS

# Room types that can appear in the middle of the critical path or in branches
_MID_TYPES = [RoomType.EMPTY, RoomType.COMBAT, RoomType.PUZZLE, RoomType.EMPTY]
_BRANCH_TYPES = [RoomType.EMPTY, RoomType.COMBAT, RoomType.EMPTY]


def generate(
    room_count: int,
    key_count: int,
    seed: int = 0,
    branch_rooms: Optional[int] = None,
) -> DungeonFloor:
    """
    Generate a valid DungeonFloor.

    Args:
        room_count:   Total number of rooms (minimum 3).
        key_count:    Number of key/gate pairs (0–5). Each adds one locked gate
                      and one key room on the critical path.
        seed:         RNG seed for reproducibility.
        branch_rooms: How many rooms to hang off the critical path as dead ends /
                      optional content. Defaults to room_count - critical_path_length.
    """
    rng = random.Random(seed)

    # --- Clamp parameters ---
    room_count = max(3, room_count)
    key_count = max(0, min(key_count, len(VALID_KEY_COLORS)))

    # Critical path must have room for: START, one room per key, one gate per key, BOSS
    # Minimum cp = 2 (start + boss) + key_count (key rooms) + key_count (gated rooms)
    # but gated rooms can overlap with middle rooms, so min cp = key_count * 2 + 2
    min_cp_length = key_count * 2 + 2
    max_cp_length = room_count if branch_rooms == 0 else room_count - (branch_rooms or 0)
    cp_length = max(min_cp_length, min(max_cp_length, room_count))
    cp_length = min(cp_length, room_count)

    actual_branches = room_count - cp_length

    rooms: list[DungeonRoom] = []
    next_id = 0

    # --- Build critical path ---
    cp: list[DungeonRoom] = []

    for i in range(cp_length):
        if i == 0:
            rtype = RoomType.START
        elif i == cp_length - 1:
            rtype = RoomType.BOSS
        else:
            rtype = rng.choice(_MID_TYPES)

        room = DungeonRoom(next_id, rtype, (i, 0))

        if i > 0:
            prev = cp[i - 1]
            room.connections.append(prev.room_id)
            prev.connections.append(next_id)

        cp.append(room)
        rooms.append(room)
        next_id += 1

    # --- Place keys and gates on the critical path ---
    key_colors = rng.sample(sorted(VALID_KEY_COLORS), key_count)

    # Gate positions: distributed along cp[2 .. cp_length-1] (never on start or room-after-start)
    gate_candidates = list(range(2, cp_length))
    rng.shuffle(gate_candidates)
    gate_indices = sorted(gate_candidates[:key_count])

    used_for_key: set[int] = set()

    for gate_idx, color in zip(gate_indices, key_colors):
        cp[gate_idx].gate_key = color

        # Key goes on any room strictly before gate_idx that doesn't already have a key
        key_candidates = [j for j in range(gate_idx) if j not in used_for_key]
        key_idx = rng.choice(key_candidates) if key_candidates else 0
        used_for_key.add(key_idx)

        cp[key_idx].key_spawn = color
        if cp[key_idx].room_type not in (RoomType.START, RoomType.BOSS):
            cp[key_idx].room_type = RoomType.KEY

    # --- Add branch rooms ---
    # Branches hang off non-gated, non-boss critical path rooms.
    # Being off the critical path means they're optional — dead ends / loot.
    eligible_parents = [r for r in cp if r.room_type != RoomType.BOSS and not r.is_locked()]
    if not eligible_parents:
        eligible_parents = cp[:-1]

    for i in range(actual_branches):
        parent = rng.choice(eligible_parents)
        btype = rng.choice(_BRANCH_TYPES)
        y = (i // 2 + 1) * (1 if i % 2 == 0 else -1)
        branch = DungeonRoom(next_id, btype, (parent.grid_position[0], y))
        branch.connections.append(parent.room_id)
        parent.connections.append(next_id)
        rooms.append(branch)
        next_id += 1

    return DungeonFloor(floor_seed=seed, rooms=rooms)
