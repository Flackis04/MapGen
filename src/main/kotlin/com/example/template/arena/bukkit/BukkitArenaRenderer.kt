package com.example.template.arena.bukkit

import com.example.template.arena.domain.BlockPlacement
import org.bukkit.World

class BukkitArenaRenderer(
    private val world: World,
    private val materialMapper: BukkitMaterialMapper,
) {
    fun render(placements: Sequence<BlockPlacement>) {
        placements.forEach { placement ->
            val offset = placement.offset
            world.getBlockAt(offset.x, offset.y, offset.z).type = materialMapper.toBukkit(placement.blockType)
        }
    }
}
