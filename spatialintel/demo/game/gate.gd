extends StaticBody3D

var _required_key := ""
var _open         := false

func _ready() -> void:
	_required_key = get_meta("required_key", "")

func _on_key_collected(color: String) -> void:
	if not _open and color == _required_key:
		_open = true
		visible = false
		for child in get_children():
			if child is CollisionShape3D:
				child.disabled = true
