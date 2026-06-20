package com.example.template.arena.bukkit

import org.bukkit.World

class BukkitArenaRendererFactory(
    private val materialMapper: BukkitMaterialMapper,
) {
    fun create(world: World): BukkitArenaRenderer = BukkitArenaRenderer(world, materialMapper)
}
