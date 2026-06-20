package com.example.template.arena.bukkit

import com.example.template.arena.domain.ArenaBlockType
import org.bukkit.Material

class BukkitMaterialMapper {
    fun toBukkit(blockType: ArenaBlockType): Material =
        when (blockType) {
            ArenaBlockType.GRAY_CONCRETE -> Material.GRAY_CONCRETE
        }
}
