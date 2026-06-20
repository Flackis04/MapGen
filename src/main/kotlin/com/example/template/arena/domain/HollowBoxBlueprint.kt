package com.example.template.arena.domain

data class HollowBoxBlueprint(
    val dimensions: BoxDimensions,
    val blockType: ArenaBlockType,
    val filledFaces: BoxFaces,
    val doorOpenings: DoorOpenings,
    val doorwayDimensions: DoorwayDimensions,
)
