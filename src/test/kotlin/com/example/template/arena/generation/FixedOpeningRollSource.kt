package com.example.template.arena.generation

class FixedOpeningRollSource(
    private val roll: OpeningRoll,
) : OpeningRollSource {
    override fun nextRoll(): OpeningRoll = roll
}
