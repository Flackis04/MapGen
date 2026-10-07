package com.example.template

import org.bukkit.Location
import org.bukkit.Material

class HollowBox {

    // faceRolls: 0 = face left open, anything else = solid.
    //   [0]=west [1]=east [2]=floor [3]=ceiling [4]=north [5]=south
    // doorRolls: 0 = cut a doorway into that wall.
    //   [0]=north [1]=south [2]=west [3]=east
    // Border faces are always solid and never get doors.
    fun makeHollowBox(
        origin: Location, width: Int, height: Int, depth: Int,
        material: Material, faceRolls: List<Int>, doorRolls: List<Int>
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                for (z in 0 until depth) {
                    val inDoorSpanX  = x in Settings.DOOR_MIN..Settings.DOOR_MAX
                    val inDoorSpanZ  = z in Settings.DOOR_MIN..Settings.DOOR_MAX
                    val inDoorHeight = y in Settings.DOOR_FLOOR_Y..Settings.DOOR_CEILING_Y

                    val isDoorVoxel =
                        (inDoorSpanX && inDoorHeight && z == 0         && doorRolls[0] == 0 && !Settings.IS_NORTH_BORDER) ||
                        (inDoorSpanX && inDoorHeight && z == depth - 1 && doorRolls[1] == 0 && !Settings.IS_SOUTH_BORDER) ||
                        (inDoorSpanZ && inDoorHeight && x == 0         && doorRolls[2] == 0 && !Settings.IS_WEST_BORDER)  ||
                        (inDoorSpanZ && inDoorHeight && x == width - 1 && doorRolls[3] == 0 && !Settings.IS_EAST_BORDER)

                    val isSolidWestWall  = x == 0          && (faceRolls[0] != 0 || Settings.IS_WEST_BORDER)
                    val isSolidEastWall  = x == width - 1  && (faceRolls[1] != 0 || Settings.IS_EAST_BORDER)
                    val isSolidFloor     = y == 0          && (faceRolls[2] != 0 || Settings.IS_BORDER_FLOOR)
                    val isSolidCeiling   = y == height - 1 &&  faceRolls[3] != 0
                    val isSolidNorthWall = z == 0          && (faceRolls[4] != 0 || Settings.IS_NORTH_BORDER)
                    val isSolidSouthWall = z == depth - 1  && (faceRolls[5] != 0 || Settings.IS_SOUTH_BORDER)

                    if ((isSolidWestWall || isSolidEastWall || isSolidFloor || isSolidCeiling || isSolidNorthWall || isSolidSouthWall) && !isDoorVoxel) {
                        origin.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = material
                    }
                }
            }
        }
    }
}
