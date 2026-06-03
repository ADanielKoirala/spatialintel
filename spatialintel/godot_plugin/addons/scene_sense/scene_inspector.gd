@tool
extends RefCounted

func get_scene_tree(root: Node) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	return {"result": _serialize_node(root, root)}

func _serialize_node(node: Node, root: Node) -> Dictionary:
	var root_path := str(root.get_path())
	var node_path := str(node.get_path())
	var rel_path: String
	if node == root:
		rel_path = "."
	elif node_path.begins_with(root_path + "/"):
		rel_path = node_path.substr(root_path.length() + 1)
	else:
		rel_path = str(root.get_path_to(node))
	var data: Dictionary = {
		"name": node.name,
		"type": node.get_class(),
		"path": rel_path,
		"children": []
	}

	if node is Node3D:
		var n3d: Node3D = node
		data["position"] = _v3(n3d.global_position)
		data["rotation_degrees"] = _v3(n3d.global_rotation_degrees)
		data["scale"] = _v3(n3d.scale)

	if node is MeshInstance3D:
		var mi: MeshInstance3D = node
		data["has_mesh"] = mi.mesh != null

	if node is Camera3D:
		var cam: Camera3D = node
		data["fov"] = cam.fov
		data["current"] = cam.current

	if node is Light3D:
		var light: Light3D = node
		data["light_energy"] = light.light_energy

	if node is CollisionShape3D:
		var cs: CollisionShape3D = node
		data["shape_type"] = cs.shape.get_class() if cs.shape else null

	var script = node.get_script()
	if script:
		data["script"] = script.resource_path

	for child in node.get_children():
		data["children"].append(_serialize_node(child, root))

	return data

func _v3(v: Vector3) -> Dictionary:
	return {"x": snappedf(v.x, 0.001), "y": snappedf(v.y, 0.001), "z": snappedf(v.z, 0.001)}
