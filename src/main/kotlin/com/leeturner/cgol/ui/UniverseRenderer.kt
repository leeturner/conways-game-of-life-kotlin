package com.leeturner.cgol.ui

import com.leeturner.cgol.engine.Universe
import io.micronaut.context.annotation.Property
import jakarta.inject.Singleton

@Singleton
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

        moveCursorHome()
        val frame =
            buildString {
                appendLine("Generation: $generation | Population: ${universe.population()}")
                appendLine()
                for (y in 0..<universe.gridSize) {
                    for (x in 0..<universe.gridSize) {
                        if (universe.isAlive(x, y)) {
                            append(aliveCell)
                        } else {
                            append(deadCell)
                        }
                    }
                    appendLine()
                }
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
