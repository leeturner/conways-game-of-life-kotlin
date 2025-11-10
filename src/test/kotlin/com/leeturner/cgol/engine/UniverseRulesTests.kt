package com.leeturner.cgol.engine

import arrow.core.getOrElse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class UniverseRulesTests {
    @Test
    fun `any live cell with fewer than two live neighbours dies as if by underpopulation`() {
        val coordinateUnderTest = Coordinate(1, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells = setOf(coordinateUnderTest),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . . .
           | . # .
           | . . .
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isFalse()
    }

    @Test
    fun `any live cell with more than three live neighbours dies as if by overpopulation`() {
        val coordinateUnderTest = Coordinate(1, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(1, 0),
                        Coordinate(2, 0),
                        coordinateUnderTest,
                        Coordinate(2, 1),
                        Coordinate(2, 2),
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . # #
           | . # #
           | . . #
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isFalse()
    }

    @Test
    fun `any dead cell with exactly three live neighbours becomes a live cell, as if by reproduction`() {
        val coordinateUnderTest = Coordinate(1, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(1, 0),
                        Coordinate(2, 0),
                        // coordinateUnderTest is not present due to it being dead
                        Coordinate(2, 1),
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . # #
           | . . #
           | . . .
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isTrue()
    }

    @Test
    fun `any live cell with two (or three) live neighbours lives on to the next generation`() {
        val coordinateUnderTest = Coordinate(1, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(1, 0),
                        Coordinate(2, 0),
                        coordinateUnderTest,
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . # #
           | . # .
           | . . .
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isTrue()
    }

    @Test
    fun `any live cell with three (or two) live neighbours lives on to the next generation`() {
        val coordinateUnderTest = Coordinate(1, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(1, 0),
                        Coordinate(2, 0),
                        coordinateUnderTest,
                        Coordinate(2, 1),
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . # #
           | . # #
           | . . .
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isTrue()
    }

    @Test
    fun `oscillators - blinker`() {
        // see https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life

        val universe =
            Universe.create(
                gridSize = 5,
                aliveCells =
                    setOf(
                        Coordinate(2, 1),
                        Coordinate(2, 2),
                        Coordinate(2, 3),
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | . . . . .
           | . . # . .
           | . . # . .
           | . . # . .
           | . . . . .
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.toString()).isEqualTo(
            """
           | . . . . .
           | . . . . .
           | . # # # .
           | . . . . .
           | . . . . .
            """.trimMargin(),
        )

        val thirdGeneration = secondGeneration.tick()

        expectThat(thirdGeneration.toString()).isEqualTo(
            """
           | . . . . .
           | . . # . .
           | . . # . .
           | . . # . .
           | . . . . .
            """.trimMargin(),
        )
    }
}
