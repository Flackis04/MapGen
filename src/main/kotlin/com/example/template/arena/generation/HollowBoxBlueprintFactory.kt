package com.example.template.arena.generation

import com.example.template.arena.config.ArenaConfiguration
import com.example.template.arena.domain.BoxFaces
import com.example.template.arena.domain.DoorOpenings
import com.example.template.arena.domain.GridCoordinate
import com.example.template.arena.domain.HollowBoxBlueprint

class HollowBoxBlueprintFactory(
    private val rollSource: OpeningRollSource,
) {
    fun create(configuration: ArenaConfiguration, coordinate: GridCoordinate): HollowBoxBlueprint {
        val leftFaceRoll = rollSource.nextRoll()
        val rightFaceRoll = rollSource.nextRoll()
        val bottomFaceRoll = rollSource.nextRoll()
        val topFaceRoll = rollSource.nextRoll()
        val frontFaceRoll = rollSource.nextRoll()
        val backFaceRoll = rollSource.nextRoll()

        return HollowBoxBlueprint(
            dimensions = configuration.boxDimensions,
            blockType = configuration.blockType,
            filledFaces = BoxFaces(
                left = leftFaceRoll.fillsSurface,
                right = rightFaceRoll.fillsSurface,
                bottom = coordinate.isBaseLevel || bottomFaceRoll.fillsSurface,
                top = topFaceRoll.fillsSurface,
                front = frontFaceRoll.fillsSurface,
                back = backFaceRoll.fillsSurface,
            ),
            doorOpenings = DoorOpenings(
                front = nextDoorIsOpen(),
                back = nextDoorIsOpen(),
                left = nextDoorIsOpen(),
                right = nextDoorIsOpen(),
            ),
            doorwayDimensions = configuration.doorwayDimensions,
        )
    }

    private fun nextDoorIsOpen(): Boolean =
        rollSource.nextRoll().opensDoorway
}
