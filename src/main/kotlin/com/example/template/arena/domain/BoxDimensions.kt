package com.example.template.arena.domain

data class BoxDimensions(
    val width: Int,
    val height: Int,
    val depth: Int,
) {
    init {
        require(width > 0) { "width must be greater than zero." }
        require(height > 0) { "height must be greater than zero." }
        require(depth > 0) { "depth must be greater than zero." }
    }

    val minimumSide: Int
        get() = minOf(width, height, depth)
}
