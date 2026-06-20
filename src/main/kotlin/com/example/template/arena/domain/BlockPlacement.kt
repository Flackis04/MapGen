package com.example.template.arena.domain

data class BlockPlacement(
    val offset: BlockOffset,
    val blockType: ArenaBlockType,
) {
    fun shiftedBy(origin: BlockOffset): BlockPlacement =
        copy(offset = origin + offset)
}
