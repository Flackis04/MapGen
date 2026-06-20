package com.example.template

import com.example.template.arena.bootstrap.ArenaPluginModule
import com.example.template.arena.bootstrap.ArenaStartup
import org.bukkit.plugin.java.JavaPlugin

class TemplatePlugin : JavaPlugin() {
    private lateinit var arenaStartup: ArenaStartup

    override fun onEnable() {
        arenaStartup = ArenaPluginModule.create(this)
        arenaStartup.generateArena()
    }
}
