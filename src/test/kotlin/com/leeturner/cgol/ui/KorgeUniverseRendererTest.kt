package com.leeturner.cgol.ui

import arrow.core.getOrElse
import com.leeturner.cgol.engine.Coordinate
import com.leeturner.cgol.engine.Universe
import korlibs.image.bitmap.Bitmap32
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.awt.HeadlessException
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class KorgeUniverseRendererTest {
    private fun universe(vararg alive: Coordinate) =
        Universe.create(gridSize = 3, aliveCells = alive.toSet()).getOrElse { fail("Expected valid universe") }

    @Test
    fun `alive cells are painted alive and every other cell dead, including the edges`() {
        // Asymmetric pattern: a transposed x/y would paint (2,0) instead of (0,2)
        val bitmap = Bitmap32(3, 3)

        fillBitmap(universe(Coordinate(0, 0), Coordinate(0, 2), Coordinate(2, 2)), bitmap)

        val alive = setOf(0 to 0, 0 to 2, 2 to 2)
        for (x in 0..2) {
            for (y in 0..2) {
                val expected = if ((x to y) in alive) ALIVE_CELL_COLOR else DEAD_CELL_COLOR
                expectThat(bitmap[x, y]).describedAs("pixel ($x, $y)").isEqualTo(expected)
            }
        }
    }

    @Test
    fun `a cell that dies is repainted dead when the bitmap is reused`() {
        val bitmap = Bitmap32(3, 3)
        fillBitmap(universe(Coordinate(1, 1)), bitmap)

        fillBitmap(universe(Coordinate(0, 0)), bitmap)

        expectThat(bitmap[1, 1]).isEqualTo(DEAD_CELL_COLOR)
        expectThat(bitmap[0, 0]).isEqualTo(ALIVE_CELL_COLOR)
    }

    @Test
    fun `a window that fails to open gives an error exit code and says why`() {
        ByteArrayOutputStream().use { baos ->
            val originalErr = System.err
            System.setErr(PrintStream(baos))
            val exitCode =
                try {
                    windowExitCode { throw HeadlessException() }
                } finally {
                    System.setErr(originalErr)
                }

            expectThat(exitCode).isEqualTo(1)
            expectThat(baos.toString()).contains("Could not open the KorGE window")
        }
    }

    @Test
    fun `a window that closes normally gives a success exit code`() {
        expectThat(windowExitCode {}).isEqualTo(0)
    }
}
