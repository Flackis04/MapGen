package com.example.template.arena.domain

data class GridCoordinate(
    val column: Int,
    val level: Int,
    val row: Int,
) {
    val isBaseLevel: Boolean
        get() = level == 0
}
