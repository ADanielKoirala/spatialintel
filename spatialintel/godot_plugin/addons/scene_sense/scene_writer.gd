@tool
extends RefCounted

func create_node(scene_root: Node, undo_redo: EditorUndoRedoManager, args: Dictionary) -> Dictionary:
	var type: String = args.get("type", "")
	var name: String = args.get("name", "")
	var parent_path: String = args.get("parent", ".")

	if type.is_empty():
		return {"error": "type is required"}
	if name.is_empty():
		return {"error": "name is required"}
	if not ClassDB.class_exists(type):
		return {"error": "unknown Godot class: %s" % type}

	var parent := _get_node(scene_root, parent_path)
	if parent == null:
		return {"error": "parent not found: %s" % parent_path}

	var new_node: Node = ClassDB.instantiate(type)
	new_node.name = name

	undo_redo.create_action("SceneSense: create %s '%s'" % [type, name])
	undo_redo.add_do_method(parent, "add_child", new_node)
	undo_redo.add_do_method(new_node, "set_owner", scene_root)
	undo_redo.add_do_reference(new_node)
	undo_redo.add_undo_method(parent, "remove_child", new_node)
	undo_redo.commit_action()

	return {"result": {"path": _rel(new_node, scene_root), "type": type, "name": name}}


func set_property(scene_root: Node, undo_redo: EditorUndoRedoManager, args: Dictionary) -> Dictionary:
	var node_path: String = args.get("node", "")
	var property: String = args.get("property", "")
	var raw_value = args.get("value", null)

	if node_path.is_empty():
		return {"error": "node is required"}
	if property.is_empty():
		return {"error": "property is required"}

	var node := _get_node(scene_root, node_path)
	if node == null:
		return {"error": "node not found: %s" % node_path}

	var value = _parse_value(raw_value)
	var old_value = node.get(property)

	undo_redo.create_action("SceneSense: set %s.%s" % [node_path, property])
	undo_redo.add_do_property(node, property, value)
	undo_redo.add_undo_property(node, property, old_value)
	undo_redo.commit_action()

	return {"result": {"node": node_path, "property": property, "value": str(value)}}


func delete_node(scene_root: Node, undo_redo: EditorUndoRedoManager, args: Dictionary) -> Dictionary:
	var node_path: String = args.get("node", "")

	if node_path.is_empty() or node_path == ".":
		return {"error": "cannot delete the scene root"}

	var node := _get_node(scene_root, node_path)
	if node == null:
		return {"error": "node not found: %s" % node_path}

	var parent := node.get_parent()

	undo_redo.create_action("SceneSense: delete '%s'" % node_path)
	undo_redo.add_do_method(parent, "remove_child", node)
	undo_redo.add_undo_method(parent, "add_child", node)
	undo_redo.add_undo_method(node, "set_owner", scene_root)
	undo_redo.add_undo_reference(node)
	undo_redo.commit_action()

	return {"result": {"deleted": node_path}}


func move_node(scene_root: Node, undo_redo: EditorUndoRedoManager, args: Dictionary) -> Dictionary:
	var node_path: String = args.get("node", "")
	var new_parent_path: String = args.get("new_parent", "")

	if node_path.is_empty() or node_path == ".":
		return {"error": "cannot move the scene root"}
	if new_parent_path.is_empty():
		return {"error": "new_parent is required"}

	var node := _get_node(scene_root, node_path)
	if node == null:
		return {"error": "node not found: %s" % node_path}

	var old_parent := node.get_parent()
	var new_parent := _get_node(scene_root, new_parent_path)
	if new_parent == null:
		return {"error": "new_parent not found: %s" % new_parent_path}
	if old_parent == new_parent:
		return {"error": "node is already under %s" % new_parent_path}

	undo_redo.create_action("SceneSense: move '%s' -> '%s'" % [node_path, new_parent_path])
	undo_redo.add_do_method(old_parent, "remove_child", node)
	undo_redo.add_do_method(new_parent, "add_child", node)
	undo_redo.add_do_method(node, "set_owner", scene_root)
	undo_redo.add_undo_method(new_parent, "remove_child", node)
	undo_redo.add_undo_method(old_parent, "add_child", node)
	undo_redo.add_undo_method(node, "set_owner", scene_root)
	undo_redo.commit_action()

	return {"result": {"moved": node_path, "new_parent": new_parent_path}}


func _get_node(scene_root: Node, path: String) -> Node:
	if path == "." or path.is_empty():
		return scene_root
	return scene_root.get_node_or_null(path)

func _parse_value(raw: Variant) -> Variant:
	if raw is Dictionary:
		if raw.has("x") and raw.has("y") and raw.has("z"):
			return Vector3(float(raw.get("x", 0)), float(raw.get("y", 0)), float(raw.get("z", 0)))
		if raw.has("x") and raw.has("y"):
			return Vector2(float(raw.get("x", 0)), float(raw.get("y", 0)))
		if raw.has("r") and raw.has("g") and raw.has("b"):
			return Color(float(raw.get("r", 0)), float(raw.get("g", 0)), float(raw.get("b", 0)), float(raw.get("a", 1)))
	return raw

func _rel(node: Node, root: Node) -> String:
	if node == root:
		return "."
	var root_path := str(root.get_path())
	var node_path := str(node.get_path())
	if node_path.begins_with(root_path + "/"):
		return node_path.substr(root_path.length() + 1)
	return str(root.get_path_to(node))
