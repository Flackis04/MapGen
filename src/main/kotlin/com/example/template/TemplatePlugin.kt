package com.example.template

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private typealias DoorRollGrid = MutableList<MutableList<MutableList<MutableList<Int>>>>

class TemplatePlugin : JavaPlugin() {

    private var nextGroupId = 0
    private val groupConns = HashMap<Int, Int>()

    // Box-Muller: returns an integer drawn from N(mean, std^2) clamped to [min, max].
    private fun gaussianLength(mean: Double, std: Double, min: Int, max: Int): Int {
        val u1 = Random.nextDouble().coerceAtLeast(1e-10)
        val u2 = Random.nextDouble()
        val z = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
        return (mean + std * z).roundToInt().coerceIn(min, max)
    }

    private fun dirToIdx(dir: Dir) = when (dir) {
        Dir.N -> 0
        Dir.S -> 1
        Dir.W -> 2
        Dir.E -> 3
        else -> -1
    }

    private fun isInGrid(x: Int, y: Int, z: Int) =
        x in 0 until Settings.ROW_BOX_COUNT &&
            y in 0 until Settings.HEIGHT_BOX_COUNT &&
            z in 0 until Settings.ROW_BOX_COUNT

    private fun isInInteriorGrid(x: Int, y: Int, z: Int) =
        x in 1 until Settings.ROW_BOX_COUNT - 1 &&
            y in 0 until Settings.HEIGHT_BOX_COUNT &&
            z in 1 until Settings.ROW_BOX_COUNT - 1

    private fun isVertical(dir: Dir) = dir == Dir.UP || dir == Dir.DOWN

    private fun roomFacesForConnector(x: Int, y: Int, z: Int, openFaces: Set<Dir>): Set<Dir> =
        openFaces.filterTo(mutableSetOf()) { face ->
            val nx = x + face.dx
            val ny = y + face.dy
            val nz = z + face.dz
            isInGrid(nx, ny, nz) && Settings.isHollow[nx][ny][nz]
        }

    private fun isRoomConnectionOpen(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int, face: Dir): Boolean {
        if (!isInGrid(x, y, z)) return false

        return when (face) {
            Dir.UP, Dir.DOWN -> face in Settings.verticalOpenings[x][y][z]
            else -> {
                val idx = dirToIdx(face)
                idx >= 0 && doorRolls[x][y][z][idx] == 0
            }
        }
    }

    private fun roomConnectionCount(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int): Int {
        val gid = Settings.groupIdGrid[x][y][z]
        if (gid < 0) {
            return doorRolls[x][y][z].count { it == 0 } + Settings.verticalOpenings[x][y][z].size
        }

        var conns = 0
        for (rx in 1 until Settings.ROW_BOX_COUNT - 1)
            for (ry in 0 until Settings.HEIGHT_BOX_COUNT)
                for (rz in 1 until Settings.ROW_BOX_COUNT - 1)
                    if (Settings.groupIdGrid[rx][ry][rz] == gid)
                        conns += doorRolls[rx][ry][rz].count { it == 0 } + Settings.verticalOpenings[rx][ry][rz].size
        return conns
    }

    private fun canOpenRoomConnection(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int, face: Dir): Boolean {
        if (!isInGrid(x, y, z)) return false
        if (!Settings.isHollow[x][y][z]) return false
        if (isRoomConnectionOpen(doorRolls, x, y, z, face)) return true
        return roomConnectionCount(doorRolls, x, y, z) < Settings.TUNNELS_PER_ROOM_MAX
    }

    private fun faceRollIdx(dir: Dir) = when (dir) {
        Dir.W -> 0
        Dir.E -> 1
        Dir.DOWN -> 2
        Dir.UP -> 3
        Dir.N -> 4
        Dir.S -> 5
    }

    private fun isTraversableCell(x: Int, y: Int, z: Int) =
        isInGrid(x, y, z) &&
            (Settings.connectorGrid[x][y][z].isNotEmpty() || Settings.isHollow[x][y][z])

    private fun hasOpenFace(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int, face: Dir): Boolean {
        if (!isTraversableCell(x, y, z)) return false

        if (Settings.connectorGrid[x][y][z].isNotEmpty()) {
            return face in Settings.connectorGrid[x][y][z]
        }

        return isRoomConnectionOpen(doorRolls, x, y, z, face) ||
            Settings.faceRollsGrid[x][y][z][faceRollIdx(face)] == 0
    }

    private fun alreadyConnected(
        doorRolls: DoorRollGrid,
        fromX: Int,
        fromY: Int,
        fromZ: Int,
        toX: Int,
        toY: Int,
        toZ: Int
    ): Boolean {
        if (!Settings.PREVENT_REDUNDANT_PATHS) return false
        if (!isTraversableCell(fromX, fromY, fromZ) || !isTraversableCell(toX, toY, toZ)) return false

        val target = Triple(toX, toY, toZ)
        val queue = ArrayDeque<Triple<Int, Int, Int>>()
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        queue.add(Triple(fromX, fromY, fromZ))

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            if (current == target) return true

            val (x, y, z) = current
            for (face in Dir.entries) {
                if (!hasOpenFace(doorRolls, x, y, z, face)) continue

                val nx = x + face.dx
                val ny = y + face.dy
                val nz = z + face.dz
                if (!isTraversableCell(nx, ny, nz)) continue
                if (!hasOpenFace(doorRolls, nx, ny, nz, face.opposite())) continue

                queue.add(Triple(nx, ny, nz))
            }
        }

        return false
    }

    private fun connectorDistanceInDirection(
        x: Int,
        y: Int,
        z: Int,
        dir: Dir,
        visited: Set<Triple<Int, Int, Int>>
    ): Int? {
        for (distance in 1..Settings.TUNNEL_INTERSECTION_SEEK_RANGE) {
            val nx = x + dir.dx * distance
            val ny = y + dir.dy * distance
            val nz = z + dir.dz * distance
            if (!isInInteriorGrid(nx, ny, nz)) return null
            if (Triple(nx, ny, nz) in visited) return null
            if (Settings.connectorGrid[nx][ny][nz].isNotEmpty()) return distance
        }
        return null
    }

    private fun chooseTunnelExit(
        dir: Dir,
        step: Int,
        maxLen: Int,
        cx: Int,
        cy: Int,
        cz: Int,
        visited: Set<Triple<Int, Int, Int>>
    ): Dir {
        if (step >= maxLen - 1) return dir
        if (step == 0 && isVertical(dir)) return dir

        val options = (listOf(dir) + dir.turns()).distinct().filter {
            val nx = cx + it.dx
            val ny = cy + it.dy
            val nz = cz + it.dz
            isInInteriorGrid(nx, ny, nz) && Triple(nx, ny, nz) !in visited
        }
        if (options.isEmpty()) return dir

        if (Random.nextFloat() < Settings.TUNNEL_INTERSECTION_SEEK_CHANCE) {
            val connectorOptions = options.mapNotNull {
                val distance = connectorDistanceInDirection(cx, cy, cz, it, visited)
                if (distance == null) null else it to distance
            }
            val nearest = connectorOptions.minOfOrNull { it.second }
            if (nearest != null) {
                return connectorOptions.filter { it.second == nearest }.random().first
            }
        }

        if (Random.nextFloat() >= Settings.TUNNEL_TURN_CHANCE) return dir
        return options.randomOrNull() ?: dir
    }

    private fun openRoomConnection(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int, face: Dir): Boolean {
        if (!canOpenRoomConnection(doorRolls, x, y, z, face)) return false

        when (face) {
            Dir.UP, Dir.DOWN -> Settings.verticalOpenings[x][y][z].add(face)
            else -> {
                val idx = dirToIdx(face)
                if (idx >= 0) doorRolls[x][y][z][idx] = 0
            }
        }
        return true
    }

    private fun closeRoomConnection(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int, face: Dir) {
        if (!isInGrid(x, y, z)) return

        when (face) {
            Dir.UP, Dir.DOWN -> Settings.verticalOpenings[x][y][z].remove(face)
            else -> {
                val idx = dirToIdx(face)
                if (idx >= 0) doorRolls[x][y][z][idx] = 1
            }
        }
    }

    // Walks and commits a tunnel branch starting at (startX, startY, startZ).
    private fun growBranch(
        doorRolls: DoorRollGrid,
        startX: Int,
        startY: Int,
        startZ: Int,
        travelDir: Dir
    ): Boolean {
        var cx = startX
        var cy = startY
        var cz = startZ
        var dir = travelDir
        val originX = startX - travelDir.dx
        val originY = startY - travelDir.dy
        val originZ = startZ - travelDir.dz

        if (!isInInteriorGrid(cx, cy, cz)) return false
        if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) {
            if (alreadyConnected(doorRolls, originX, originY, originZ, cx, cy, cz)) return false
            Settings.connectorGrid[cx][cy][cz].add(travelDir.opposite())
            return true
        }
        if (Settings.isHollow[cx][cy][cz]) {
            if (alreadyConnected(doorRolls, originX, originY, originZ, cx, cy, cz)) return false
            return openRoomConnection(doorRolls, cx, cy, cz, travelDir.opposite())
        }

        val cells = mutableListOf<Triple<Int, Int, Int>>()
        val entries = mutableListOf<Dir>()
        val exits = mutableListOf<Dir>()
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        var reachedRoom = false
        var reachedConnector = false
        var selfLoop = false

        val maxLen = gaussianLength(6.0, 4.0, 2, Settings.TUNNEL_MAX_LEN / 2 + 3)
        for (step in 0 until maxLen) {
            if (!isInInteriorGrid(cx, cy, cz)) break
            if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) {
                reachedConnector = true
                break
            }
            if (Settings.isHollow[cx][cy][cz]) {
                reachedRoom = true
                break
            }
            if (!visited.add(Triple(cx, cy, cz))) {
                selfLoop = true
                break
            }

            val entry = dir.opposite()
            val exit = chooseTunnelExit(dir, step, maxLen, cx, cy, cz, visited)

            cells += Triple(cx, cy, cz)
            entries += entry
            exits += exit

            cx += exit.dx
            cy += exit.dy
            cz += exit.dz
            dir = exit
        }

        if (selfLoop || cells.isEmpty()) return false

        val finalFace = exits.last().opposite()
        if (reachedRoom && !canOpenRoomConnection(doorRolls, cx, cy, cz, finalFace)) return false
        if ((reachedRoom || reachedConnector) &&
            alreadyConnected(doorRolls, originX, originY, originZ, cx, cy, cz)
        ) return false

        if (!reachedRoom && !reachedConnector) {
            if (!isInInteriorGrid(cx, cy, cz)) return false
            if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) return false
            Settings.isHollow[cx][cy][cz] = true
        }

        for (i in cells.indices) {
            val (px, py, pz) = cells[i]
            Settings.connectorGrid[px][py][pz].add(entries[i])
            Settings.connectorGrid[px][py][pz].add(exits[i])
        }

        if (reachedConnector) {
            Settings.connectorGrid[cx][cy][cz].add(finalFace)
        } else {
            openRoomConnection(doorRolls, cx, cy, cz, finalFace)
        }
        return true
    }

    private fun growTunnel(
        doorRolls: DoorRollGrid,
        startX: Int,
        startY: Int,
        startZ: Int,
        startDir: Dir
    ): Boolean {
        var cx = startX + startDir.dx
        var cy = startY + startDir.dy
        var cz = startZ + startDir.dz
        var dir = startDir

        if (!canOpenRoomConnection(doorRolls, startX, startY, startZ, startDir)) return false
        if (!isInInteriorGrid(cx, cy, cz)) return false
        if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) {
            if (alreadyConnected(doorRolls, startX, startY, startZ, cx, cy, cz)) return false
            openRoomConnection(doorRolls, startX, startY, startZ, startDir)
            Settings.connectorGrid[cx][cy][cz].add(startDir.opposite())
            return true
        }
        if (Settings.isHollow[cx][cy][cz]) {
            if (!canOpenRoomConnection(doorRolls, cx, cy, cz, startDir.opposite())) return false
            if (alreadyConnected(doorRolls, startX, startY, startZ, cx, cy, cz)) return false
            openRoomConnection(doorRolls, startX, startY, startZ, startDir)
            openRoomConnection(doorRolls, cx, cy, cz, startDir.opposite())
            return true
        }

        val cells = mutableListOf<Triple<Int, Int, Int>>()
        val entries = mutableListOf<Dir>()
        val exits = mutableListOf<Dir>()
        val visited = mutableSetOf<Triple<Int, Int, Int>>()
        var reachedRoom = false
        var reachedConnector = false
        var selfLoop = false

        val maxLen = gaussianLength(9.0, 5.5, 3, Settings.TUNNEL_MAX_LEN)
        for (step in 0 until maxLen) {
            if (!isInInteriorGrid(cx, cy, cz)) break
            if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) {
                reachedConnector = true
                break
            }
            if (Settings.isHollow[cx][cy][cz]) {
                reachedRoom = true
                break
            }
            if (!visited.add(Triple(cx, cy, cz))) {
                selfLoop = true
                break
            }

            val entry = dir.opposite()
            val exit = chooseTunnelExit(dir, step, maxLen, cx, cy, cz, visited)

            cells += Triple(cx, cy, cz)
            entries += entry
            exits += exit

            cx += exit.dx
            cy += exit.dy
            cz += exit.dz
            dir = exit
        }

        if (selfLoop || cells.isEmpty()) return false

        val finalFace = exits.last().opposite()
        if (reachedRoom && !canOpenRoomConnection(doorRolls, cx, cy, cz, finalFace)) return false
        if ((reachedRoom || reachedConnector) &&
            alreadyConnected(doorRolls, startX, startY, startZ, cx, cy, cz)
        ) return false

        if (!reachedRoom && !reachedConnector) {
            if (!isInInteriorGrid(cx, cy, cz)) return false
            if (Settings.connectorGrid[cx][cy][cz].isNotEmpty()) return false
            Settings.isHollow[cx][cy][cz] = true
        }

        for (i in cells.indices) {
            val (px, py, pz) = cells[i]
            Settings.connectorGrid[px][py][pz].add(entries[i])
            Settings.connectorGrid[px][py][pz].add(exits[i])
        }

        openRoomConnection(doorRolls, startX, startY, startZ, startDir)
        if (reachedConnector) {
            Settings.connectorGrid[cx][cy][cz].add(finalFace)
        } else {
            openRoomConnection(doorRolls, cx, cy, cz, finalFace)
        }

        for (i in 1 until cells.size - 1) {
            if (Random.nextFloat() >= Settings.TUNNEL_BRANCH_CHANCE) continue
            val (bx, by, bz) = cells[i]
            val used = Settings.connectorGrid[bx][by][bz]
            val candidate = Dir.entries.filter { it !in used }.randomOrNull() ?: continue

            val nx = bx + candidate.dx
            val ny = by + candidate.dy
            val nz = bz + candidate.dz
            if (!isInInteriorGrid(nx, ny, nz)) continue

            if (growBranch(doorRolls, nx, ny, nz, candidate)) {
                Settings.connectorGrid[bx][by][bz].add(candidate)
            }
        }

        return true
    }

    private fun chooseExtraBranchDir(
        x: Int,
        y: Int,
        z: Int,
        used: Set<Dir>
    ): Dir? {
        val candidates = Dir.entries.filter {
            it !in used && isInInteriorGrid(x + it.dx, y + it.dy, z + it.dz)
        }
        if (candidates.isEmpty()) return null

        val connectorOptions = candidates.mapNotNull {
            val distance = connectorDistanceInDirection(x, y, z, it, emptySet())
            if (distance == null) null else it to distance
        }
        val nearest = connectorOptions.minOfOrNull { it.second }
        if (nearest != null && Random.nextFloat() < Settings.TUNNEL_INTERSECTION_SEEK_CHANCE) {
            return connectorOptions.filter { it.second == nearest }.random().first
        }

        return candidates.random()
    }

    private fun growExtraConnectorBranches(doorRolls: DoorRollGrid) {
        repeat(Settings.TUNNEL_EXTRA_BRANCH_PASSES) {
            val connectors = mutableListOf<Triple<Int, Int, Int>>()
            for (x in 1 until Settings.ROW_BOX_COUNT - 1)
                for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                    for (z in 1 until Settings.ROW_BOX_COUNT - 1)
                        if (Settings.connectorGrid[x][y][z].isNotEmpty())
                            connectors += Triple(x, y, z)

            for ((x, y, z) in connectors.shuffled()) {
                if (Random.nextFloat() >= Settings.TUNNEL_EXTRA_BRANCH_CHANCE) continue
                val used = Settings.connectorGrid[x][y][z]
                val candidate = chooseExtraBranchDir(x, y, z, used) ?: continue

                if (growBranch(doorRolls, x + candidate.dx, y + candidate.dy, z + candidate.dz, candidate)) {
                    used.add(candidate)
                }
            }
        }
    }

    private fun pruneDanglingConnector(doorRolls: DoorRollGrid, startX: Int, startY: Int, startZ: Int) {
        val queue = ArrayDeque<Triple<Int, Int, Int>>()
        queue.add(Triple(startX, startY, startZ))

        while (queue.isNotEmpty()) {
            val (x, y, z) = queue.removeFirst()
            if (!isInGrid(x, y, z)) continue
            val faces = Settings.connectorGrid[x][y][z].toList()
            if (faces.size > 1) continue

            Settings.connectorGrid[x][y][z].clear()
            for (face in faces) {
                val nx = x + face.dx
                val ny = y + face.dy
                val nz = z + face.dz
                if (!isInGrid(nx, ny, nz)) continue

                if (Settings.connectorGrid[nx][ny][nz].remove(face.opposite())) {
                    queue.add(Triple(nx, ny, nz))
                }
                closeRoomConnection(doorRolls, nx, ny, nz, face.opposite())
            }
        }
    }

    private fun fillSingleRoom(doorRolls: DoorRollGrid, x: Int, y: Int, z: Int) {
        for (face in Dir.entries) {
            closeRoomConnection(doorRolls, x, y, z, face)

            val nx = x + face.dx
            val ny = y + face.dy
            val nz = z + face.dz
            if (!isInGrid(nx, ny, nz)) continue

            if (Settings.connectorGrid[nx][ny][nz].remove(face.opposite())) {
                pruneDanglingConnector(doorRolls, nx, ny, nz)
            }
            closeRoomConnection(doorRolls, nx, ny, nz, face.opposite())
        }

        Settings.isHollow[x][y][z] = false
        Settings.groupIdGrid[x][y][z] = -1
        Settings.connectorGrid[x][y][z].clear()
        Settings.verticalOpenings[x][y][z].clear()
        doorRolls[x][y][z].fill(1)
        Settings.faceRollsGrid[x][y][z].fill(1)
    }

    private fun fillLeftoverSingleRooms(doorRolls: DoorRollGrid) {
        repeat(2) {
            for (x in 1 until Settings.ROW_BOX_COUNT - 1)
                for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                    for (z in 1 until Settings.ROW_BOX_COUNT - 1) {
                        if (!Settings.isHollow[x][y][z]) continue
                        if (Settings.groupIdGrid[x][y][z] >= 0) continue
                        if (Settings.connectorGrid[x][y][z].isNotEmpty()) continue

                        val conns = roomConnectionCount(doorRolls, x, y, z)
                        if (conns == 0 || (conns == 1 && Random.nextFloat() < Settings.SINGLE_ROOM_FILL_CHANCE)) {
                            fillSingleRoom(doorRolls, x, y, z)
                        }
                    }
        }
    }

    // Expands the hollow room at (x, y, z) into a rectangular multi-cell room.
    private fun tryExpandToMegaRoom(
        x: Int,
        y: Int,
        z: Int,
        maxW: Int,
        maxH: Int,
        maxD: Int,
        rolls: Int = 1,
        attempts: Int = 1
    ) {
        fun pick(max: Int) = (1..rolls).maxOf { Random.nextInt(1, max + 1) }

        repeat(attempts) {
            val w = pick(maxW)
            val h = pick(maxH)
            val d = pick(maxD)
            if (w == 1 && h == 1 && d == 1) return@repeat

            val ox = x - Random.nextInt(w)
            val oy = y - Random.nextInt(h)
            val oz = z - Random.nextInt(d)

            val valid = (0 until w).all { dx ->
                (0 until h).all { dy ->
                    (0 until d).all { dz ->
                        val nx = ox + dx
                        val ny = oy + dy
                        val nz = oz + dz
                        isInInteriorGrid(nx, ny, nz) &&
                            Settings.connectorGrid[nx][ny][nz].isEmpty() &&
                            ((nx == x && ny == y && nz == z) || !Settings.isHollow[nx][ny][nz])
                    }
                }
            }

            if (!valid) return@repeat

            val gid = nextGroupId++
            Settings.groupMaxConns[gid] = Settings.TUNNELS_PER_ROOM_MAX
            groupConns[gid] = 0

            for (dx in 0 until w) for (dy in 0 until h) for (dz in 0 until d) {
                val nx = ox + dx
                val ny = oy + dy
                val nz = oz + dz
                Settings.isHollow[nx][ny][nz] = true
                Settings.groupIdGrid[nx][ny][nz] = gid
                val f = Settings.faceRollsGrid[nx][ny][nz]
                if (dx > 0) f[0] = 0
                if (dx < w - 1) f[1] = 0
                if (dy > 0) f[2] = 0
                if (dy < h - 1) f[3] = 0
                if (dz > 0) f[4] = 0
                if (dz < d - 1) f[5] = 0
            }
            return
        }
    }

    override fun onEnable() {
        val world = Bukkit.getWorld("gens") ?: return
        val origin = Location(world, 0.0, 0.0, 0.0)

        val doorRollsGrid = MutableList(Settings.ROW_BOX_COUNT) {
            MutableList(Settings.HEIGHT_BOX_COUNT) {
                MutableList(Settings.ROW_BOX_COUNT) { MutableList(4) { 1 } }
            }
        }

        for (x in 1 until Settings.ROW_BOX_COUNT - 1)
            for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                for (z in 1 until Settings.ROW_BOX_COUNT - 1)
                    if (Random.nextFloat() < Settings.HOLLOW_CHANCE)
                        Settings.isHollow[x][y][z] = true

        for (x in 1 until Settings.ROW_BOX_COUNT - 1)
            for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                for (z in 1 until Settings.ROW_BOX_COUNT - 1)
                    if (Settings.isHollow[x][y][z] && Random.nextFloat() < Settings.ROOM_EXPAND_CHANCE)
                        tryExpandToMegaRoom(x, y, z, 4, 2, 4, rolls = 2, attempts = 6)

        for (x in 1 until Settings.ROW_BOX_COUNT - 1) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 1 until Settings.ROW_BOX_COUNT - 1) {
                    if (!Settings.isHollow[x][y][z]) continue
                    val gid = Settings.groupIdGrid[x][y][z]
                    val maxConns = if (gid >= 0) {
                        Settings.groupMaxConns.getOrDefault(gid, Settings.TUNNELS_PER_ROOM_MAX)
                    } else {
                        Random.nextInt(1, Settings.TUNNELS_PER_ROOM_MAX + 1)
                    }
                    if (gid >= 0 && groupConns.getOrDefault(gid, 0) >= maxConns) continue

                    val dirs = Dir.entries.shuffled()
                    var placed = 0
                    for (d in dirs) {
                        val used = if (gid >= 0) groupConns.getOrDefault(gid, 0) else placed
                        if (used >= maxConns) break
                        if (growTunnel(doorRollsGrid, x, y, z, d)) {
                            placed++
                            if (gid >= 0) groupConns[gid] = groupConns.getOrDefault(gid, 0) + 1
                        }
                    }
                }
            }
        }

        growExtraConnectorBranches(doorRollsGrid)

        for (x in 1 until Settings.ROW_BOX_COUNT - 1)
            for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                for (z in 1 until Settings.ROW_BOX_COUNT - 1) {
                    if (!Settings.isHollow[x][y][z]) continue
                    val conns = roomConnectionCount(doorRollsGrid, x, y, z)
                    if (conns == 0 && Random.nextFloat() < Settings.MASSIVE_ROOM_EXPAND_CHANCE)
                        tryExpandToMegaRoom(x, y, z, 6, 3, 6, rolls = 3, attempts = 8)
                }

        for (x in 1 until Settings.ROW_BOX_COUNT - 1)
            for (y in 0 until Settings.HEIGHT_BOX_COUNT)
                for (z in 1 until Settings.ROW_BOX_COUNT - 1) {
                    if (!Settings.isHollow[x][y][z]) continue
                    if (Settings.groupIdGrid[x][y][z] >= 0) continue
                    if (roomConnectionCount(doorRollsGrid, x, y, z) == 0 &&
                        Random.nextFloat() < Settings.FINAL_SINGLE_ROOM_EXPAND_CHANCE
                    ) {
                        tryExpandToMegaRoom(x, y, z, 4, 2, 4, rolls = 2, attempts = 5)
                    }
                }

        fillLeftoverSingleRooms(doorRollsGrid)

        for (x in 0 until Settings.ROW_BOX_COUNT) {
            for (y in 0 until Settings.HEIGHT_BOX_COUNT) {
                for (z in 0 until Settings.ROW_BOX_COUNT) {
                    Settings.IS_BORDER_FLOOR = y == 0
                    Settings.IS_WEST_BORDER = x == 0
                    Settings.IS_EAST_BORDER = x == Settings.ROW_BOX_COUNT - Settings.WALL_THICKNESS
                    Settings.IS_NORTH_BORDER = z == 0
                    Settings.IS_SOUTH_BORDER = z == Settings.ROW_BOX_COUNT - Settings.WALL_THICKNESS

                    val cellOrigin = origin.clone().add(
                        (x * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble(),
                        (y * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble() - 60,
                        (z * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble()
                    )

                    val openFaces = Settings.connectorGrid[x][y][z]
                    when {
                        openFaces.isNotEmpty() -> HollowBox().makeConnectorBox(
                            cellOrigin,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Material.GRAY_CONCRETE,
                            openFaces,
                            roomFacesForConnector(x, y, z, openFaces)
                        )
                        Settings.isHollow[x][y][z] -> HollowBox().makeHollowBox(
                            cellOrigin,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Material.GRAY_CONCRETE,
                            Settings.faceRollsGrid[x][y][z],
                            doorRollsGrid[x][y][z],
                            Settings.verticalOpenings[x][y][z]
                        )
                        else -> HollowBox().makeSolidBox(
                            cellOrigin,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Settings.BOX_SIZE,
                            Material.GRAY_CONCRETE
                        )
                    }
                }
            }
        }
    }
}
