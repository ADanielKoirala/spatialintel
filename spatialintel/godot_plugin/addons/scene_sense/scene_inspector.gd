@tool
extends RefCounted

func get_scene_tree(root: Node) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	return {"result": _serialize_node(root)}

func _serialize_node(node: Node) -> Dictionary:
	var data: Dictionary = {
		"name": node.name,
		"type": node.get_class(),
		"path": str(node.get_path()),
		"children": []
	}

	if node is Node3D:
		data["position"] = _v3(node.global_position)
		data["rotation_degrees"] = _v3(node.global_rotation_degrees)
		data["scale"] = _v3(node.scale)

	if node is MeshInstance3D:
		data["has_mesh"] = node.mesh != null

	if node is Camera3D:
		data["fov"] = node.fov
		data["current"] = node.current

	if node is Light3D:
		data["light_energy"] = node.light_energy

	if node is CollisionShape3D:
		data["shape_type"] = node.shape.get_class() if node.shape else null

	var script = node.get_script()
	if script:
		data["script"] = script.resource_path

	for child in node.get_children():
		data["children"].append(_serialize_node(child))

	return data

func _v3(v: Vector3) -> Dictionary:
	return {"x": snappedf(v.x, 0.001), "y": snappedf(v.y, 0.001), "z": snappedf(v.z, 0.001)}
