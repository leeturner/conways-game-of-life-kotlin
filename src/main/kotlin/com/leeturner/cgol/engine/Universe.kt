package com.leeturner.cgol.engine

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import kotlin.random.Random

/**
 * https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life
 *
 * The universe of the Game of Life is an infinite, two-dimensional orthogonal grid
 * of square cells, each of which is in one of two possible states, live or dead (or
 * populated and unpopulated, respectively) in this case we will use a set to hold
 * the alive cells in our universe.  This allows us to quickly look up a cell based on its
 * x and y coordinates and hopefully allows us to be a little more efficient by not
 * having to store state for every cell.
 */
@ConsistentCopyVisibility
data class Universe internal constructor(
    val gridSize: Int,
    private val aliveCells: Set<Coordinate>,
) {
    fun isAlive(coordinate: Coordinate) = coordinate in aliveCells

    fun isAlive(
        x: Int,
        y: Int,
    ) = Coordinate(x, y) in aliveCells

    fun population() = aliveCells.size

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
     * Optimized implementation: Instead of checking all gridSize x gridSize cells,
     * each alive cell adds one to the count of each of its neighbours. Only cells with
     * at least one live neighbour get a count, so they are the only ones that can be
     * alive next generation. This is much more efficient for sparse populations.
     */
    fun tick(): Universe =
        copy(
            aliveCells =
                aliveCells
                    .flatMap(::neighbors)
                    .groupingBy { it }
                    .eachCount()
                    .filter { (cell, liveNeighbourCount) ->
                        // Three live neighbours means alive next generation, whether by birth or survival
                        liveNeighbourCount == BIRTH_COUNT ||
                            (isAlive(cell) && liveNeighbourCount in SURVIVAL_MIN..SURVIVAL_MAX)
                    }.keys,
        )

    /**
     * We implement a Toroidal (wrapping) universe - The grid wraps around like a torus.
     * A cell at (0,0) would have neighbors wrapping to the opposite edges.
     * For example, (-1,0) wraps to (size-1, 0).
     */
    private fun neighbors(coordinate: Coordinate): List<Coordinate> =
        listOf(-1, 0, 1).flatMap { dx ->
            listOf(-1, 0, 1).mapNotNull { dy ->
                when {
                    dx == 0 && dy == 0 -> {
                        null
                    }

                    else -> {
                        Coordinate(
                            (coordinate.x + dx + gridSize) % gridSize,
                            (coordinate.y + dy + gridSize) % gridSize,
                        )
                    }
                }
            }
        }

    /**
     * Draws the grid one row per line, using [aliveCell] and [deadCell] for each cell.
     */
    fun toGridString(
        aliveCell: String,
        deadCell: String,
    ): String =
        (0..<gridSize).joinToString("\n") { y ->
            (0..<gridSize).joinToString("") { x ->
                if (isAlive(x, y)) aliveCell else deadCell
            }
        }

    override fun toString(): String = toGridString(aliveCell = " #", deadCell = " ·")

    companion object {
        fun create(
            gridSize: Int = DEFAULT_GRID_SIZE,
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
                Universe(gridSize, aliveCells)
            }

        /**
         * Regenerates until at least one cell is alive, so small grids don't fail at random.
         * A grid with no cells (gridSize < 1) can never succeed, so it is returned empty
         * for create() to reject instead of looping forever.
         */
        private fun randomAliveCells(gridSize: Int): Set<Coordinate> =
            generateSequence {
                (0..<gridSize)
                    .asSequence()
                    .flatMap { x ->
                        (0..<gridSize).mapNotNull { y ->
                            Coordinate(x, y).takeIf { Random.nextBoolean() }
                        }
                    }.toSet()
            }.first { it.isNotEmpty() || gridSize < 1 }

        const val DEFAULT_GRID_SIZE = 64
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

sealed interface UniverseCreationError

data class UniverseMinimumSizeError(
    val minimumGridSize: Int,
) : UniverseCreationError

data class UniverseCoordinatesOutOfBoundsError(
    val outOfBoundsCoordinates: List<Coordinate> = emptyList(),
) : UniverseCreationError

data object UniverseNoAliveCells : UniverseCreationError
