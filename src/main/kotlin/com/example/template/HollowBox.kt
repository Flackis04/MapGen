package com.example.template

import org.bukkit.Location
import org.bukkit.Material

class HollowBox {

    // Each open face contributes a half-corridor; the union of all of them is the air region.
    private fun isHalfCorridor(
        face: Dir,
        openFaces: Set<Dir>,
        roomFaces: Set<Dir>,
        width: Int,
        height: Int,
        depth: Int,
        x: Int,
        y: Int,
        z: Int
    ): Boolean {
        val hasHorizontal = openFaces.any { it == Dir.N || it == Dir.S || it == Dir.E || it == Dir.W }
        val usesRoomProfile = face in roomFaces && isWithinRoomEntranceLength(face, width, height, depth, x, y, z)
        val min = if (usesRoomProfile) Settings.ROOM_ENTRANCE_MIN else Settings.TUNNEL_MIN
        val max = if (usesRoomProfile) Settings.ROOM_ENTRANCE_MAX else Settings.TUNNEL_MAX
        val floorY = if (usesRoomProfile) Settings.ROOM_ENTRANCE_FLOOR_Y else Settings.TUNNEL_FLOOR_Y
        val ceilingY = if (usesRoomProfile) Settings.ROOM_ENTRANCE_CEILING_Y else Settings.TUNNEL_CEILING_Y
        val tunnelX = x in min..max
        val tunnelY = y in floorY..ceilingY
        val tunnelZ = z in min..max

        return when (face) {
            Dir.N    -> tunnelX && z <= Settings.TUNNEL_MAX && tunnelY
            Dir.S    -> tunnelX && z >= Settings.TUNNEL_MIN && tunnelY
            Dir.E    -> tunnelZ && x >= Settings.TUNNEL_MIN && tunnelY
            Dir.W    -> tunnelZ && x <= Settings.TUNNEL_MAX && tunnelY
            Dir.UP   -> tunnelX && tunnelZ && y >= if (hasHorizontal) ceilingY else tunnelMidpoint()
            Dir.DOWN -> tunnelX && tunnelZ && y <= if (hasHorizontal) ceilingY else tunnelMidpoint()
        }
    }

    private fun tunnelMidpoint() = (Settings.TUNNEL_MIN + Settings.TUNNEL_MAX) / 2

    private fun isWithinRoomEntranceLength(
        face: Dir,
        width: Int,
        height: Int,
        depth: Int,
        x: Int,
        y: Int,
        z: Int
    ) = when (face) {
        Dir.N -> z < Settings.ROOM_ENTRANCE_LENGTH
        Dir.S -> z >= depth - Settings.ROOM_ENTRANCE_LENGTH
        Dir.E -> x >= width - Settings.ROOM_ENTRANCE_LENGTH
        Dir.W -> x < Settings.ROOM_ENTRANCE_LENGTH
        Dir.UP -> y >= height - Settings.ROOM_ENTRANCE_LENGTH
        Dir.DOWN -> y < Settings.ROOM_ENTRANCE_LENGTH
    }

    fun makeConnectorBox(
        origin: Location, width: Int, height: Int, depth: Int,
        material: Material, openFaces: Set<Dir>, roomFaces: Set<Dir> = emptySet()
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                for (z in 0 until depth) {
                    val isAir = openFaces.any { isHalfCorridor(it, openFaces, roomFaces, width, height, depth, x, y, z) }
                    origin.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                        .block.type = if (isAir) Material.AIR else material
                }
            }
        }
    }

    fun makeSolidBox(origin: Location, width: Int, height: Int, depth: Int, material: Material) {
        for (x in 0 until width)
            for (y in 0 until height)
                for (z in 0 until depth)
                    origin.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = material
    }

    fun makeHollowBox(
        origin: Location, width: Int, height: Int, depth: Int,
        material: Material, faceRolls: List<Int>, doorRolls: List<Int>,
        verticalOpenings: Set<Dir> = emptySet()
    ) {
        val hasFloorOpening   = Dir.DOWN in verticalOpenings
        val hasCeilingOpening = Dir.UP   in verticalOpenings

        for (x in 0 until width) {
            for (y in 0 until height) {
                for (z in 0 until depth) {
                    val entranceY = y in Settings.ROOM_ENTRANCE_FLOOR_Y..Settings.ROOM_ENTRANCE_CEILING_Y
                    val entranceX = x in Settings.ROOM_ENTRANCE_MIN..Settings.ROOM_ENTRANCE_MAX
                    val entranceZ = z in Settings.ROOM_ENTRANCE_MIN..Settings.ROOM_ENTRANCE_MAX
                    val tunnelSpot = entranceX && entranceZ
                    val entranceDepthOffset = Settings.ROOM_ENTRANCE_LENGTH / 2

                    val isDoor =
                        (entranceX && entranceY && z <= entranceDepthOffset         && doorRolls[0] == 0 && !Settings.IS_NORTH_BORDER) ||
                        (entranceX && entranceY && z >= depth - 1 - entranceDepthOffset && doorRolls[1] == 0 && !Settings.IS_SOUTH_BORDER) ||
                        (entranceZ && entranceY && x <= entranceDepthOffset         && doorRolls[2] == 0 && !Settings.IS_WEST_BORDER)  ||
                        (entranceZ && entranceY && x >= width - 1 - entranceDepthOffset && doorRolls[3] == 0 && !Settings.IS_EAST_BORDER)

                    val leftFace   = x == 0         && (faceRolls[0] != 0 || Settings.IS_WEST_BORDER)
                    val rightFace  = x == width - 1  && (faceRolls[1] != 0 || Settings.IS_EAST_BORDER)
                    val bottomFace = y == 0          && (faceRolls[2] != 0 || Settings.IS_BORDER_FLOOR) && !(hasFloorOpening && tunnelSpot)
                    val topFace    = y == height - 1 && faceRolls[3] != 0 && !(hasCeilingOpening && tunnelSpot)
                    val frontFace  = z == 0          && (faceRolls[4] != 0 || Settings.IS_NORTH_BORDER)
                    val backFace   = z == depth - 1  && (faceRolls[5] != 0 || Settings.IS_SOUTH_BORDER)

                    if ((leftFace || rightFace || bottomFace || topFace || frontFace || backFace) && !isDoor) {
                        origin.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = material
                    }
                }
            }
        }
    }
}
