package com.example.template.arena.generation

import com.example.template.arena.config.ArenaConfiguration
import com.example.template.arena.domain.GridCoordinate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HollowBoxBlueprintFactoryTest {
    @Test
    fun `base level forces bottom face to be filled`() {
        val factory = HollowBoxBlueprintFactory(FixedOpeningRollSource(OpeningRoll.OPEN))

        val blueprint = factory.create(
            configuration = ArenaConfiguration(),
            coordinate = GridCoordinate(column = 0, level = 0, row = 0),
        )

        assertTrue(blueprint.filledFaces.bottom)
        assertFalse(blueprint.filledFaces.left)
        assertFalse(blueprint.filledFaces.right)
        assertFalse(blueprint.filledFaces.top)
        assertFalse(blueprint.filledFaces.front)
        assertFalse(blueprint.filledFaces.back)
    }

    @Test
    fun `upper levels allow bottom face to open`() {
        val factory = HollowBoxBlueprintFactory(FixedOpeningRollSource(OpeningRoll.OPEN))

        val blueprint = factory.create(
            configuration = ArenaConfiguration(),
            coordinate = GridCoordinate(column = 0, level = 1, row = 0),
        )

        assertFalse(blueprint.filledFaces.bottom)
    }
}
