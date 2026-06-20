package com.example.template.arena.bootstrap

import com.example.template.arena.bukkit.BukkitArenaRendererFactory
import com.example.template.arena.bukkit.BukkitMaterialMapper
import com.example.template.arena.bukkit.BukkitWorldResolver
import com.example.template.arena.config.ArenaConfiguration
import com.example.template.arena.generation.ArenaGenerator
import com.example.template.arena.generation.HollowBoxBlueprintFactory
import com.example.template.arena.generation.HollowBoxPlanner
import com.example.template.arena.generation.RandomOpeningRollSource
import org.bukkit.plugin.java.JavaPlugin

object ArenaPluginModule {
    fun create(plugin: JavaPlugin): ArenaStartup {
        val configuration = ArenaConfiguration()
        val generator = ArenaGenerator(
            configuration = configuration,
            blueprintFactory = HollowBoxBlueprintFactory(RandomOpeningRollSource()),
            planner = HollowBoxPlanner(),
        )

        return ArenaStartup(
            configuration = configuration,
            worldResolver = BukkitWorldResolver(plugin.server, plugin.logger),
            rendererFactory = BukkitArenaRendererFactory(BukkitMaterialMapper()),
            generator = generator,
        )
    }
}
