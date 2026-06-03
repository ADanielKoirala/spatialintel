class_name DungeonGenerator
extends RefCounted

const KEY_COLORS: Array[String] = ["blue", "red", "gold", "silver", "purple"]

const MID_TYPES: Array = [
	DungeonRoom.RoomType.EMPTY,
	DungeonRoom.RoomType.COMBAT,
	DungeonRoom.RoomType.PUZZLE,
	DungeonRoom.RoomType.EMPTY,
]
const BRANCH_TYPES: Array = [
	DungeonRoom.RoomType.EMPTY,
	DungeonRoom.RoomType.COMBAT,
	DungeonRoom.RoomType.EMPTY,
]

static func generate(room_count: int, key_count: int, seed_val: int = 0) -> DungeonFloor:
	var rng := RandomNumberGenerator.new()
	rng.seed = seed_val if seed_val != 0 else rng.randi()

	room_count = maxi(3, room_count)
	key_count  = maxi(0, mini(key_count, KEY_COLORS.size()))

	var min_cp: int    = key_count * 2 + 2
	var cp_length: int = mini(room_count, maxi(min_cp, room_count / 2 + 1))

	var rooms: Array[DungeonRoom] = []
	var next_id := 0

	# ── Critical path ──────────────────────────────────────────────────────────
	var cp: Array[DungeonRoom] = []
	for i in range(cp_length):
		var room := DungeonRoom.new()
		room.room_id       = next_id
		room.grid_position = Vector2i(i, 0)
		if i == 0:
			room.room_type = DungeonRoom.RoomType.START
		elif i == cp_length - 1:
			room.room_type = DungeonRoom.RoomType.BOSS
		else:
			room.room_type = MID_TYPES[rng.randi() % MID_TYPES.size()]
		if i > 0:
			room.connections.append(cp[i - 1].room_id)
			cp[i - 1].connections.append(next_id)
		cp.append(room)
		rooms.append(room)
		next_id += 1

	# ── Keys and gates ─────────────────────────────────────────────────────────
	var shuffled_keys: Array = KEY_COLORS.duplicate()
	_shuffle(shuffled_keys, rng)

	var gate_candidates: Array[int] = []
	for i in range(2, cp_length):
		gate_candidates.append(i)
	_shuffle(gate_candidates, rng)

	var gate_indices: Array[int] = gate_candidates.slice(0, key_count)
	gate_indices.sort()

	var used_for_key: Array[int] = []
	for idx in range(gate_indices.size()):
		var gate_idx: int = gate_indices[idx]
		var color: String = shuffled_keys[idx]
		cp[gate_idx].gate_key = color

		var candidates: Array[int] = []
		for j in range(gate_idx):
			if not used_for_key.has(j):
				candidates.append(j)
		var key_pos := 0 if candidates.is_empty() else candidates[rng.randi() % candidates.size()]
		used_for_key.append(key_pos)
		cp[key_pos].key_spawn = color
		if cp[key_pos].room_type != DungeonRoom.RoomType.START and cp[key_pos].room_type != DungeonRoom.RoomType.BOSS:
			cp[key_pos].room_type = DungeonRoom.RoomType.KEY

	# ── Branch rooms ───────────────────────────────────────────────────────────
	var eligible: Array[DungeonRoom] = []
	for r in cp:
		if r.room_type != DungeonRoom.RoomType.BOSS and r.gate_key.is_empty():
			eligible.append(r)
	if eligible.is_empty():
		eligible = cp.slice(0, cp.size() - 1)

	for i in range(room_count - cp_length):
		var parent_room: DungeonRoom = eligible[rng.randi() % eligible.size()]
		var room := DungeonRoom.new()
		room.room_id       = next_id
		var y := (i / 2 + 1) * (1 if i % 2 == 0 else -1)
		room.grid_position = Vector2i(parent_room.grid_position.x, y)
		room.room_type     = BRANCH_TYPES[rng.randi() % BRANCH_TYPES.size()]
		room.connections.append(parent_room.room_id)
		parent_room.connections.append(next_id)
		rooms.append(room)
		next_id += 1

	var floor := DungeonFloor.new()
	floor.floor_seed = seed_val
	floor.rooms      = rooms
	return floor

static func _shuffle(arr: Array, rng: RandomNumberGenerator) -> void:
	for i in range(arr.size() - 1, 0, -1):
		var j := rng.randi() % (i + 1)
		var tmp = arr[i]
		arr[i]  = arr[j]
		arr[j]  = tmp
