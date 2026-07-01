package com.example.template

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.WorldCreator
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.collections.List

class TemplatePlugin : JavaPlugin(), TabExecutor {
    private val cellStride = Settings.BOX_SIZE - Settings.WALL_THICKNESS

    override fun onEnable() {
        getCommand("mapgen")?.setExecutor(this)
        getCommand("mapgen")?.tabCompleter = this
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!sender.isServerOwner()) {
            sender.sendMessage("Only the server owner can run this command.")
            return true
        }

        if (args.size != 1) {
            sender.sendMessage("Usage: /$label <worldname>")
            return true
        }

        val worldName = args[0]
        if (!worldName.matches(Regex("[A-Za-z0-9_\\-]+"))) {
            sender.sendMessage("World names can only contain letters, numbers, underscores, and hyphens.")
            return true
        }

        val world = Bukkit.getWorld(worldName) ?: Bukkit.createWorld(WorldCreator(worldName))
        if (world == null) {
            sender.sendMessage("Could not create or load world '$worldName'.")
            return true
        }

        generateMap(worldName)
        sender.sendMessage("Generated map in world '$worldName'.")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (!sender.isServerOwner() || args.size != 1) return mutableListOf()

        return Bukkit.getWorlds()
            .map { it.name }
            .filter { it.startsWith(args[0], ignoreCase = true) }
            .toMutableList()
    }

    private fun CommandSender.isServerOwner(): Boolean {
        return this is ConsoleCommandSender || (this is Player && isOp)
    }

    private fun generateMap(worldName: String) {
        val world = Bukkit.getWorld(worldName) ?: return
        val origin = Location(world, 0.0, 0.0, 0.0)
        resetGenerationState()
        val doorRollsGrid = generateDoorRollsGrid()
        val doorTunnels = mutableListOf<DoorTunnel>()
        doorTunnels += generateVerticalTunnels(doorRollsGrid)

        //if x = 0 || x+totalwidth-1 || z = 0 || z+totalwidth-1 || y == 0 then that is the border wall/floor!
        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {

                    if (y == 0) Settings.IS_BORDER_FLOOR = true
                    else Settings.IS_BORDER_FLOOR = false

                    if (x == 0) Settings.IS_WEST_BORDER = true
                    else Settings.IS_WEST_BORDER = false
                    if (x == Settings.ROW_BOX_COUNT - Settings.WALL_THICKNESS) Settings.IS_EAST_BORDER = true
                    else Settings.IS_EAST_BORDER = false
                    if (z == 0) Settings.IS_NORTH_BORDER = true
                    else Settings.IS_NORTH_BORDER = false
                    if (z == Settings.ROW_BOX_COUNT - Settings.WALL_THICKNESS) Settings.IS_SOUTH_BORDER = true
                    else Settings.IS_SOUTH_BORDER = false

                    val doorRolls = doorRollsGrid[x][y][z]
                    doorTunnels += doorRolls.toDoorTunnels(x, y, z)
                    val hasTunnel = doorRolls.any { it == 0 }
                    val hasFloorTunnel = Dir.DOWN in Settings.verticalOpenings[x][y][z]
                    val hasCeilTunnel  = Dir.UP   in Settings.verticalOpenings[x][y][z]
                    val faceRolls = List(6) { i ->
                        when {
                            i == 2 && (hasTunnel || hasFloorTunnel) -> kotlin.random.Random.nextInt(1, 3)
                            i == 3 && hasCeilTunnel -> kotlin.random.Random.nextInt(1, 3)
                            else -> kotlin.random.Random.nextInt(0, 3)
                        }
                    }

                    HollowBox().makeHollowBox(
                        origin.clone().add(
                            (x * cellStride).toDouble(),
                            (y * cellStride).toDouble() - 60,
                            (z * cellStride).toDouble()
                        ),
                        Settings.BOX_SIZE,
                        Settings.BOX_SIZE,
                        Settings.BOX_SIZE,
                        Material.GRAY_CONCRETE,
                        faceRolls,
                        doorRolls,
                        Settings.verticalOpenings[x][y][z].toSet()
                    )
                }
                //else Settings.grid[x][y][z] = false
            }
        }

        doorTunnels.forEach { carveDoorTunnel(origin, it) }
    }

    private fun resetGenerationState() {
        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {
                    Settings.verticalOpenings[x][y][z].clear()
                }
            }
        }
    }

    private fun generateDoorRollsGrid(): List<List<List<List<Int>>>> {
        val rolls = MutableList(Settings.ROW_BOX_COUNT) {
            MutableList(Settings.HEIGHT_BOX_COUNT) {
                MutableList(Settings.ROW_BOX_COUNT) {
                    MutableList(4) { 1 }
                }
            }
        }

        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {
                    if (z < Settings.ROW_BOX_COUNT - 1) {
                        val roll = kotlin.random.Random.nextInt(0, 3)
                        rolls[x][y][z][1] = roll
                        rolls[x][y][z + 1][0] = roll
                    }

                    if (x < Settings.ROW_BOX_COUNT - 1) {
                        val roll = kotlin.random.Random.nextInt(0, 3)
                        rolls[x][y][z][3] = roll
                        rolls[x + 1][y][z][2] = roll
                    }
                }
            }
        }

        // Enforce max tunnels per box by closing excess doors on both sides of the shared wall
        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {
                    val open = (0..3).filter { rolls[x][y][z][it] == 0 }.toMutableList()
                    while (open.size > Settings.TUNNELS_PER_ROOM_MAX) {
                        val pick = open.removeAt(kotlin.random.Random.nextInt(open.size))
                        rolls[x][y][z][pick] = 1
                        when (pick) {
                            0 -> if (z > 0) rolls[x][y][z - 1][1] = 1
                            1 -> if (z < Settings.ROW_BOX_COUNT - 1) rolls[x][y][z + 1][0] = 1
                            2 -> if (x > 0) rolls[x - 1][y][z][3] = 1
                            3 -> if (x < Settings.ROW_BOX_COUNT - 1) rolls[x + 1][y][z][2] = 1
                        }
                    }
                }
            }
        }

        return rolls
    }

    private fun generateVerticalTunnels(doorRollsGrid: List<List<List<List<Int>>>>): List<DoorTunnel> {
        // Seed counts from horizontal tunnels already decided by generateDoorRollsGrid
        val count = Array(Settings.ROW_BOX_COUNT) { x ->
            Array(Settings.HEIGHT_BOX_COUNT) { y ->
                IntArray(Settings.ROW_BOX_COUNT) { z -> doorRollsGrid[x][y][z].count { it == 0 } }
            }
        }
        val tunnels = mutableListOf<DoorTunnel>()
        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 1 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {
                    if (kotlin.random.Random.nextInt(8) != 0) continue
                    // Both rooms must still be under the tunnel cap
                    if (count[x][y][z] >= Settings.TUNNELS_PER_ROOM_MAX) continue
                    if (count[x][y - 1][z] >= Settings.TUNNELS_PER_ROOM_MAX) continue
                    // Punch the floor of the upper room and ceiling of the lower room
                    Settings.verticalOpenings[x][y][z].add(Dir.DOWN)
                    Settings.verticalOpenings[x][y - 1][z].add(Dir.UP)
                    count[x][y][z]++
                    count[x][y - 1][z]++
                    val isLong = kotlin.random.Random.nextInt(10) == 0
                    if (kotlin.random.Random.nextBoolean()) {
                        tunnels += DoorTunnel(x, y, z, Dir.DOWN, isLong)
                    } else {
                        tunnels += DoorTunnel(x, y - 1, z, Dir.UP, isLong)
                    }
                }
            }
        }
        return tunnels
    }

    private fun List<Int>.toDoorTunnels(x: Int, y: Int, z: Int): List<DoorTunnel> {
        val tunnels = mutableListOf<DoorTunnel>()
        // Only emit S and E so each shared wall is processed exactly once
        if (this[1] == 0 && z < Settings.ROW_BOX_COUNT - 1)
            tunnels += DoorTunnel(x, y, z, Dir.S, kotlin.random.Random.nextInt(10) == 0)
        if (this[3] == 0 && x < Settings.ROW_BOX_COUNT - 1)
            tunnels += DoorTunnel(x, y, z, Dir.E, kotlin.random.Random.nextInt(10) == 0)
        return tunnels
    }

    private fun carveDoorTunnel(origin: Location, tunnel: DoorTunnel) {
        val roomOrigin = origin.clone().add(
            (tunnel.x * cellStride).toDouble(),
            (tunnel.y * cellStride).toDouble() - 60,
            (tunnel.z * cellStride).toDouble()
        )
        val entranceWidth = Settings.ROOM_ENTRANCE_MAX - Settings.ROOM_ENTRANCE_MIN + 1
        val entranceHeight = Settings.ROOM_ENTRANCE_CEILING_Y - Settings.ROOM_ENTRANCE_FLOOR_Y + 1
        val centerOffset = Settings.ROOM_ENTRANCE_LENGTH / 2
        val extensionOffsets = (-centerOffset until 0).toList() + (1..centerOffset).toList()

        when (tunnel.dir) {
            Dir.S -> {
                // Skip if south wall is missing (already open face)
                if (roomOrigin.clone().add(1.0, 5.0, (Settings.BOX_SIZE - 1).toDouble()).block.type == Material.AIR) return
                placeFrameSliceNS(roomOrigin, Settings.BOX_SIZE - 1)
                if (tunnel.isLong) {
                    val floorInside  = roomOrigin.clone().add(5.0, 0.0, (Settings.BOX_SIZE - 2).toDouble()).block.type != Material.AIR
                    val floorOutside = roomOrigin.clone().add(5.0, 0.0, Settings.BOX_SIZE.toDouble()).block.type != Material.AIR
                    if (floorInside && floorOutside) {
                        carveAirBox(
                            roomOrigin.clone().add(
                                Settings.ROOM_ENTRANCE_MIN.toDouble(),
                                Settings.ROOM_ENTRANCE_FLOOR_Y.toDouble(),
                                (Settings.BOX_SIZE - 1 - centerOffset).toDouble()
                            ),
                            entranceWidth, entranceHeight, Settings.ROOM_ENTRANCE_LENGTH
                        )
                        extensionOffsets.forEach { placeFrameSliceNS(roomOrigin, Settings.BOX_SIZE - 1 + it) }
                    }
                }
            }

            Dir.E -> {
                // Skip if east wall is missing (already open face)
                if (roomOrigin.clone().add((Settings.BOX_SIZE - 1).toDouble(), 5.0, 1.0).block.type == Material.AIR) return
                placeFrameSliceEW(roomOrigin, Settings.BOX_SIZE - 1)
                if (tunnel.isLong) {
                    val floorInside  = roomOrigin.clone().add((Settings.BOX_SIZE - 2).toDouble(), 0.0, 5.0).block.type != Material.AIR
                    val floorOutside = roomOrigin.clone().add(Settings.BOX_SIZE.toDouble(), 0.0, 5.0).block.type != Material.AIR
                    if (floorInside && floorOutside) {
                        carveAirBox(
                            roomOrigin.clone().add(
                                (Settings.BOX_SIZE - 1 - centerOffset).toDouble(),
                                Settings.ROOM_ENTRANCE_FLOOR_Y.toDouble(),
                                Settings.ROOM_ENTRANCE_MIN.toDouble()
                            ),
                            Settings.ROOM_ENTRANCE_LENGTH, entranceHeight, entranceWidth
                        )
                        extensionOffsets.forEach { placeFrameSliceEW(roomOrigin, Settings.BOX_SIZE - 1 + it) }
                    }
                }
            }

            // DOWN: shaft descends from the upper room's floor; long adds a 2-block chimney below
            Dir.DOWN -> {
                placeFrameSliceVertical(roomOrigin, 0)
                if (tunnel.isLong) {
                    placeFrameSliceVertical(roomOrigin, -1)
                    placeFrameSliceVertical(roomOrigin, -2)
                }
            }

            // UP: shaft rises from the lower room's ceiling; long adds a 2-block chimney above
            Dir.UP -> {
                placeFrameSliceVertical(roomOrigin, Settings.BOX_SIZE - 1)
                if (tunnel.isLong) {
                    placeFrameSliceVertical(roomOrigin, Settings.BOX_SIZE)
                    placeFrameSliceVertical(roomOrigin, Settings.BOX_SIZE + 1)
                }
            }

            else -> {}
        }
    }

    private fun placeFrameSliceNS(roomOrigin: Location, localZ: Int) {
        val min = Settings.ROOM_ENTRANCE_MIN
        val max = Settings.ROOM_ENTRANCE_MAX
        val floor = Settings.ROOM_ENTRANCE_FLOOR_Y
        val ceiling = Settings.ROOM_ENTRANCE_CEILING_Y
        for (y in floor..ceiling) {
            roomOrigin.clone().add((min - 1).toDouble(), y.toDouble(), localZ.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
            roomOrigin.clone().add((max + 1).toDouble(), y.toDouble(), localZ.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
        }
        for (x in (min - 1)..(max + 1))
            roomOrigin.clone().add(x.toDouble(), (ceiling + 1).toDouble(), localZ.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
    }

    private fun placeFrameSliceEW(roomOrigin: Location, localX: Int) {
        val min = Settings.ROOM_ENTRANCE_MIN
        val max = Settings.ROOM_ENTRANCE_MAX
        val floor = Settings.ROOM_ENTRANCE_FLOOR_Y
        val ceiling = Settings.ROOM_ENTRANCE_CEILING_Y
        for (y in floor..ceiling) {
            roomOrigin.clone().add(localX.toDouble(), y.toDouble(), (min - 1).toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
            roomOrigin.clone().add(localX.toDouble(), y.toDouble(), (max + 1).toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
        }
        for (z in (min - 1)..(max + 1))
            roomOrigin.clone().add(localX.toDouble(), (ceiling + 1).toDouble(), z.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
    }

    private fun placeFrameSliceVertical(roomOrigin: Location, localY: Int) {
        val min = Settings.ROOM_ENTRANCE_MIN
        val max = Settings.ROOM_ENTRANCE_MAX
        for (z in min..max) {
            roomOrigin.clone().add((min - 1).toDouble(), localY.toDouble(), z.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
            roomOrigin.clone().add((max + 1).toDouble(), localY.toDouble(), z.toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
        }
        for (x in (min - 1)..(max + 1)) {
            roomOrigin.clone().add(x.toDouble(), localY.toDouble(), (min - 1).toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
            roomOrigin.clone().add(x.toDouble(), localY.toDouble(), (max + 1).toDouble()).block.type = Material.LIGHT_BLUE_CONCRETE
        }
    }

    private fun carveAirBox(origin: Location, width: Int, height: Int, depth: Int) {
        for (x in 0 until width) {
            for (y in 0 until height) {
                for (z in 0 until depth) {
                    origin.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block.type = Material.AIR
                }
            }
        }
    }

    private data class DoorTunnel(val x: Int, val y: Int, val z: Int, val dir: Dir, val isLong: Boolean)
}
