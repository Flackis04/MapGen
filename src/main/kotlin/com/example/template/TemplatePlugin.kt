package com.example.template

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin
import kotlin.collections.List

class TemplatePlugin : JavaPlugin() {
    override fun onEnable() {
        val world = Bukkit.getWorld("world") ?: return
        val origin = Location(world, 0.0, 0.0, 0.0)

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

                    Settings.grid[x][y][z] = true
                    HollowBox().makeHollowBox(
                        origin.clone().add(
                            (x * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble(),
                            (y * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble() - 60,
                            (z * (Settings.BOX_SIZE - Settings.WALL_THICKNESS)).toDouble()
                        ),
                        Settings.BOX_SIZE,
                        Settings.BOX_SIZE,
                        Settings.BOX_SIZE,
                        Material.GRAY_CONCRETE,
                        List(6) { kotlin.random.Random.nextInt(0, 3) },
                        List(4) { kotlin.random.Random.nextInt(0, 3) }
                    )
                }
                //else Settings.grid[x][y][z] = false
            }
        }
    }
}

