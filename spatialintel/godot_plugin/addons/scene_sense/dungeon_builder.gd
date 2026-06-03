@tool
extends RefCounted

const ROOM_SPACING := 12.0
const FLOOR_SIZE := Vector3(10.0, 0.3, 10.0)
const CONTAINER_NAME := "Dungeon"

func build(scene_root: Node, undo_redo: EditorUndoRedoManager, args: Dictionary) -> Dictionary:
	var dungeon: Dictionary = args.get("dungeon", {})
	var rooms_data: Array = dungeon.get("rooms", [])

	if rooms_data.is_empty():
		return {"error": "dungeon data contains no rooms"}

	# Build the full tree in memory before touching the scene
	var container := Node3D.new()
	container.name = CONTAINER_NAME

	for room_data in rooms_data:
		var room_node := _make_room(room_data)
		container.add_child(room_node)

	# Single undoable action: swap in the new container
	var existing := scene_root.get_node_or_null(CONTAINER_NAME)

	undo_redo.create_action("SceneSense: build dungeon (%d rooms)" % rooms_data.size())
	if existing:
		undo_redo.add_do_method(scene_root, "remove_child", existing)
	undo_redo.add_do_method(scene_root, "add_child", container)
	undo_redo.add_do_method(container, "set_owner", scene_root)
	undo_redo.add_do_reference(container)
	undo_redo.add_undo_method(scene_root, "remove_child", container)
	if existing:
		undo_redo.add_undo_method(scene_root, "add_child", existing)
		undo_redo.add_undo_method(existing, "set_owner", scene_root)
		undo_redo.add_undo_reference(existing)
	undo_redo.commit_action()

	# Set owner on every descendant (requires node to be in the scene tree)
	_set_owners(container, scene_root)

	return {
		"result": {
			"container": CONTAINER_NAME,
			"rooms_built": rooms_data.size(),
			"seed": dungeon.get("floor_seed", 0),
		}
	}


func _make_room(data: Dictionary) -> Node3D:
	var room_id: int = data.get("room_id", 0)
	var room_type: String = str(data.get("room_type", "empty"))
	var raw_key_spawn = data.get("key_spawn")
	var raw_gate_key = data.get("gate_key")
	var key_spawn: String = "" if raw_key_spawn == null else str(raw_key_spawn)
	var gate_key: String = "" if raw_gate_key == null else str(raw_gate_key)
	var grid_pos: Array = data.get("grid_position", [0, 0])

	var room := Node3D.new()
	room.name = "Room_%d" % room_id
	room.position = Vector3(grid_pos[0] * ROOM_SPACING, 0.0, grid_pos[1] * ROOM_SPACING)

	# Metadata for game logic
	room.set_meta("room_id", room_id)
	room.set_meta("room_type", room_type)
	if key_spawn != "":
		room.set_meta("key_spawn", key_spawn)
	if gate_key != "":
		room.set_meta("gate_key", gate_key)

	# Floor mesh coloured by room type
	var floor_mesh := MeshInstance3D.new()
	floor_mesh.name = "Floor"
	var box := BoxMesh.new()
	box.size = FLOOR_SIZE
	floor_mesh.mesh = box
	var mat := StandardMaterial3D.new()
	mat.albedo_color = _room_color(room_type)
	floor_mesh.material_override = mat
	room.add_child(floor_mesh)

	# Key marker: small yellow sphere above the floor
	if key_spawn != "":
		var marker := MeshInstance3D.new()
		marker.name = "KeyMarker"
		var sphere := SphereMesh.new()
		sphere.radius = 0.4
		sphere.height = 0.8
		marker.mesh = sphere
		marker.position = Vector3(0.0, 1.2, 0.0)
		var key_mat := StandardMaterial3D.new()
		key_mat.albedo_color = _key_color(key_spawn)
		key_mat.emission_enabled = true
		key_mat.emission = _key_color(key_spawn)
		key_mat.emission_energy_multiplier = 0.6
		marker.material_override = key_mat
		room.add_child(marker)

	# Gate marker: flat red ring at the room edge
	if gate_key != "":
		var gate_marker := MeshInstance3D.new()
		gate_marker.name = "GateMarker"
		var cyl := CylinderMesh.new()
		cyl.top_radius = 0.3
		cyl.bottom_radius = 0.3
		cyl.height = 2.0
		gate_marker.mesh = cyl
		gate_marker.position = Vector3(-5.0, 1.0, 0.0)
		var gate_mat := StandardMaterial3D.new()
		gate_mat.albedo_color = _key_color(gate_key)
		gate_marker.material_override = gate_mat
		room.add_child(gate_marker)

	return room


func _set_owners(node: Node, owner: Node) -> void:
	node.set_owner(owner)
	for child in node.get_children():
		_set_owners(child, owner)


func _room_color(room_type: String) -> Color:
	match room_type:
		"start":  return Color(0.20, 0.75, 0.20)
		"boss":   return Color(0.75, 0.10, 0.10)
		"key":    return Color(0.85, 0.75, 0.10)
		"combat": return Color(0.65, 0.30, 0.15)
		"puzzle": return Color(0.20, 0.35, 0.75)
		_:        return Color(0.45, 0.45, 0.45)


func _key_color(key: String) -> Color:
	match key:
		"blue":   return Color(0.1, 0.4, 1.0)
		"red":    return Color(1.0, 0.15, 0.15)
		"gold":   return Color(1.0, 0.8, 0.0)
		"silver": return Color(0.8, 0.8, 0.85)
		"purple": return Color(0.65, 0.1, 0.9)
		_:        return Color(1.0, 1.0, 1.0)
