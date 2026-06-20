package com.example.template.arena.domain

data class DoorwayDimensions(
    val width: Int,
    val height: Int,
    val bottomY: Int,
) {
    init {
        require(width > 0) { "width must be greater than zero." }
        require(height > 0) { "height must be greater than zero." }
        require(bottomY >= 0) { "bottomY must not be negative." }
    }

    fun boundsFor(dimensions: BoxDimensions): DoorwayBounds {
        require(width <= dimensions.width) { "Doorway width must fit inside the box width." }
        require(width <= dimensions.depth) { "Doorway width must fit inside the box depth." }
        require(bottomY + height <= dimensions.height) { "Doorway height must fit inside the box height." }

        val xStart = (dimensions.width - width) / 2
        val zStart = (dimensions.depth - width) / 2

        return DoorwayBounds(
            xRange = xStart until xStart + width,
            yRange = bottomY until bottomY + height,
            zRange = zStart until zStart + width,
        )
    }
}
