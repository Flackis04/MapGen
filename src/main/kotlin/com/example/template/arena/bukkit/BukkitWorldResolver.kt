package com.example.template.arena.bukkit

import org.bukkit.Server
import org.bukkit.World
import java.util.logging.Logger

class BukkitWorldResolver(
    private val server: Server,
    private val logger: Logger,
) {
    fun resolve(worldName: String): World? {
        val world = server.getWorld(worldName)

        if (world == null) {
            logger.warning("Cannot generate arena because world '$worldName' is not loaded.")
        }

        return world
    }
}
