package com.leeturner.cgol.engine

import arrow.core.getOrElse
import org.junit.jupiter.api.Nested
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
           | · · ·
           | · # ·
           | · · ·
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
           | · # #
           | · # #
           | · · #
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
           | · # #
           | · · #
           | · · ·
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
           | · # #
           | · # ·
           | · · ·
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
           | · # #
           | · # #
           | · · ·
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isTrue()
    }

    @Test
    fun `the universe wraps from left to right`() {
        val coordinateUnderTest = Coordinate(2, 1)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        Coordinate(0, 0),
                        Coordinate(0, 1),
                        Coordinate(0, 2),
                        Coordinate(2, 0),
                        coordinateUnderTest,
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | # · #
           | # · #
           | # · ·
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isFalse()
    }

    @Test
    fun `the universe wraps from top to bottom`() {
        val coordinateUnderTest = Coordinate(1, 0)

        val universe =
            Universe.create(
                gridSize = 3,
                aliveCells =
                    setOf(
                        coordinateUnderTest,
                        Coordinate(2, 0),
                        Coordinate(0, 2),
                        Coordinate(1, 2),
                        Coordinate(2, 2),
                    ),
            )

        val initialState = universe.getOrElse { fail("Expected valid initial state") }

        expectThat(initialState.toString()).isEqualTo(
            """
           | · # #
           | · · ·
           | # # #
            """.trimMargin(),
        )

        val secondGeneration = initialState.tick()

        expectThat(secondGeneration.isAlive(coordinateUnderTest)).isFalse()
    }

    @Nested
    inner class StillLifeTests {
        @Test
        fun `still life - block`() {
            val universe =
                Universe.create(
                    gridSize = 4,
                    aliveCells =
                        setOf(
                            Coordinate(1, 1),
                            Coordinate(1, 2),
                            Coordinate(2, 1),
                            Coordinate(2, 2),
                        ),
                )

            val initialState = universe.getOrElse { fail("Expected valid initial state") }

            expectThat(initialState.toString()).isEqualTo(
                """
           | · · · ·
           | · # # ·
           | · # # ·
           | · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
           | · · · ·
           | · # # ·
           | · # # ·
           | · · · ·
                """.trimMargin(),
            )
        }

        @Test
        fun `still life - bee-hive`() {
            val universe =
                Universe.create(
                    gridSize = 6,
                    aliveCells =
                        setOf(
                            Coordinate(1, 2),
                            Coordinate(2, 1),
                            Coordinate(2, 3),
                            Coordinate(3, 1),
                            Coordinate(3, 3),
                            Coordinate(4, 2),
                        ),
                )

            val initialState = universe.getOrElse { fail("Expected valid initial state") }

            expectThat(initialState.toString()).isEqualTo(
                """
           | · · · · · ·
           | · · # # · ·
           | · # · · # ·
           | · · # # · ·
           | · · · · · ·
           | · · · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
           | · · · · · ·
           | · · # # · ·
           | · # · · # ·
           | · · # # · ·
           | · · · · · ·
           | · · · · · ·
                """.trimMargin(),
            )
        }

        @Test
        fun `still life - tub`() {
            val universe =
                Universe.create(
                    gridSize = 5,
                    aliveCells =
                        setOf(
                            Coordinate(1, 2),
                            Coordinate(2, 1),
                            Coordinate(2, 3),
                            Coordinate(3, 2),
                        ),
                )

            val initialState = universe.getOrElse { fail("Expected valid initial state") }

            expectThat(initialState.toString()).isEqualTo(
                """
           | · · · · ·
           | · · # · ·
           | · # · # ·
           | · · # · ·
           | · · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
           | · · · · ·
           | · · # · ·
           | · # · # ·
           | · · # · ·
           | · · · · ·
                """.trimMargin(),
            )
        }
    }

    @Nested
    inner class OscillatorsTests {
        @Test
        fun `oscillators - blinker`() {
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
           | · · · · ·
           | · · # · ·
           | · · # · ·
           | · · # · ·
           | · · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
           | · · · · ·
           | · · · · ·
           | · # # # ·
           | · · · · ·
           | · · · · ·
                """.trimMargin(),
            )

            val thirdGeneration = secondGeneration.tick()

            expectThat(thirdGeneration.toString()).isEqualTo(
                """
           | · · · · ·
           | · · # · ·
           | · · # · ·
           | · · # · ·
           | · · · · ·
                """.trimMargin(),
            )
        }

        @Test
        fun `oscillators - toad`() {
            val universe =
                Universe.create(
                    gridSize = 6,
                    aliveCells =
                        setOf(
                            Coordinate(1, 3),
                            Coordinate(2, 2),
                            Coordinate(2, 3),
                            Coordinate(3, 2),
                            Coordinate(3, 3),
                            Coordinate(4, 2),
                        ),
                )

            val initialState = universe.getOrElse { fail("Expected valid initial state") }

            expectThat(initialState.toString()).isEqualTo(
                """
           | · · · · · ·
           | · · · · · ·
           | · · # # # ·
           | · # # # · ·
           | · · · · · ·
           | · · · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
          | · · · · · ·
          | · · · # · ·
          | · # · · # ·
          | · # · · # ·
          | · · # · · ·
          | · · · · · ·
                """.trimMargin(),
            )

            val thirdGeneration = secondGeneration.tick()

            expectThat(thirdGeneration.toString()).isEqualTo(
                """
           | · · · · · ·
           | · · · · · ·
           | · · # # # ·
           | · # # # · ·
           | · · · · · ·
           | · · · · · ·
                """.trimMargin(),
            )
        }

        @Test
        fun `oscillators - beacon`() {
            val universe =
                Universe.create(
                    gridSize = 6,
                    aliveCells =
                        setOf(
                            Coordinate(1, 1),
                            Coordinate(1, 2),
                            Coordinate(2, 1),
                            Coordinate(3, 4),
                            Coordinate(4, 3),
                            Coordinate(4, 4),
                        ),
                )

            val initialState = universe.getOrElse { fail("Expected valid initial state") }

            expectThat(initialState.toString()).isEqualTo(
                """
           | · · · · · ·
           | · # # · · ·
           | · # · · · ·
           | · · · · # ·
           | · · · # # ·
           | · · · · · ·
                """.trimMargin(),
            )

            val secondGeneration = initialState.tick()

            expectThat(secondGeneration.toString()).isEqualTo(
                """
           | · · · · · ·
           | · # # · · ·
           | · # # · · ·
           | · · · # # ·
           | · · · # # ·
           | · · · · · ·
                """.trimMargin(),
            )

            val thirdGeneration = secondGeneration.tick()

            expectThat(thirdGeneration.toString()).isEqualTo(
                """
           | · · · · · ·
           | · # # · · ·
           | · # · · · ·
           | · · · · # ·
           | · · · # # ·
           | · · · · · ·
                """.trimMargin(),
            )
        }
    }
}
