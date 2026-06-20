package com.example.template.arena.generation

import com.example.template.arena.domain.BlockOffset
import com.example.template.arena.domain.BlockPlacement
import com.example.template.arena.domain.BoxDimensions
import com.example.template.arena.domain.DoorOpenings
import com.example.template.arena.domain.DoorwayBounds
import com.example.template.arena.domain.HollowBoxBlueprint

class HollowBoxPlanner {
    fun plan(blueprint: HollowBoxBlueprint): Sequence<BlockPlacement> = sequence {
        val dimensions = blueprint.dimensions
        val doorwayBounds = blueprint.doorwayDimensions.boundsFor(dimensions)

        for (x in 0 until dimensions.width) {
            for (y in 0 until dimensions.height) {
                for (z in 0 until dimensions.depth) {
                    val offset = BlockOffset(x, y, z)

                    if (shouldPlaceBlock(offset, blueprint, doorwayBounds)) {
                        yield(BlockPlacement(offset = offset, blockType = blueprint.blockType))
                    }
                }
            }
        }
    }

    private fun shouldPlaceBlock(
        offset: BlockOffset,
        blueprint: HollowBoxBlueprint,
        doorwayBounds: DoorwayBounds,
    ): Boolean =
        isFilledFace(offset, blueprint) && !isDoorway(offset, blueprint.dimensions, blueprint.doorOpenings, doorwayBounds)

    private fun isFilledFace(offset: BlockOffset, blueprint: HollowBoxBlueprint): Boolean {
        val dimensions = blueprint.dimensions
        val faces = blueprint.filledFaces

        return (offset.x == 0 && faces.left) ||
            (offset.x == dimensions.width - 1 && faces.right) ||
            (offset.y == 0 && faces.bottom) ||
            (offset.y == dimensions.height - 1 && faces.top) ||
            (offset.z == 0 && faces.front) ||
            (offset.z == dimensions.depth - 1 && faces.back)
    }

    private fun isDoorway(
        offset: BlockOffset,
        dimensions: BoxDimensions,
        doorOpenings: DoorOpenings,
        doorwayBounds: DoorwayBounds,
    ): Boolean {
        val isDoorHeight = offset.y in doorwayBounds.yRange
        val isDoorXRange = offset.x in doorwayBounds.xRange
        val isDoorZRange = offset.z in doorwayBounds.zRange

        val frontDoor = doorOpenings.front && isDoorXRange && isDoorHeight && offset.z == 0
        val backDoor = doorOpenings.back && isDoorXRange && isDoorHeight && offset.z == dimensions.depth - 1
        val leftDoor = doorOpenings.left && isDoorZRange && isDoorHeight && offset.x == 0
        val rightDoor = doorOpenings.right && isDoorZRange && isDoorHeight && offset.x == dimensions.width - 1

        return frontDoor || backDoor || leftDoor || rightDoor
    }
}
