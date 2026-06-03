class_name DungeonFloor
extends Resource

@export var floor_seed: int = 0
@export var rooms: Array[DungeonRoom] = []

# --- Lookups ---

func get_room(id: int) -> DungeonRoom:
	for room in rooms:
		if room.room_id == id:
			return room
	return null

func get_start_room() -> DungeonRoom:
	for room in rooms:
		if room.room_type == DungeonRoom.RoomType.START:
			return room
	return null

func get_boss_room() -> DungeonRoom:
	for room in rooms:
		if room.room_type == DungeonRoom.RoomType.BOSS:
			return room
	return null

func get_rooms_by_type(type: DungeonRoom.RoomType) -> Array[DungeonRoom]:
	var result: Array[DungeonRoom] = []
	for room in rooms:
		if room.room_type == type:
			result.append(room)
	return result

func get_key_rooms() -> Array[DungeonRoom]:
	var result: Array[DungeonRoom] = []
	for room in rooms:
		if room.has_key():
			result.append(room)
	return result

# --- Validation ---

## Returns a list of error strings. Empty = floor is valid.
func validate() -> Array[String]:
	var errors: Array[String] = []
	var room_ids: Array[int] = []
	for room in rooms:
		room_ids.append(room.room_id)

	if get_start_room() == null:
		errors.append("No START room defined")
	if get_boss_room() == null:
		errors.append("No BOSS room defined")

	var valid_keys := ["blue", "red", "gold", "silver", "purple"]
	var spawned_keys: Array[String] = []

	for room in rooms:
		# Connection integrity
		for conn_id in room.connections:
			if not room_ids.has(conn_id):
				errors.append("Room %d connects to non-existent room %d" % [room.room_id, conn_id])

		# Key color validity
		if room.key_spawn != "" and not valid_keys.has(room.key_spawn):
			errors.append("Room %d has invalid key color: '%s'" % [room.room_id, room.key_spawn])
		if room.gate_key != "" and not valid_keys.has(room.gate_key):
			errors.append("Room %d has invalid gate key: '%s'" % [room.room_id, room.gate_key])

		if room.key_spawn != "":
			spawned_keys.append(room.key_spawn)

	# Every gate key must be spawned somewhere
	for room in rooms:
		if room.gate_key != "" and not spawned_keys.has(room.gate_key):
			errors.append("Room %d requires '%s' key but it is not spawned anywhere" % [room.room_id, room.gate_key])

	# Reachability: can player reach boss collecting keys along the way?
	var reach_error := _check_reachability()
	if reach_error != "":
		errors.append(reach_error)

	return errors

func _check_reachability() -> String:
	var start := get_start_room()
	var boss := get_boss_room()
	if start == null or boss == null:
		return ""  # already caught above

	var collected: Array[String] = []
	var visited: Array[int] = []

	# Repeat passes until no new rooms are reached.
	# Each pass rebuilds the frontier from unvisited neighbours of visited rooms,
	# so a newly collected key immediately re-unlocks blocked gates on the next pass.
	var any_progress := true
	while any_progress:
		any_progress = false

		var frontier: Array[int] = []
		if visited.is_empty():
			frontier = [start.room_id]
		else:
			for rid in visited:
				var r := get_room(rid)
				if r == null:
					continue
				for conn_id in r.connections:
					if not visited.has(conn_id) and not frontier.has(conn_id):
						frontier.append(conn_id)

		while frontier.size() > 0:
			var room_id: int = frontier.pop_front()
			if visited.has(room_id):
				continue
			var room := get_room(room_id)
			if room == null:
				continue
			if room.gate_key != "" and not collected.has(room.gate_key):
				continue  # still locked

			visited.append(room_id)
			any_progress = true

			if room.key_spawn != "" and not collected.has(room.key_spawn):
				collected.append(room.key_spawn)

			for conn_id in room.connections:
				if not visited.has(conn_id):
					frontier.append(conn_id)

	if not visited.has(boss.room_id):
		return "Boss room (id=%d) is not reachable from start" % boss.room_id
	return ""

# --- Serialisation ---

func to_dict() -> Dictionary:
	var room_list := []
	for room in rooms:
		room_list.append(room.to_dict())
	return {"floor_seed": floor_seed, "rooms": room_list}
