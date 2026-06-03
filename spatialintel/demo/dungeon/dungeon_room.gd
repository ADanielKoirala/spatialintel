class_name DungeonRoom
extends Resource

enum RoomType {
	START,   ## Where the player spawns
	EMPTY,   ## Filler/exploration room
	COMBAT,  ## Contains enemies
	PUZZLE,  ## Skill or logic challenge
	KEY,     ## Contains a key pickup
	BOSS     ## Final room — kill boss to finish floor
}

## Unique ID for this room within its DungeonFloor
@export var room_id: int = 0

## What kind of room this is
@export var room_type: RoomType = RoomType.EMPTY

## Position on the dungeon grid (for layout)
@export var grid_position: Vector2i = Vector2i.ZERO

## IDs of rooms directly connected to this one (bidirectional)
@export var connections: Array[int] = []

## Key color spawned inside this room ("blue", "red", "gold", "silver", "purple", or "")
@export var key_spawn: String = ""

## Key color required to enter this room ("" = always open)
@export var gate_key: String = ""

func is_locked() -> bool:
	return gate_key != ""

func has_key() -> bool:
	return key_spawn != ""

func to_dict() -> Dictionary:
	return {
		"room_id": room_id,
		"room_type": RoomType.keys()[room_type].to_lower(),
		"grid_position": [grid_position.x, grid_position.y],
		"connections": connections,
		"key_spawn": key_spawn,
		"gate_key": gate_key,
	}
