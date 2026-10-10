package com.leeturner.cgol.engine

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import strikt.api.expectThat
import strikt.arrow.isLeft
import strikt.arrow.isRight
import strikt.arrow.value
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

        expectThat(universe)
            .isLeft()
            .value
            .isA<UniverseMinimumSizeError>()
            .get { minimumGridSize }
            .isEqualTo(3)
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `cannot create a random universe with a size of 0`() {
        val universe = Universe.create(gridSize = 0)

        expectThat(universe).isLeft().value.isA<UniverseMinimumSizeError>()
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `cannot create a universe with a size greater than 256`() {
        expectThat(Universe.create(gridSize = 257))
            .isLeft()
            .value
            .isA<UniverseMaximumSizeError>()
            .get { maximumGridSize }
            .isEqualTo(256)

        // Without a limit, building the random start for this size would run out of memory
        expectThat(Universe.create(gridSize = 100_000)).isLeft().value.isA<UniverseMaximumSizeError>()
    }

    @Test
    fun `can create a random universe of the maximum size`() {
        expectThat(Universe.create(gridSize = 256))
            .isRight()
            .value
            .get { gridSize }
            .isEqualTo(256)
    }

    @Test
    fun `a random universe always has at least one alive cell`() {
        // An empty 3x3 start used to happen 1 in 512 times, so 5,000 attempts would almost certainly hit one
        repeat(5_000) {
            expectThat(Universe.create(gridSize = 3)).isRight()
        }
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

        expectThat(universe)
            .isLeft()
            .value
            .isA<UniverseCoordinatesOutOfBoundsError>()
            .get { outOfBoundsCoordinates }
            .containsExactly(
                Coordinate(4, 1),
                Coordinate(1, 4),
            )
    }

    @Test
    fun `cannot create a universe with no alive cells`() {
        val universe = Universe.create(gridSize = 3, aliveCells = setOf())

        expectThat(universe).isLeft().value.isA<UniverseNoAliveCells>()
    }

    @Test
    fun `default universe is 64`() {
        val universe = Universe.create()

        expectThat(universe)
            .isRight()
            .value
            .get { gridSize }
            .isEqualTo(64)
    }

    @Test
    fun `can create a universe with an initial random state`() {
        val universe = Universe.create()

        expectThat(universe)
            .isRight()
            .value
            .get { population() }
            .isGreaterThan(0)
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

        expectThat(universe).isRight().value.and {
            get { gridSize }.isEqualTo(4)
            get { isAlive(Coordinate(0, 0)) }.isTrue()
            get { isAlive(Coordinate(1, 1)) }.isTrue()
            get { isAlive(Coordinate(0, 1)) }.isFalse()
        }
    }
}
