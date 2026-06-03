extends Node3D

const ROOM_SPACING := 12.0

@onready var _timer_label:    Label    = $HUD/TimerLabel
@onready var _keys_label:     Label    = $HUD/KeysLabel
@onready var _win_panel:      Panel    = $HUD/WinPanel
@onready var _win_time_label: Label    = $HUD/WinPanel/WinTimeLabel
@onready var _camera:         Camera3D = $Camera3D

var _game_time  := 0.0
var _game_active := false
var _player: Player
var _floor: DungeonFloor

func _ready() -> void:
	_floor = DungeonGenerator.generate(12, 2, randi())
	DungeonBuilderRuntime.build(_floor, self)
	_spawn_player()
	_connect_gates()
	_connect_boss_triggers()
	$HUD/WinPanel/RestartButton.pressed.connect(_on_restart_pressed)
	_win_panel.visible = false
	_game_active = true

func _spawn_player() -> void:
	_player = CharacterBody3D.new()
	_player.set_script(load("res://game/player.gd"))
	_player.name = "Player"

	var col   := CollisionShape3D.new()
	var cap   := CapsuleShape3D.new()
	cap.radius = 0.4
	cap.height = 1.8
	col.shape  = cap
	_player.add_child(col)

	var mesh := MeshInstance3D.new()
	var cm   := CapsuleMesh.new()
	cm.radius = 0.4
	cm.height = 1.8
	mesh.mesh = cm
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.2, 0.6, 1.0)
	mesh.material_override = mat
	_player.add_child(mesh)

	var start := _floor.get_start_room()
	if start:
		_player.position = Vector3(
			start.grid_position.x * ROOM_SPACING,
			1.2,
			start.grid_position.y * ROOM_SPACING
		)

	add_child(_player)

func _connect_gates() -> void:
	for gate in get_tree().get_nodes_in_group("gates"):
		_player.key_collected.connect(gate._on_key_collected)

func _connect_boss_triggers() -> void:
	for trigger in get_tree().get_nodes_in_group("boss_triggers"):
		trigger.body_entered.connect(_on_boss_entered)

func _on_boss_entered(body: Node3D) -> void:
	if body == _player and _game_active:
		_win()

func _win() -> void:
	_game_active = false
	_win_panel.visible = true
	_win_time_label.text = "Time: %.2f s" % _game_time

func _process(delta: float) -> void:
	if not _game_active:
		return

	_game_time += delta
	_timer_label.text = "%.1f s" % _game_time

	if _player and is_instance_valid(_player):
		var keys := _player.collected_keys
		_keys_label.text = "Keys: " + (", ".join(keys) if keys.size() > 0 else "none")

		# Smooth camera follow from above
		var target := _player.position + Vector3(0.0, 18.0, 12.0)
		_camera.position = _camera.position.lerp(target, delta * 5.0)
		_camera.look_at(_player.position + Vector3(0.0, 1.0, 0.0))

func _on_restart_pressed() -> void:
	get_tree().reload_current_scene()
