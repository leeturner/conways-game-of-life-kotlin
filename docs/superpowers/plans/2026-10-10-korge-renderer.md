# KorGE Renderer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a KorGE 2D window renderer for the universe, selectable with `-r korge`, keeping the terminal renderer as the default.

**Architecture:** `KorgeUniverseRenderer` implements the existing push-style `UniverseRenderer`. It stores the latest frame in a `@Volatile` field and, on the first `render()`, starts KorGE on a daemon thread; a KorGE `addUpdater` copies the latest frame into a `Bitmap32` (one pixel per cell) shown as a scaled, unsmoothed `Image`. `GameOfLifeCommand` injects both renderers by `@Named` qualifier and picks one from a new `--renderer` option.

**Tech Stack:** Kotlin 2.4.20, JDK 25, Micronaut 5 + picocli 4.7.7, KorGE 6.0.0 (`com.soywiz.korge:korge-jvm`), JUnit 5 + Strikt.

**Spec:** `docs/superpowers/specs/2026-10-10-korge-renderer-design.md`

## Global Constraints

- KorGE dependency: `com.soywiz.korge:korge-jvm:6.0.0`, plain `implementation`, no KorGE Gradle plugin.
- Engine (`engine/Universe.kt`) is not modified.
- Terminal renderer stays the default; option is `-r, --renderer`, values `terminal` / `korge`.
- Window: 800x800 grid area plus a 24px header strip (so the header never covers cells), alive cells green, dead cells dark grey, header `Generation: N | Population: M`.
- View only: no pause/step, no cell toggling, no speed control, no configurable KorGE colours.
- Commits use `git commit --no-gpg-sign` and end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- `./gradlew build` (tests + detekt + kotlinter) must pass at the end of every task.

## Review Focus

- An unknown renderer value (`-r foo`) must fail with picocli's "Invalid value" error listing `terminal, korge`, and never start the simulation. → Task 3, Step 1.
- No `-r` flag must still use the terminal renderer (existing behaviour unchanged). → Task 3, Step 1.
- Lowercase `-r korge` (as documented) must select the KorGE renderer; picocli enums are case-sensitive, so the enum constants are lowercase. → Task 3, Step 1.
- `fillBitmap` must not transpose x/y or drop edge cells (`(0,0)` and `(gridSize-1, gridSize-1)`). → Task 2, Step 1.
- The bitmap is reused across frames, so a cell that dies must be repainted dead, not left green. → Task 2, Step 1.

(Terminal mode must not open a window or touch AWT: covered by the existing `GameOfLifeCommandTest` grid-size test, which now constructs both renderers through Micronaut and must still pass headless.)

---

### Task 1: KorGE dependency + feasibility check

Proves KorGE 6.0.0 compiles under Kotlin 2.4.20 and opens a window on JDK 25 when started from a non-main thread, which is exactly how the renderer will run. **If this task fails, stop and report back; do not continue to Task 2.**

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Create (throwaway, deleted in Step 6): `src/main/kotlin/com/leeturner/cgol/ui/KorgeSpike.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `libs.korge` on the main classpath; KorGE JVM flags in `application.applicationDefaultJvmArgs`.

- [ ] **Step 1: Add the dependency to the version catalog**

In `gradle/libs.versions.toml`, under `[versions]` `# Libraries` add:

```toml
korge = "6.0.0"
```

Under `[libraries]` add (after the Arrow entry):

```toml
# KorGE
korge = { module = "com.soywiz.korge:korge-jvm", version.ref = "korge" }
```

- [ ] **Step 2: Wire the dependency and KorGE's JVM flags into the build**

In `build.gradle.kts`, add to `dependencies` after `implementation(libs.arrow.core)`:

```kotlin
  implementation(libs.korge)
```

Replace the `application { ... }` block with:

```kotlin
application {
    mainClass = "com.leeturner.cgol.GameOfLifeCommand"
    // KorGE's AWT/OpenGL window reflects into JDK internals and loads native code
    applicationDefaultJvmArgs =
        listOf(
            "java.desktop/sun.java2d.opengl",
            "java.desktop/java.awt",
            "java.desktop/sun.awt",
            "java.desktop/sun.lwawt",
            "java.desktop/sun.lwawt.macosx",
            "java.desktop/com.apple.eawt",
            "java.desktop/com.apple.eawt.event",
            "java.desktop/sun.awt.X11",
        ).map { "--add-opens=$it=ALL-UNNAMED" } + "--enable-native-access=ALL-UNNAMED"
}
```

(The package list is KorGE's own `jvmAddOpensList()` from `korlibs.korge.EnsureAddOpens`.)

- [ ] **Step 3: Build to prove the dependency resolves and existing checks pass**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`. If Kotlin reports incompatible metadata from KorGE classes, stop and report.

- [ ] **Step 4: Write the throwaway spike**

Create `src/main/kotlin/com/leeturner/cgol/ui/KorgeSpike.kt`:

```kotlin
package com.leeturner.cgol.ui

import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.view.text
import korlibs.math.geom.Size
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

fun main() {
    thread(isDaemon = true) {
        runBlocking {
            Korge(windowSize = Size(400, 400), title = "spike", backgroundColor = Colors.BLACK) {
                text("hello from korge", color = Colors.GREEN)
            }
        }
    }
    Thread.sleep(5000)
}
```

If `kotlinx.coroutines.runBlocking` does not resolve, add to the catalog `kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.9.0" }` and `implementation(libs.kotlinx.coroutines.core)`, then rebuild.

- [ ] **Step 5: Run the spike**

Run:

```bash
./gradlew installDist -x detekt -x lintKotlin && \
java $(for p in sun.java2d.opengl java.awt sun.awt sun.lwawt sun.lwawt.macosx com.apple.eawt com.apple.eawt.event sun.awt.X11; do printf -- '--add-opens=java.desktop/%s=ALL-UNNAMED ' $p; done) \
  --enable-native-access=ALL-UNNAMED \
  -cp "build/install/golk/lib/*" com.leeturner.cgol.ui.KorgeSpikeKt
```

Expected: a 400x400 window with green "hello from korge" text appears for ~5 seconds, then the process exits 0. Record any stack trace verbatim. If no window appears or it throws, stop and report.

- [ ] **Step 6: Delete the spike and commit**

```bash
rm src/main/kotlin/com/leeturner/cgol/ui/KorgeSpike.kt
./gradlew build
git add gradle/libs.versions.toml build.gradle.kts
git commit --no-gpg-sign -m "build: add KorGE dependency and its JVM flags

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

Expected: `BUILD SUCCESSFUL`, commit created, spike file not in the commit.

---

### Task 2: `KorgeUniverseRenderer`

**Files:**
- Create: `src/main/kotlin/com/leeturner/cgol/ui/KorgeUniverseRenderer.kt`
- Test: `src/test/kotlin/com/leeturner/cgol/ui/KorgeUniverseRendererTest.kt`

**Interfaces:**
- Consumes: `UniverseRenderer` (`fun render(universe: Universe, generation: Int)`) from `ui/UniverseRenderer.kt`; `Universe.gridSize`, `Universe.isAlive(x, y)`, `Universe.population()`.
- Produces: `@Singleton @Named("korge") class KorgeUniverseRenderer : UniverseRenderer`; top-level `internal fun fillBitmap(universe: Universe, bitmap: Bitmap32)`; `internal val ALIVE_CELL_COLOR: RGBA`, `internal val DEAD_CELL_COLOR: RGBA`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/kotlin/com/leeturner/cgol/ui/KorgeUniverseRendererTest.kt`:

```kotlin
package com.leeturner.cgol.ui

import arrow.core.getOrElse
import com.leeturner.cgol.engine.Coordinate
import com.leeturner.cgol.engine.Universe
import korlibs.image.bitmap.Bitmap32
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.isEqualTo

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
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests 'com.leeturner.cgol.ui.KorgeUniverseRendererTest'`
Expected: compilation FAIL with `Unresolved reference 'fillBitmap'` (and `ALIVE_CELL_COLOR`, `DEAD_CELL_COLOR`).

- [ ] **Step 3: Write the implementation**

Create `src/main/kotlin/com/leeturner/cgol/ui/KorgeUniverseRenderer.kt`:

```kotlin
package com.leeturner.cgol.ui

import com.leeturner.cgol.engine.Universe
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.Korge
import korlibs.korge.view.addUpdater
import korlibs.korge.view.image
import korlibs.korge.view.size
import korlibs.korge.view.text
import korlibs.math.geom.Size
import jakarta.inject.Named
import jakarta.inject.Singleton
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
            runBlocking {
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
            exitProcess(0) // KorGE already exits when its window closes; this covers any other way its loop ends
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
```

Notes for the implementer:
- `bitmap.lock { }` bumps the bitmap's `contentVersion`, which is what makes KorGE re-upload the texture; without it the window shows only the first frame.
- `addUpdater`'s lambda receives `dt: Duration`; it is unused here.
- If `size(...)` on `Image` does not resolve, use `scale = WINDOW_SIZE.toDouble() / gridSize` instead; the goal is the bitmap filling 800x800.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests 'com.leeturner.cgol.ui.KorgeUniverseRendererTest'`
Expected: PASS (2 tests).

- [ ] **Step 5: Keep the command's injection unambiguous until Task 3**

`GameOfLifeCommand` still injects a bare `UniverseRenderer`, and there are now two beans, so Micronaut would throw `NonUniqueBeanException`. In `src/main/kotlin/com/leeturner/cgol/ui/UniverseRenderer.kt` add `import io.micronaut.context.annotation.Primary` and annotate `SimpleTerminalUniverseRenderer` with `@Primary` (below `@Singleton`). Task 3 removes it.

- [ ] **Step 6: Full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`, including the existing `GameOfLifeCommandTest` (which constructs the command through Micronaut and must not open a window).

- [ ] **Step 7: Commit**

```bash
git add src/main/kotlin/com/leeturner/cgol/ui/KorgeUniverseRenderer.kt \
        src/test/kotlin/com/leeturner/cgol/ui/KorgeUniverseRendererTest.kt \
        src/main/kotlin/com/leeturner/cgol/ui/UniverseRenderer.kt
git commit --no-gpg-sign -m "feat: add a KorGE window renderer for the universe

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: `--renderer` option, wiring, README, manual check

**Files:**
- Modify: `src/main/kotlin/com/leeturner/cgol/GameOfLifeCommand.kt`
- Modify: `src/main/kotlin/com/leeturner/cgol/ui/UniverseRenderer.kt` (the `SimpleTerminalUniverseRenderer` annotations)
- Modify: `src/test/kotlin/com/leeturner/cgol/GameOfLifeCommandTest.kt`
- Modify: `README.md`

**Interfaces:**
- Consumes: `@Named("korge") KorgeUniverseRenderer` (Task 2); `SimpleTerminalUniverseRenderer`.
- Produces: `enum class RendererType { terminal, korge }`; `GameOfLifeCommand(terminalRenderer: UniverseRenderer, korgeRenderer: UniverseRenderer)`; `internal fun selectedRenderer(): UniverseRenderer`; `fun runSimulation(initialUniverse: Universe, renderer: UniverseRenderer)`.

- [ ] **Step 1: Write the failing tests**

In `src/test/kotlin/com/leeturner/cgol/GameOfLifeCommandTest.kt`:

Add imports:

```kotlin
import com.leeturner.cgol.ui.UniverseRenderer
import picocli.CommandLine
import strikt.assertions.isSameInstanceAs
```

Replace the body of `the simulation stops when all cells have died` from `val renderedGenerations` down to the `command.runSimulation(universe)` call so it reads:

```kotlin
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
```

(the two `expectThat` lines after it stay unchanged).

Add these tests and the helper inside the class:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests 'com.leeturner.cgol.GameOfLifeCommandTest'`
Expected: compilation FAIL (`No parameter with name 'terminalRenderer'`, `Unresolved reference 'selectedRenderer'`).

- [ ] **Step 3: Qualify the terminal renderer**

In `src/main/kotlin/com/leeturner/cgol/ui/UniverseRenderer.kt`, add `import jakarta.inject.Named` and annotate the class, and remove the `@Primary` annotation and its import added as a bridge in Task 2:

```kotlin
@Singleton
@Named("terminal")
class SimpleTerminalUniverseRenderer(
```

- [ ] **Step 4: Add the option and selection to the command**

In `src/main/kotlin/com/leeturner/cgol/GameOfLifeCommand.kt`:

Add import `jakarta.inject.Named`.

Replace the constructor:

```kotlin
class GameOfLifeCommand(
    @param:Named("terminal") private val terminalRenderer: UniverseRenderer,
    @param:Named("korge") private val korgeRenderer: UniverseRenderer,
) : Callable<Int> {
```

Add after the `gridSize` option:

```kotlin
    @Option(
        names = ["-r", "--renderer"],
        description = ["Where to draw the universe: \${COMPLETION-CANDIDATES} (default: \${DEFAULT-VALUE})"],
    )
    private var renderer: RendererType = RendererType.terminal

    internal fun selectedRenderer(): UniverseRenderer =
        when (renderer) {
            RendererType.terminal -> terminalRenderer
            RendererType.korge -> korgeRenderer
        }
```

Change `ifRight = { runSimulation(it) }` to:

```kotlin
            ifRight = {
                runSimulation(it, selectedRenderer())
            },
```

Change `runSimulation` to take the renderer:

```kotlin
    fun runSimulation(
        initialUniverse: Universe,
        renderer: UniverseRenderer,
    ) {
        generateSequence(initialUniverse to 0) { (universe, generation) ->
            universe.tick() to generation + 1
        }.forEach { (universe, generation) ->
            renderer.render(universe, generation)
```

(rest of the loop unchanged).

Add at the bottom of the file, after the class:

```kotlin
// Lowercase so `-r korge` works: picocli matches enum values case-sensitively
@Suppress("EnumNaming", "ktlint:standard:enum-entry-name-case")
enum class RendererType { terminal, korge }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests 'com.leeturner.cgol.GameOfLifeCommandTest'`
Expected: PASS (5 tests).

- [ ] **Step 6: Document the option in the README**

In `README.md`, in the "Using Gradle" section after the custom grid size example, add:

````markdown
Run in a KorGE 2D window instead of the terminal:

```bash
./gradlew run --args="--renderer korge"
```
````

In the "Features" list add:

```markdown
- **KorGE Window Rendering**: Optionally draw the universe in a [KorGE](https://korge.org/) 2D window with `--renderer korge`
```

- [ ] **Step 7: Full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL` (tests, detekt, kotlinter all pass).

- [ ] **Step 8: Manual check in the real app**

Run: `./gradlew run --args="-r korge"`
Expected: an 800x824 window titled "golk" shows a 64x64 grid of green/grey cells animating every 200ms, with the header counting up. Closing the window ends the Gradle run. Also run `./gradlew run --args="-r korge -g 256"` (cells are ~3px, still animating) and `./gradlew run` (terminal output as before, no window).

- [ ] **Step 9: Commit**

```bash
git add src/main/kotlin/com/leeturner/cgol/GameOfLifeCommand.kt \
        src/main/kotlin/com/leeturner/cgol/ui/UniverseRenderer.kt \
        src/test/kotlin/com/leeturner/cgol/GameOfLifeCommandTest.kt \
        README.md
git commit --no-gpg-sign -m "feat: choose the terminal or KorGE renderer with --renderer

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
