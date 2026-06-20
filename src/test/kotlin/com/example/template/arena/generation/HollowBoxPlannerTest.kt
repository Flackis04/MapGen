package com.example.template.arena.generation

import com.example.template.arena.domain.ArenaBlockType
import com.example.template.arena.domain.BlockOffset
import com.example.template.arena.domain.BoxDimensions
import com.example.template.arena.domain.BoxFaces
import com.example.template.arena.domain.DoorOpenings
import com.example.template.arena.domain.DoorwayDimensions
import com.example.template.arena.domain.HollowBoxBlueprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class HollowBoxPlannerTest {
    private val planner = HollowBoxPlanner()

    @Test
    fun `filled box contains only shell blocks`() {
        val placements = planner.plan(
            HollowBoxBlueprint(
                dimensions = BoxDimensions(width = 3, height = 3, depth = 3),
                blockType = ArenaBlockType.GRAY_CONCRETE,
                filledFaces = BoxFaces.ALL,
                doorOpenings = DoorOpenings.NONE,
                doorwayDimensions = DoorwayDimensions(width = 1, height = 1, bottomY = 1),
            ),
        ).toList()

        assertEquals(26, placements.size)
        assertFalse(placements.any { it.offset == BlockOffset(1, 1, 1) })
    }

    @Test
    fun `open door removes matching face blocks`() {
        val placements = planner.plan(
            HollowBoxBlueprint(
                dimensions = BoxDimensions(width = 5, height = 5, depth = 5),
                blockType = ArenaBlockType.GRAY_CONCRETE,
                filledFaces = BoxFaces.ALL,
                doorOpenings = DoorOpenings(front = true, back = false, left = false, right = false),
                doorwayDimensions = DoorwayDimensions(width = 3, height = 3, bottomY = 1),
            ),
        ).toList()

        val removedFrontDoorBlocks = (1..3).flatMap { x ->
            (1..3).map { y -> BlockOffset(x, y, 0) }
        }

        assertFalse(placements.any { it.offset in removedFrontDoorBlocks })
    }
}
