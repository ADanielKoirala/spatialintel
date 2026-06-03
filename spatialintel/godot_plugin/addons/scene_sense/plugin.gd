@tool
extends EditorPlugin

const WSServerScript = preload("res://addons/scene_sense/ws_server.gd")

var _ws_server: Node

func _enter_tree() -> void:
	_ws_server = WSServerScript.new()
	_ws_server.setup(get_editor_interface())
	add_child(_ws_server)
	_ws_server.start()

func _exit_tree() -> void:
	if _ws_server:
		_ws_server.stop()
		_ws_server.free()
		_ws_server = null
