package com.example.template.arena.domain

data class BoxFaces(
    val left: Boolean,
    val right: Boolean,
    val bottom: Boolean,
    val top: Boolean,
    val front: Boolean,
    val back: Boolean,
) {
    companion object {
        val ALL = BoxFaces(
            left = true,
            right = true,
            bottom = true,
            top = true,
            front = true,
            back = true,
        )
    }
}
