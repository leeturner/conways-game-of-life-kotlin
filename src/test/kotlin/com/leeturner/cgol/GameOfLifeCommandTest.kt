package com.leeturner.cgol

import arrow.core.getOrElse
import com.leeturner.cgol.engine.Coordinate
import com.leeturner.cgol.engine.Universe
import com.leeturner.cgol.ui.UniverseRenderer
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.fail
import picocli.CommandLine
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class GameOfLifeCommandTest {
    private val noOpRenderer = UniverseRenderer { _, _ -> }

    @Test
    fun `the terminal renderer is used when no renderer is given`() {
        val terminal = UniverseRenderer { _, _ -> }
        val command = GameOfLifeCommand(terminalRenderer = terminal, korgeRenderer = noOpRenderer)

        CommandLine(command).parseArgs()

        expectThat(command.selectedRenderer()).isSameInstanceAs(terminal)
    }

    @Test
    fun `the korge renderer is used when asked for in lowercase`() {
        val korge = UniverseRenderer { _, _ -> }
        val command = GameOfLifeCommand(terminalRenderer = noOpRenderer, korgeRenderer = korge)

        CommandLine(command).parseArgs("-r", "korge")

        expectThat(command.selectedRenderer()).isSameInstanceAs(korge)
    }

    @Test
    fun `an unknown renderer is rejected with the valid choices`() {
        ApplicationContext.run(Environment.CLI, Environment.TEST).use { ctx ->
            ByteArrayOutputStream().use { baos ->
                val originalErr = System.err
                System.setErr(PrintStream(baos))
                try {
                    PicocliRunner.call(GameOfLifeCommand::class.java, ctx, "-r", "foo")
                } finally {
                    System.setErr(originalErr)
                }

                expectThat(baos.toString()).contains("Invalid value for option '--renderer'")
                expectThat(baos.toString()).contains("terminal, korge")
            }
        }
    }

    @Test
    fun `the grid size parameter is passed into universe creation to return an error`() {
        ApplicationContext.run(Environment.CLI, Environment.TEST).use { ctx ->
            ByteArrayOutputStream().use { baos ->
                val originalOut = System.out
                System.setOut(PrintStream(baos))

                val args = arrayOf("-g", "2")
                val exitCode =
                    try {
                        PicocliRunner.call(GameOfLifeCommand::class.java, ctx, *args)
                    } finally {
                        System.setOut(originalOut)
                    }

                expectThat(exitCode).isEqualTo(1)
                expectThat(baos.toString()).contains("Error creating universe:")
                expectThat(baos.toString()).contains("The minimum grid size is 3")
            }
        }
    }

    @Test
    @Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    fun `the simulation stops when all cells have died`() {
        // A single cell has no neighbours, so it dies in the first generation
        val universe =
            Universe
                .create(gridSize = 3, aliveCells = setOf(Coordinate(1, 1)))
                .getOrElse { fail("Expected valid initial state") }
        val renderedGenerations = mutableListOf<Int>()
        val command = GameOfLifeCommand(terminalRenderer = noOpRenderer, korgeRenderer = noOpRenderer)

        ByteArrayOutputStream().use { baos ->
            val originalOut = System.out
            System.setOut(PrintStream(baos))
            try {
                command.runSimulation(universe) { _, generation -> renderedGenerations += generation }
            } finally {
                System.setOut(originalOut)
            }

            expectThat(renderedGenerations).containsExactly(0, 1)
            expectThat(baos.toString()).contains("All cells died at generation 1")
        }
    }
}
