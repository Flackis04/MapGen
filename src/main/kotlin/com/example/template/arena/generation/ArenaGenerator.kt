package com.example.template.arena.generation

import com.example.template.arena.config.ArenaConfiguration
import com.example.template.arena.domain.BlockPlacement

class ArenaGenerator(
    private val configuration: ArenaConfiguration,
    private val blueprintFactory: HollowBoxBlueprintFactory,
    private val planner: HollowBoxPlanner,
) {
    fun generate(): Sequence<BlockPlacement> =
        configuration.gridCoordinates().flatMap { coordinate ->
            val cellOrigin = configuration.originFor(coordinate)
            val blueprint = blueprintFactory.create(configuration, coordinate)

            planner.plan(blueprint).map { placement ->
                placement.shiftedBy(cellOrigin)
            }
        }
}
