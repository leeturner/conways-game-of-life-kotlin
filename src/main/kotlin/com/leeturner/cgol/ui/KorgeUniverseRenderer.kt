package com.leeturner.cgol.ui

import com.leeturner.cgol.engine.Universe
import jakarta.inject.Named
import jakarta.inject.Singleton
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.Korge
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.korge.view.size
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.system.exitProcess

internal val ALIVE_CELL_COLOR: RGBA = Colors.GREEN
internal val DEAD_CELL_COLOR: RGBA = RGBA(40, 40, 40)

/**
 * Draws the universe in a KorGE window. The command loop pushes frames in via [render];
 * KorGE runs its own frame loop on a separate thread and draws whichever frame is latest.
 */
@Singleton
@Named("korge")
class KorgeUniverseRenderer : UniverseRenderer {
    // One reference so the KorGE thread never sees a universe from one generation with the number of another
    @Volatile private var latest: Frame? = null
    private val started = AtomicBoolean(false)

    override fun render(
        universe: Universe,
        generation: Int,
    ) {
        latest = Frame(universe, generation)
        if (started.compareAndSet(false, true)) startWindow(universe.gridSize)
    }

    private fun startWindow(gridSize: Int) {
        thread(isDaemon = true, name = "korge") {
            // KorGE already exits when its window closes; this also ends the simulation if the window never opens
            exitProcess(windowExitCode { runBlocking { openWindow(gridSize) } })
        }
    }

    private suspend fun openWindow(gridSize: Int) {
        Korge(windowSize = Size(WINDOW_SIZE, WINDOW_SIZE + HEADER_HEIGHT), title = "golk") {
            val bitmap = Bitmap32(gridSize, gridSize)
            val header = text("", color = Colors.WHITE)
            image(bitmap) {
                smoothing = false
                size(WINDOW_SIZE, WINDOW_SIZE)
                y = HEADER_HEIGHT.toDouble()
            }
            addUpdater {
                val frame = latest ?: return@addUpdater
                header.text = "Generation: ${frame.generation} | Population: ${frame.universe.population()}"
                bitmap.lock { fillBitmap(frame.universe, bitmap) }
            }
        }
    }

    private data class Frame(
        val universe: Universe,
        val generation: Int,
    )

    private companion object {
        const val WINDOW_SIZE = 800
        const val HEADER_HEIGHT = 24
    }
}

internal fun fillBitmap(
    universe: Universe,
    bitmap: Bitmap32,
) {
    bitmap.setEach { x, y -> if (universe.isAlive(x, y)) ALIVE_CELL_COLOR else DEAD_CELL_COLOR }
}

/** Runs the window and turns how it ended into an exit code, so a window that can't open isn't silent. */
internal fun windowExitCode(window: () -> Unit): Int =
    runCatching(window).fold(
        onSuccess = { 0 },
        onFailure = {
            System.err.println("Could not open the KorGE window: $it")
            1
        },
    )
