@tool
extends Node

const PORT = 9800

const SceneInspectorScript = preload("res://addons/scene_sense/scene_inspector.gd")
const SpatialQueryScript = preload("res://addons/scene_sense/spatial_query.gd")

var _server := WebSocketMultiplayerPeer.new()
var _editor: EditorInterface
var _inspector: RefCounted
var _query: RefCounted

func setup(editor: EditorInterface) -> void:
	_editor = editor

func start() -> void:
	_inspector = SceneInspectorScript.new()
	_query = SpatialQueryScript.new()

	var err := _server.create_server(PORT)
	if err != OK:
		push_error("SceneSense: failed to start WebSocket server on port %d (error %d)" % [PORT, err])
		return

	_server.peer_connected.connect(_on_peer_connected)
	_server.peer_disconnected.connect(_on_peer_disconnected)
	print("SceneSense: WebSocket server listening on ws://localhost:%d" % PORT)

func stop() -> void:
	_server.close()
	print("SceneSense: WebSocket server stopped")

func _process(_delta: float) -> void:
	_server.poll()
	while _server.get_available_packet_count() > 0:
		var packet := _server.get_packet()
		var peer_id := _server.get_packet_peer()
		_handle_message(peer_id, packet)

func _on_peer_connected(id: int) -> void:
	print("SceneSense: client connected (id=%d)" % id)

func _on_peer_disconnected(id: int) -> void:
	print("SceneSense: client disconnected (id=%d)" % id)

func _handle_message(peer_id: int, packet: PackedByteArray) -> void:
	var text := packet.get_string_from_utf8()
	var data = JSON.parse_string(text)
	if data == null:
		_reply(peer_id, {"error": "invalid JSON"})
		return
	var command: String = data.get("command", "")
	var args: Dictionary = data.get("args", {})
	_reply(peer_id, _dispatch(command, args))

func _dispatch(command: String, args: Dictionary) -> Dictionary:
	var scene_root := _editor.get_edited_scene_root() if _editor else null
	match command:
		"get_scene_tree":
			return _inspector.get_scene_tree(scene_root)
		"find_nodes_near":
			return _query.find_nodes_near(scene_root, args)
		"find_missing_colliders":
			return _query.find_missing_colliders(scene_root)
		"find_overlapping_objects":
			return _query.find_overlapping_objects(scene_root)
		"find_nodes_by_type":
			return _query.find_nodes_by_type(scene_root, args)
		"summarize_scene":
			return _query.summarize_scene(scene_root)
		_:
			return {"error": "unknown command: %s" % command}

func _reply(peer_id: int, data: Dictionary) -> void:
	var peer := _server.get_peer(peer_id)
	if peer:
		peer.put_packet(JSON.stringify(data).to_utf8_buffer())
