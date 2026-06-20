package com.example.template.arena.bootstrap

import com.example.template.arena.bukkit.BukkitArenaRendererFactory
import com.example.template.arena.bukkit.BukkitWorldResolver
import com.example.template.arena.config.ArenaConfiguration
import com.example.template.arena.generation.ArenaGenerator

class ArenaStartup(
    private val configuration: ArenaConfiguration,
    private val worldResolver: BukkitWorldResolver,
    private val rendererFactory: BukkitArenaRendererFactory,
    private val generator: ArenaGenerator,
) {
    fun generateArena() {
        val world = worldResolver.resolve(configuration.worldName) ?: return
        rendererFactory.create(world).render(generator.generate())
    }
}
