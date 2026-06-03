class_name Player
extends CharacterBody3D

const SPEED   := 8.0
const GRAVITY := 20.0

signal key_collected(color: String)

var collected_keys: Array[String] = []

func _ready() -> void:
	floor_snap_length = 0.3  # smooths capsule over floor-to-floor seams

func _physics_process(delta: float) -> void:
	if not is_on_floor():
		velocity.y -= GRAVITY * delta
	else:
		velocity.y = 0.0

	var input := Vector3(
		Input.get_axis("ui_left", "ui_right"),
		0.0,
		Input.get_axis("ui_up", "ui_down")
	)
	if input.length() > 1.0:
		input = input.normalized()
	velocity.x = input.x * SPEED
	velocity.z = input.z * SPEED

	move_and_slide()

func collect_key(color: String) -> void:
	if not collected_keys.has(color):
		collected_keys.append(color)
		key_collected.emit(color)

func has_key(color: String) -> bool:
	return collected_keys.has(color)
