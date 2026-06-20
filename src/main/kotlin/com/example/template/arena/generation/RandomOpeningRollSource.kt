package com.example.template.arena.generation

import kotlin.random.Random

class RandomOpeningRollSource(
    private val random: Random = Random.Default,
) : OpeningRollSource {
    override fun nextRoll(): OpeningRoll =
        OpeningRoll.entries[random.nextInt(OpeningRoll.entries.size)]
}
