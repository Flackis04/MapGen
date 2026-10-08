package com.example.template

object Settings {
    const val BOX_SIZE = 11
    const val ROW_BOX_COUNT = 40
    const val HEIGHT_BOX_COUNT = 8

    const val WALL_THICKNESS = 1

    // Doorway cut into a wall: centered horizontally, standing on the floor
    const val DOOR_MIN = 4
    const val DOOR_MAX = 6
    const val DOOR_FLOOR_Y = 1
    const val DOOR_CEILING_Y = 3

    var IS_BORDER_FLOOR = false
    var IS_TOP_BORDER   = false
    var IS_SOUTH_BORDER = false
    var IS_NORTH_BORDER = false
    var IS_WEST_BORDER  = false
    var IS_EAST_BORDER  = false

    // grid[x][y][z] = true when a box has been placed in that cell
    val grid = Array(ROW_BOX_COUNT) { Array(HEIGHT_BOX_COUNT) { BooleanArray(ROW_BOX_COUNT) } }
}
