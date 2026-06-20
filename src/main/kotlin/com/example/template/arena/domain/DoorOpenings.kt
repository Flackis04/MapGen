package com.example.template.arena.domain

data class DoorOpenings(
    val front: Boolean,
    val back: Boolean,
    val left: Boolean,
    val right: Boolean,
) {
    companion object {
        val NONE = DoorOpenings(
            front = false,
            back = false,
            left = false,
            right = false,
        )
    }
}
