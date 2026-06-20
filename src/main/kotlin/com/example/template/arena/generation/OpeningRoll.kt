package com.example.template.arena.generation

enum class OpeningRoll {
    CLOSED,
    CLOSED_ALTERNATE,
    OPEN,
    ;

    val fillsSurface: Boolean
        get() = this != OPEN

    val opensDoorway: Boolean
        get() = this == OPEN
}
