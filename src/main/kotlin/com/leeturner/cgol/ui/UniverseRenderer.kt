package com.leeturner.cgol.ui

import com.leeturner.cgol.engine.Universe
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton

@Singleton
@Primary
class SimpleTerminalUniverseRenderer(
    @param:Property(name = "universe.renderer.alive-cell-color") private val aliveCellColor: String,
    @param:Property(name = "universe.renderer.dead-cell-color") private val deadCellColor: String,
) : UniverseRenderer {
    override fun render(
        universe: Universe,
        generation: Int,
    ) {
        val aliveCell = "$aliveCellColor #\u001b[0m"
        val deadCell = "$deadCellColor ·\u001b[0m"

        print("\u001b[?25l") // Hide cursor

        if (generation == 0) print("\u001b[2J") // Clear existing terminal content before the first frame
        moveCursorHome()
        val frame =
            buildString {
                // Clear to end of line so a shorter header doesn't leave stale digits behind
                appendLine("Generation: $generation | Population: ${universe.population()}\u001b[K")
                appendLine()
                appendLine(universe.toGridString(aliveCell, deadCell))
            }
        print(frame)
        System.out.flush()
        print("\u001b[?25h") // Show cursor
    }

    private fun moveCursorHome() {
        print("\u001b[H") // Move cursor to home without clearing
    }
}

fun interface UniverseRenderer {
    fun render(
        universe: Universe,
        generation: Int,
    )
}
