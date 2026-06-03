class_name DungeonBuilderRuntime
extends RefCounted

const ROOM_SPACING  := 12.0
const FLOOR_SIZE    := Vector3(10.0, 0.3, 10.0)
const CORRIDOR_WIDTH := 3.0

static func build(dungeon: DungeonFloor, parent: Node) -> Node3D:
	var container := Node3D.new()
	container.name = "Dungeon"
	parent.add_child(container)

	# Pass 1 — rooms
	var room_nodes: Dictionary = {}
	for room in dungeon.rooms:
		var node := _make_room(room)
		container.add_child(node)
		room_nodes[room.room_id] = node

	# Pass 2 — corridors (each pair once)
	for room in dungeon.rooms:
		for conn_id in room.connections:
			if room.room_id >= conn_id:
				continue
			var na: Node3D = room_nodes.get(room.room_id)
			var nb: Node3D = room_nodes.get(conn_id)
			if na and nb:
				var corridor := _make_corridor(room.room_id, conn_id, na.position, nb.position)
				if corridor:
					container.add_child(corridor)

	return container

# ── Room ──────────────────────────────────────────────────────────────────────

static func _make_room(room: DungeonRoom) -> Node3D:
	var root := Node3D.new()
	root.name     = "Room_%d" % room.room_id
	root.position = Vector3(room.grid_position.x * ROOM_SPACING, 0.0,
	                        room.grid_position.y * ROOM_SPACING)
	root.set_meta("room_id",   room.room_id)
	root.set_meta("room_type", DungeonRoom.RoomType.keys()[room.room_type].to_lower())

	# Walkable dungeon
	var body := StaticBody3D.new()
	body.name = "Floor"
	var col   := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = FLOOR_SIZE
	col.shape  = shape
	body.add_child(col)
	var mesh_inst := MeshInstance3D.new()
	var box       := BoxMesh.new()
	box.size      = FLOOR_SIZE
	mesh_inst.mesh = box
	var mat := StandardMaterial3D.new()
	mat.albedo_color = _room_color(room.room_type)
	mesh_inst.material_override = mat
	body.add_child(mesh_inst)
	root.add_child(body)

	if room.key_spawn != "":
		root.add_child(_make_key_pickup(room.key_spawn))

	if room.gate_key != "":
		root.add_child(_make_gate(room.gate_key))

	if room.room_type == DungeonRoom.RoomType.BOSS:
		root.add_child(_make_boss_trigger())

	return root

# ── Key pickup ────────────────────────────────────────────────────────────────

static func _make_key_pickup(color: String) -> Area3D:
	var area := Area3D.new()
	area.name = "KeyPickup"
	area.set_meta("key_color", color)

	var col    := CollisionShape3D.new()
	var sphere := SphereShape3D.new()
	sphere.radius = 1.0
	col.shape     = sphere
	area.add_child(col)

	var mesh_inst  := MeshInstance3D.new()
	mesh_inst.name = "Marker"
	var sm         := SphereMesh.new()
	sm.radius      = 0.4
	sm.height      = 0.8
	mesh_inst.mesh = sm
	mesh_inst.position = Vector3(0.0, 1.2, 0.0)
	var kmat := StandardMaterial3D.new()
	kmat.albedo_color          = _key_color(color)
	kmat.emission_enabled      = true
	kmat.emission              = _key_color(color)
	kmat.emission_energy_multiplier = 0.5
	mesh_inst.material_override = kmat
	area.add_child(mesh_inst)

	area.set_script(load("res://game/key_pickup.gd"))
	return area

# ── Gate ──────────────────────────────────────────────────────────────────────

static func _make_gate(color: String) -> StaticBody3D:
	var gate := StaticBody3D.new()
	gate.name = "Gate"
	gate.set_meta("required_key", color)
	gate.add_to_group("gates")
	# Full-width wall across the room entrance — spans the whole room so the
	# player cannot squeeze around it. Gate must be unlocked with the key.
	gate.position = Vector3(-4.0, 1.5, 0.0)

	var col   := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(0.4, 3.0, FLOOR_SIZE.z)
	col.shape  = shape
	gate.add_child(col)

	var mesh := MeshInstance3D.new()
	var box  := BoxMesh.new()
	box.size = Vector3(0.4, 3.0, FLOOR_SIZE.z)
	mesh.mesh = box
	var mat := StandardMaterial3D.new()
	mat.albedo_color = _key_color(color)
	mat.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
	mat.albedo_color.a = 0.75
	mesh.material_override = mat
	gate.add_child(mesh)

	gate.set_script(load("res://game/gate.gd"))
	return gate

# ── Boss trigger ──────────────────────────────────────────────────────────────

static func _make_boss_trigger() -> Area3D:
	var area := Area3D.new()
	area.name = "BossTrigger"
	area.add_to_group("boss_triggers")

	var col   := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(8.0, 4.0, 8.0)
	col.shape  = shape
	area.add_child(col)
	return area

# ── Corridor ──────────────────────────────────────────────────────────────────

static func _make_corridor(id_a: int, id_b: int, pos_a: Vector3, pos_b: Vector3) -> StaticBody3D:
	var diff     := pos_b - pos_a
	var distance := diff.length()
	if distance < 1.0:
		return null

	# Extend 1 unit into each room so there is no collision seam at room edges.
	var length := maxf(0.5, distance - FLOOR_SIZE.x + 2.0)

	var body := StaticBody3D.new()
	body.name     = "Corridor_%d_%d" % [id_a, id_b]
	body.position = (pos_a + pos_b) * 0.5
	body.rotation_degrees.y = -rad_to_deg(atan2(diff.z, diff.x))

	var col   := CollisionShape3D.new()
	var shape := BoxShape3D.new()
	shape.size = Vector3(length, FLOOR_SIZE.y, CORRIDOR_WIDTH)
	col.shape  = shape
	body.add_child(col)

	var mesh := MeshInstance3D.new()
	var box  := BoxMesh.new()
	box.size = Vector3(length, FLOOR_SIZE.y, CORRIDOR_WIDTH)
	mesh.mesh = box
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.38, 0.38, 0.38)
	mesh.material_override = mat
	body.add_child(mesh)

	return body

# ── Colour helpers ────────────────────────────────────────────────────────────

static func _room_color(rt: DungeonRoom.RoomType) -> Color:
	match rt:
		DungeonRoom.RoomType.START:  return Color(0.20, 0.75, 0.20)
		DungeonRoom.RoomType.BOSS:   return Color(0.75, 0.10, 0.10)
		DungeonRoom.RoomType.KEY:    return Color(0.85, 0.75, 0.10)
		DungeonRoom.RoomType.COMBAT: return Color(0.65, 0.30, 0.15)
		DungeonRoom.RoomType.PUZZLE: return Color(0.20, 0.35, 0.75)
		_:                           return Color(0.45, 0.45, 0.45)

static func _key_color(key: String) -> Color:
	match key:
		"blue":   return Color(0.10, 0.40, 1.00)
		"red":    return Color(1.00, 0.15, 0.15)
		"gold":   return Color(1.00, 0.80, 0.00)
		"silver": return Color(0.80, 0.80, 0.85)
		"purple": return Color(0.65, 0.10, 0.90)
		_:        return Color(1.00, 1.00, 1.00)
