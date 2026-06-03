@tool
extends Node

const PORT = 9800

const SceneInspectorScript = preload("res://addons/scene_sense/scene_inspector.gd")
const SpatialQueryScript = preload("res://addons/scene_sense/spatial_query.gd")
const SceneWriterScript = preload("res://addons/scene_sense/scene_writer.gd")
const DungeonBuilderScript = preload("res://addons/scene_sense/dungeon_builder.gd")

var _tcp: TCPServer
var _peers: Dictionary = {}   # instance_id -> WebSocketPeer
var _editor: EditorInterface
var _undo_redo: EditorUndoRedoManager
var _inspector: RefCounted
var _query: RefCounted
var _writer: RefCounted
var _dungeon_builder: RefCounted

func setup(editor: EditorInterface, undo_redo: EditorUndoRedoManager) -> void:
	_editor = editor
	_undo_redo = undo_redo

func start() -> void:
	_tcp = TCPServer.new()
	_inspector = SceneInspectorScript.new()
	_query = SpatialQueryScript.new()
	_writer = SceneWriterScript.new()
	_dungeon_builder = DungeonBuilderScript.new()

	var err := _tcp.listen(PORT)
	if err != OK:
		push_error("SceneSense: failed to listen on port %d (error %d)" % [PORT, err])
		return
	print("SceneSense: WebSocket server listening on ws://localhost:%d" % PORT)

func stop() -> void:
	for peer in _peers.values():
		peer.close()
	_peers.clear()
	if _tcp:
		_tcp.stop()
		_tcp = null
	print("SceneSense: WebSocket server stopped")

func _process(_delta: float) -> void:
	if not _tcp:
		return
	while _tcp.is_connection_available():
		var stream := _tcp.take_connection()
		var peer := WebSocketPeer.new()
		peer.outbound_buffer_size = 8 * 1024 * 1024  # 8 MB — needed for screenshot payloads
		if peer.accept_stream(stream) == OK:
			_peers[peer.get_instance_id()] = peer

	var dead: Array = []
	for id in _peers:
		var peer: WebSocketPeer = _peers[id]
		peer.poll()
		match peer.get_ready_state():
			WebSocketPeer.STATE_OPEN:
				while peer.get_available_packet_count() > 0:
					_handle_message(id, peer.get_packet())
			WebSocketPeer.STATE_CLOSED:
				dead.append(id)
				print("SceneSense: client disconnected")

	for id in dead:
		_peers.erase(id)

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
		# --- screenshot ---
		"screenshot":
			if _editor == null:
				return {"error": "editor interface not available"}
			var viewport: SubViewport = _editor.get_editor_viewport_3d(0)
			if viewport == null:
				return {"error": "could not get 3D editor viewport"}
			var image: Image = viewport.get_texture().get_image()
			if image == null or image.is_empty():
				return {"error": "viewport texture is empty — is a scene open?"}
			image.resize(image.get_width() / 2, image.get_height() / 2, Image.INTERPOLATE_BILINEAR)
			return {
				"result": {
					"b64": Marshalls.raw_to_base64(image.save_jpg_to_buffer(0.75)),
					"format": "jpg",
					"width": image.get_width(),
					"height": image.get_height(),
				}
			}
		# --- read ---
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
		"analyze_scene":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return {"result": {
				"summary": _query.summarize_scene(scene_root).get("result", {}),
				"missing_colliders": _query.find_missing_colliders(scene_root).get("result", []),
				"overlapping_objects": _query.find_overlapping_objects(scene_root).get("result", []),
			}}
		# --- write ---
		"create_node":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return _writer.create_node(scene_root, _undo_redo, args)
		"set_property":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return _writer.set_property(scene_root, _undo_redo, args)
		"delete_node":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return _writer.delete_node(scene_root, _undo_redo, args)
		"move_node":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return _writer.move_node(scene_root, _undo_redo, args)
		"build_dungeon":
			if scene_root == null:
				return {"error": "no scene is currently open in the editor"}
			return _dungeon_builder.build(scene_root, _undo_redo, args)
		_:
			return {"error": "unknown command: %s" % command}

func _reply(peer_id: int, data: Dictionary) -> void:
	if _peers.has(peer_id):
		var peer: WebSocketPeer = _peers[peer_id]
		peer.send_text(JSON.stringify(data))
