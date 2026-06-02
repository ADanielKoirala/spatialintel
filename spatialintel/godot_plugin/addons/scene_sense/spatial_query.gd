@tool
extends RefCounted

func find_nodes_near(root: Node, args: Dictionary) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	var od: Dictionary = args.get("origin", {})
	var origin := Vector3(od.get("x", 0.0), od.get("y", 0.0), od.get("z", 0.0))
	var radius: float = args.get("radius", 10.0)
	var results: Array = []
	_collect_near(root, origin, radius, results)
	return {"result": results}

func _collect_near(node: Node, origin: Vector3, radius: float, out: Array) -> void:
	if node is Node3D:
		var dist := node.global_position.distance_to(origin)
		if dist <= radius:
			out.append({
				"name": node.name,
				"type": node.get_class(),
				"path": str(node.get_path()),
				"distance": snappedf(dist, 0.001)
			})
	for child in node.get_children():
		_collect_near(child, origin, radius, out)


func find_missing_colliders(root: Node) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	var results: Array = []
	_check_missing_colliders(root, results)
	return {"result": results}

func _check_missing_colliders(node: Node, out: Array) -> void:
	if node is MeshInstance3D:
		if not _has_collision_sibling_or_parent(node):
			out.append({
				"name": node.name,
				"path": str(node.get_path()),
				"issue": "MeshInstance3D has no associated CollisionShape3D"
			})
	for child in node.get_children():
		_check_missing_colliders(child, out)

func _has_collision_sibling_or_parent(node: Node) -> bool:
	var parent := node.get_parent()
	if parent == null:
		return false
	if parent is CollisionObject3D:
		return true
	for sibling in parent.get_children():
		if sibling is CollisionShape3D or sibling is CollisionPolygon3D:
			return true
	return false


func find_overlapping_objects(root: Node) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	var nodes: Array = []
	_collect_spatial(root, nodes)
	var results: Array = []
	for i in range(nodes.size()):
		for j in range(i + 1, nodes.size()):
			var dist := (nodes[i].pos as Vector3).distance_to(nodes[j].pos as Vector3)
			if dist < 0.5:
				results.append({
					"node_a": nodes[i].name,
					"path_a": nodes[i].path,
					"node_b": nodes[j].name,
					"path_b": nodes[j].path,
					"distance": snappedf(dist, 0.001)
				})
	return {"result": results}

func _collect_spatial(node: Node, out: Array) -> void:
	# Only physics bodies matter for overlap detection — collecting all Node3D
	# would flood results with children sharing their parent's world position.
	if node is PhysicsBody3D:
		out.append({"name": node.name, "path": str(node.get_path()), "pos": node.global_position})
	for child in node.get_children():
		_collect_spatial(child, out)


func find_nodes_by_type(root: Node, args: Dictionary) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	var target: String = args.get("type", "")
	if target.is_empty():
		return {"error": "missing required argument: type"}
	var results: Array = []
	_collect_by_type(root, target, results)
	return {"result": results}

func _collect_by_type(node: Node, type_name: String, out: Array) -> void:
	if node.get_class() == type_name or node.is_class(type_name):
		var entry: Dictionary = {
			"name": node.name,
			"type": node.get_class(),
			"path": str(node.get_path())
		}
		if node is Node3D:
			entry["position"] = {"x": snappedf(node.global_position.x, 0.001), "y": snappedf(node.global_position.y, 0.001), "z": snappedf(node.global_position.z, 0.001)}
		out.append(entry)
	for child in node.get_children():
		_collect_by_type(child, type_name, out)


func summarize_scene(root: Node) -> Dictionary:
	if root == null:
		return {"error": "no scene is currently open in the editor"}
	var s := {
		"total_nodes": 0,
		"node3d_count": 0,
		"mesh_count": 0,
		"collision_shape_count": 0,
		"light_count": 0,
		"camera_count": 0,
		"physics_body_count": 0,
		"scripted_node_count": 0
	}
	_count(root, s)
	return {"result": s}

func _count(node: Node, s: Dictionary) -> void:
	s["total_nodes"] += 1
	if node is Node3D:            s["node3d_count"] += 1
	if node is MeshInstance3D:    s["mesh_count"] += 1
	if node is CollisionShape3D:  s["collision_shape_count"] += 1
	if node is Light3D:           s["light_count"] += 1
	if node is Camera3D:          s["camera_count"] += 1
	if node is PhysicsBody3D:     s["physics_body_count"] += 1
	if node.get_script():         s["scripted_node_count"] += 1
	for child in node.get_children():
		_count(child, s)
