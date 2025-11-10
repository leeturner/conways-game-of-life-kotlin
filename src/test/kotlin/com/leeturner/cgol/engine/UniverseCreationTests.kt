package com.leeturner.cgol.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue

class UniverseCreationTests {
    @Test
    fun `cannot create a universe with a size less than 3`() {
        val universe = Universe.create(gridSize = 2)

        universe.fold(
            ifLeft = { error ->
                expectThat(error)
                    .isA<UniverseMinimumSizeError>()
                    .get { minimumGridSize }
                    .isEqualTo(3)
            },
            ifRight = { fail("Expected Left but got Right: $it") },
        )
    }

    @Test
    fun `cannot create a universe with alive cells out of range`() {
        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(0, 0),
                        Coordinate(4, 1),
                        Coordinate(1, 4),
                    ),
            )

        universe.fold(
            ifLeft = { error ->
                expectThat(error)
                    .isA<UniverseCoordinatesOutOfBoundsError>()
                    .get { outOfBoundsCoordinates }
                    .containsExactly(
                        Coordinate(4, 1),
                        Coordinate(1, 4),
                    )
            },
            ifRight = { fail("Expected Left but got Right: $it") },
        )
    }

    @Test
    fun `cannot create a universe with no alive cells`() {
        val universe = Universe.create(gridSize = 3, aliveCells = setOf())

        universe.fold(
            ifLeft = { error ->
                expectThat(error).isA<UniverseNoAliveCells>()
            },
            ifRight = { fail("Expected Left but got Right: $it") },
        )
    }

    @Test
    fun `default universe is 64`() {
        val universe = Universe.create()

        universe.fold(
            ifLeft = { fail("Expected Right but got Left: $it") },
            ifRight = { universe ->
                expectThat(universe) {
                    get { gridSize }.isEqualTo(64)
                }
            },
        )
    }

    @Test
    fun `can create a universe with an initial random state`() {
        val universe = Universe.create()

        universe.fold(
            ifLeft = { fail("Expected Right but got Left: $it") },
            ifRight = { universe ->
                expectThat(universe) {
                    get { population() }.isGreaterThan(0)
                }
            },
        )
        println(universe.toString())
    }

    @Test
    fun `can create a universe with an initial state`() {
        val universe =
            Universe.create(
                gridSize = 4,
                aliveCells =
                    setOf(
                        Coordinate(0, 0),
                        Coordinate(1, 1),
                    ),
            )

        universe.fold(
            ifLeft = { fail("Expected Right but got Left: $it") },
            ifRight = { universe ->
                expectThat(universe) {
                    get { gridSize }.isEqualTo(4)
                    get { isAlive(Coordinate(0, 0)) }.isTrue()
                    get { isAlive(Coordinate(1, 1)) }.isTrue()
                    get { isAlive(Coordinate(0, 1)) }.isFalse()
                }
            },
        )
    }
}
