package com.leeturner.cgol

import com.leeturner.cgol.engine.Universe
import com.leeturner.cgol.engine.UniverseCoordinatesOutOfBoundsError
import com.leeturner.cgol.engine.UniverseFactory
import com.leeturner.cgol.engine.UniverseMinimumSizeError
import com.leeturner.cgol.engine.UniverseNoAliveCells
import com.leeturner.cgol.ui.UniverseRenderer
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Inject
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
    @Inject private val universeFactory: UniverseFactory,
    @Inject private val universeRenderer: UniverseRenderer,
) : Callable<Int> {
    @Option(
        names = ["-g", "--grid-size"],
        description = ["The size of the grid (default: ${DEFAULT_GRID_SIZE}x${ DEFAULT_GRID_SIZE })"],
    )
    private var gridSize: Int = DEFAULT_GRID_SIZE

    override fun call(): Int {
        val universe = universeFactory.universeCreator().create(gridSize = gridSize)
        universe.fold(
            ifLeft = { error ->
                println("Error creating universe:")
                when (error) {
                    is UniverseCoordinatesOutOfBoundsError ->
                        println(
                            "The following coordinates are out of bounds: ${error.outOfBoundsCoordinates}",
                        )
                    is UniverseMinimumSizeError -> println("The minimum grid size is ${error.minimumGridSize}")
                    is UniverseNoAliveCells -> println("There are no alive cells in the initial state")
                }
                return 1 // error
            },
            ifRight = {
                runSimulation(it)
            },
        )

        return 0 // success
    }

    fun runSimulation(initialUniverse: Universe) {
        generateSequence(initialUniverse to 0) { (universe, generation) ->
            universe.tick() to generation + 1
        }.forEach { (universe, generation) ->
            universeRenderer.render(universe, generation)
            Thread.sleep(DELAY_BETWEEN_GENERATIONS_IN_MS)
        }
    }

    companion object {
        private const val DELAY_BETWEEN_GENERATIONS_IN_MS = 200L
        private const val DEFAULT_GRID_SIZE = 64

        @JvmStatic fun main(args: Array<String>) {
            val exitCode = PicocliRunner.call(GameOfLifeCommand::class.java, *args)
            exitProcess(exitCode ?: 0)
        }
    }
}
