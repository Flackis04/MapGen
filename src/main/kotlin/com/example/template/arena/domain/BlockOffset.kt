package com.example.template.arena.domain

data class BlockOffset(
    val x: Int,
    val y: Int,
    val z: Int,
) {
    operator fun plus(other: BlockOffset): BlockOffset =
        BlockOffset(
            x = x + other.x,
            y = y + other.y,
            z = z + other.z,
        )
}
