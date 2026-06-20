package com.example.template.arena.config

import com.example.template.arena.domain.ArenaBlockType
import com.example.template.arena.domain.BlockOffset
import com.example.template.arena.domain.BoxDimensions
import com.example.template.arena.domain.DoorwayDimensions
import com.example.template.arena.domain.GridCoordinate

data class ArenaConfiguration(
    val worldName: String = "world",
    val origin: BlockOffset = BlockOffset(0, 0, 0),
    val cellsPerAxis: Int = 10,
    val levelCount: Int = 5,
    val boxDimensions: BoxDimensions = BoxDimensions(width = 11, height = 11, depth = 11),
    val wallThickness: Int = 1,
    val blockType: ArenaBlockType = ArenaBlockType.GRAY_CONCRETE,
    val doorwayDimensions: DoorwayDimensions = DoorwayDimensions(width = 3, height = 3, bottomY = 1),
) {
    init {
        require(worldName.isNotBlank()) { "worldName must not be blank." }
        require(cellsPerAxis > 0) { "cellsPerAxis must be greater than zero." }
        require(levelCount > 0) { "levelCount must be greater than zero." }
        require(wallThickness >= 0) { "wallThickness must not be negative." }
        require(wallThickness < boxDimensions.minimumSide) { "wallThickness must be smaller than every box dimension." }
    }

    fun gridCoordinates(): Sequence<GridCoordinate> = sequence {
        for (column in 0 until cellsPerAxis) {
            for (level in 0 until levelCount) {
                for (row in 0 until cellsPerAxis) {
                    yield(GridCoordinate(column = column, level = level, row = row))
                }
            }
        }
    }

    fun originFor(coordinate: GridCoordinate): BlockOffset =
        origin + BlockOffset(
            x = coordinate.column * (boxDimensions.width - wallThickness),
            y = coordinate.level * (boxDimensions.height - wallThickness),
            z = coordinate.row * (boxDimensions.depth - wallThickness),
        )
}
