package com.leeturner.cgol.engine

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.leeturner.cgol.engine.CellState.ALIVE
import kotlin.random.Random

/**
 * https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life
 *
 * The universe of the Game of Life is an infinite, two-dimensional orthogonal grid
 * of square cells, each of which is in one of two possible states, live or dead (or
 * populated and unpopulated, respectively) in this case we will use a map to hold
 * the cells in our universe.  This allows us to quickly look up a cell based on its
 * x and y coordinates and hopefully allows us to be a little more efficient by not
 * having to store state for every cell.
 */
@ConsistentCopyVisibility
data class Universe internal constructor(
    val gridSize: Int = 64,
    private val cells: Map<Coordinate, CellState> = emptyMap(),
) {
    fun isAlive(coordinate: Coordinate) = cells[coordinate] == ALIVE

    fun population() = cells.size

    fun tick(): Universe {
        /**
         * Every cell interacts with its eight neighbours, which are the cells that are
         * horizontally, vertically, or diagonally adjacent. At each step in time, the
         * following transitions occur:
         *
         * Any live cell with fewer than two live neighbours dies, as if by underpopulation.
         * Any live cell with more than three live neighbours dies, as if by overpopulation.
         * Any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction.
         * Any live cell with two or three live neighbours lives on to the next generation.
         *
         * The nice thing about only tracking live cells is that we only need to worry about the rules
         * that produce a live cell
         */
        val newCells =
            allCoordinates
                .mapNotNull { coordinate ->
                    val liveNeighbourCount = neighbors(coordinate).count { isAlive(it) }
                    val isCurrentlyAlive = isAlive(coordinate)

                    when {
                        isCurrentlyAlive && liveNeighbourCount in SURVIVAL_MIN..SURVIVAL_MAX -> coordinate to ALIVE
                        !isCurrentlyAlive && liveNeighbourCount == BIRTH_COUNT -> coordinate to ALIVE
                        else -> null
                    }
                }.toMap()

        return copy(cells = newCells)
    }

    private val allCoordinates: Sequence<Coordinate>
        get() =
            (0..<gridSize).asSequence().flatMap { y ->
                (0..<gridSize).map { x -> Coordinate(x, y) }
            }

    /**
     * We implement a Toroidal (wrapping) universe - The grid wraps around like a torus.
     * A cell at (0,0) would have neighbors wrapping to the opposite edges.
     * For example, (-1,0) wraps to (size-1, 0).
     */
    private fun neighbors(coordinate: Coordinate): List<Coordinate> =
        listOf(-1, 0, 1).flatMap { dx ->
            listOf(-1, 0, 1).mapNotNull { dy ->
                when {
                    dx == 0 && dy == 0 -> null
                    else ->
                        Coordinate(
                            (coordinate.x + dx + gridSize) % gridSize,
                            (coordinate.y + dy + gridSize) % gridSize,
                        )
                }
            }
        }

    private fun aliveCells() = cells.keys

    override fun toString(): String =
        allCoordinates
            .chunked(gridSize)
            .joinToString("\n") { row ->
                row.joinToString("") { coord ->
                    if (isAlive(coord)) " #" else " ."
                }
            }

    companion object {
        fun create(
            gridSize: Int = 64,
            aliveCells: Set<Coordinate> = randomAliveCells(gridSize),
        ): Either<UniverseCreationError, Universe> =
            either {
                ensure(gridSize >= MINIMUM_GRID_SIZE) {
                    UniverseMinimumSizeError(MINIMUM_GRID_SIZE)
                }
                ensure(aliveCells.isNotEmpty()) { UniverseNoAliveCells }
                val outOfBoundCoordinates =
                    aliveCells.filterNot {
                        it.x in 0..<gridSize && it.y in 0..<gridSize
                    }
                ensure(outOfBoundCoordinates.isEmpty()) {
                    UniverseCoordinatesOutOfBoundsError(outOfBoundCoordinates)
                }
                return Universe(gridSize, aliveCells.associateWith { ALIVE }).right()
            }

        // TODO:  This might add too many alive cells
        private fun randomAliveCells(gridSize: Int): Set<Coordinate> =
            (0..<gridSize)
                .asSequence()
                .flatMap { x ->
                    (0..<gridSize).mapNotNull { y ->
                        Coordinate(x, y).takeIf { Random.nextBoolean() }
                    }
                }.toSet()

        private const val MINIMUM_GRID_SIZE = 3
        private const val SURVIVAL_MIN = 2 // Minimum neighbors for survival
        private const val SURVIVAL_MAX = 3 // Maximum neighbors for survival
        private const val BIRTH_COUNT = 3 // Neighbors needed for birth
    }
}

data class Coordinate(
    val x: Int,
    val y: Int,
)

enum class CellState {
    DEAD,
    ALIVE,
}

sealed interface UniverseCreationError

data class UniverseMinimumSizeError(
    val minimumGridSize: Int,
) : UniverseCreationError

data class UniverseCoordinatesOutOfBoundsError(
    val outOfBoundsCoordinates: List<Coordinate> = emptyList(),
) : UniverseCreationError

data object UniverseNoAliveCells : UniverseCreationError
