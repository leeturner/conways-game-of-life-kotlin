package com.leeturner.cgol

import com.leeturner.cgol.engine.Universe
import com.leeturner.cgol.engine.UniverseCoordinatesOutOfBoundsError
import com.leeturner.cgol.engine.UniverseMaximumSizeError
import com.leeturner.cgol.engine.UniverseMinimumSizeError
import com.leeturner.cgol.engine.UniverseNoAliveCells
import com.leeturner.cgol.ui.UniverseRenderer
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Inject
import jakarta.inject.Named
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "golk",
    description = ["Conway's Game of Life in Kotlin with Micronaut"],
    mixinStandardHelpOptions = true,
)
class GameOfLifeCommand(
    @Inject @param:Named("terminal") private val terminalRenderer: UniverseRenderer,
    @param:Named("korge") private val korgeRenderer: UniverseRenderer,
) : Callable<Int> {
    @Option(
        names = ["-g", "--grid-size"],
        description = ["The size of the grid (default: ${Universe.DEFAULT_GRID_SIZE}x${Universe.DEFAULT_GRID_SIZE})"],
    )
    private var gridSize: Int = Universe.DEFAULT_GRID_SIZE

    @Option(
        names = ["-r", "--renderer"],
        paramLabel = "<renderer>",
        description = ["Where to draw the universe: \${COMPLETION-CANDIDATES} (default: \${DEFAULT-VALUE})"],
    )
    private var rendererType: RendererType = RendererType.terminal

    internal fun selectedRenderer(): UniverseRenderer =
        when (rendererType) {
            RendererType.terminal -> terminalRenderer
            RendererType.korge -> korgeRenderer
        }

    override fun call(): Int {
        val universe = Universe.create(gridSize = gridSize)
        universe.fold(
            ifLeft = { error ->
                println("Error creating universe:")
                when (error) {
                    is UniverseCoordinatesOutOfBoundsError -> {
                        println(
                            "The following coordinates are out of bounds: ${error.outOfBoundsCoordinates}",
                        )
                    }

                    is UniverseMinimumSizeError -> {
                        println("The minimum grid size is ${error.minimumGridSize}")
                    }

                    is UniverseMaximumSizeError -> {
                        println("The maximum grid size is ${error.maximumGridSize}")
                    }

                    is UniverseNoAliveCells -> {
                        println("There are no alive cells in the initial state")
                    }
                }
                return 1 // error
            },
            ifRight = {
                runSimulation(it, selectedRenderer())
            },
        )

        return 0 // success
    }

    fun runSimulation(
        initialUniverse: Universe,
        renderer: UniverseRenderer,
    ) {
        generateSequence(initialUniverse to 0) { (universe, generation) ->
            universe.tick() to generation + 1
        }.forEach { (universe, generation) ->
            renderer.render(universe, generation)
            if (universe.population() == 0) {
                println("All cells died at generation $generation")
                return
            }
            Thread.sleep(DELAY_BETWEEN_GENERATIONS_IN_MS)
        }
    }

    companion object {
        private const val DELAY_BETWEEN_GENERATIONS_IN_MS = 200L

        @JvmStatic fun main(args: Array<String>) {
            val exitCode = PicocliRunner.call(GameOfLifeCommand::class.java, *args)
            exitProcess(exitCode ?: 0)
        }
    }
}

// Lowercase so `-r korge` works: picocli matches enum values case-sensitively
@Suppress("EnumNaming", "ktlint:standard:enum-entry-name-case")
enum class RendererType { terminal, korge }
