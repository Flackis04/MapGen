package com.example.template

enum class Dir(val dx: Int, val dy: Int, val dz: Int) {
    N(0, 0, -1), S(0, 0, 1), E(1, 0, 0), W(-1, 0, 0),
    UP(0, 1, 0), DOWN(0, -1, 0);
    fun opposite() = when (this) { N -> S; S -> N; E -> W; W -> E; UP -> DOWN; DOWN -> UP }
    fun turns() = when (this) {
        N, S     -> listOf(E, W, UP, DOWN)
        E, W     -> listOf(N, S, UP, DOWN)
        UP, DOWN -> listOf(N, S, E, W)
    }
}

object Settings {
    const val BOX_SIZE = 11
    const val ROW_BOX_COUNT = 25
    const val HEIGHT_BOX_COUNT = 10
    const val HOLLOW_CHANCE = 0.05f
    const val ROOM_EXPAND_CHANCE = 0.12f
    const val MASSIVE_ROOM_EXPAND_CHANCE = 0.05f
    const val FINAL_SINGLE_ROOM_EXPAND_CHANCE = 0.06f
    const val TUNNELS_PER_ROOM_MAX = 2
    const val TUNNEL_MAX_LEN = 22
    const val TUNNEL_BRANCH_CHANCE = 0.55f
    const val TUNNEL_TURN_CHANCE = 0.60f
    const val TUNNEL_INTERSECTION_SEEK_CHANCE = 0.65f
    const val TUNNEL_INTERSECTION_SEEK_RANGE = 6
    const val TUNNEL_EXTRA_BRANCH_PASSES = 2
    const val TUNNEL_EXTRA_BRANCH_CHANCE = 0.18f
    const val PREVENT_REDUNDANT_PATHS = true
    const val SINGLE_ROOM_FILL_CHANCE = 0.05f
    const val TOWER_COUNT = 64
    val TOWER_HEIGHT_ROLLS = listOf(1, 1, 2, 2, 3, 3, 4, 5)
    const val WALL_THICKNESS = 1
    const val TUNNEL_MIN = 3
    const val TUNNEL_MAX = 7
    const val TUNNEL_FLOOR_Y = 1
    const val TUNNEL_CEILING_Y = 5
    const val ROOM_ENTRANCE_MIN = 4
    const val ROOM_ENTRANCE_MAX = 6
    const val ROOM_ENTRANCE_FLOOR_Y = 1
    const val ROOM_ENTRANCE_CEILING_Y = 3
    const val ROOM_ENTRANCE_LENGTH = 3

    var IS_BORDER_FLOOR = false
    var IS_SOUTH_BORDER = false
    var IS_NORTH_BORDER = false
    var IS_WEST_BORDER  = false
    var IS_EAST_BORDER  = false

    val isHollow = MutableList(ROW_BOX_COUNT) {
        MutableList(HEIGHT_BOX_COUNT) { MutableList(ROW_BOX_COUNT) { false } }
    }

    // Each cell stores the set of faces that are open (empty = solid, non-empty = connector)
    val connectorGrid = MutableList(ROW_BOX_COUNT) {
        MutableList(HEIGHT_BOX_COUNT) { MutableList(ROW_BOX_COUNT) { mutableSetOf<Dir>() } }
    }

    // Floor (DOWN) / ceiling (UP) openings punched through hollow-room faces by vertical tunnels
    val verticalOpenings = MutableList(ROW_BOX_COUNT) {
        MutableList(HEIGHT_BOX_COUNT) { MutableList(ROW_BOX_COUNT) { mutableSetOf<Dir>() } }
    }

    // Per-cell face rolls: 0 = open (no wall), 1 = solid wall.
    // Index: [0]=west [1]=east [2]=floor [3]=ceiling [4]=north [5]=south
    val faceRollsGrid = MutableList(ROW_BOX_COUNT) {
        MutableList(HEIGHT_BOX_COUNT) { MutableList(ROW_BOX_COUNT) { MutableList(6) { 1 } } }
    }

    // Mega-room group tracking. -1 = single-cell room (no group).
    val groupIdGrid = MutableList(ROW_BOX_COUNT) {
        MutableList(HEIGHT_BOX_COUNT) { MutableList(ROW_BOX_COUNT) { -1 } }
    }
    // Max tunnel connections allowed per group (keyed by group ID)
    val groupMaxConns = HashMap<Int, Int>()
}
