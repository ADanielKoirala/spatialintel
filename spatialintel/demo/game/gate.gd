extends StaticBody3D

var _required_key := ""
var _open         := false

func _ready() -> void:
	_required_key = get_meta("required_key", "")

func _on_key_collected(color: String) -> void:
	if not _open and color == _required_key:
		_open = true
		visible = false
		# Zero collision layers — takes effect this physics step, not deferred
		collision_layer = 0
		collision_mask  = 0
		queue_free()
