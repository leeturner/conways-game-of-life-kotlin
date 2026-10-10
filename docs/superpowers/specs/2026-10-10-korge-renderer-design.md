# KorGE Renderer Design

## Goal

Add a second `UniverseRenderer` that draws the universe in a [KorGE](https://korge.org/) 2D window,
selectable from the CLI. The terminal renderer stays the default. The engine (`Universe`) is unchanged.

## Scope

- View only: the window shows the grid plus a `Generation: N | Population: M` header.
- The existing command loop (`runSimulation`, 200ms per generation) still drives timing.
- Out of scope: pause/step controls, clicking to toggle cells, speed control, configurable KorGE colours.

## Renderer selection

- New picocli option `-r, --renderer` of enum type `RendererType { TERMINAL, KORGE }`, default `TERMINAL`.
- Both renderers are Micronaut `@Singleton`s with `@Named("terminal")` / `@Named("korge")`.
- `GameOfLifeCommand` injects both via constructor parameters `terminalRenderer` and `korgeRenderer`
  (picocli sets options after construction, so the choice is made in `call()`).
- `runSimulation(initialUniverse, renderer)` takes the chosen renderer as a parameter.

## `KorgeUniverseRenderer` (`ui/KorgeUniverseRenderer.kt`)

- `render(universe, generation)` stores the pair in a `@Volatile` field.
- On the first `render()` call it starts KorGE on a daemon thread, so terminal mode never opens a window.
- Scene:
  - one `Bitmap32(gridSize, gridSize)` displayed as an `Image`, scaled to fill an 800x800 window,
    with smoothing disabled (one pixel per cell; cheap even at the 256x256 maximum);
  - one `Text` header showing generation and population.
- An `addUpdater` block redraws the bitmap and header from the latest stored state each frame.
- Pure helper `fillBitmap(universe, bitmap)` writes alive (green) and dead (dark grey) pixels.
- Closing the window calls `exitProcess(0)`.

## Build

- Add `com.soywiz.korge:korge-jvm:6.0.0` to `gradle/libs.versions.toml` as a plain `implementation`
  dependency. The KorGE Gradle plugin is not used.
- Risk: KorGE 6.0.0 was built against Kotlin 2.0; this project uses Kotlin 2.4.20 and JDK 25.
  The first implementation step is a feasibility check that a window opens on macOS. If it fails,
  stop and revisit (pin versions, or move KorGE to a separate Gradle module).

## Known limitation

If the whole population dies, the command returns and `main` calls `exitProcess`, so the window
closes immediately. This is rare on a random toroidal grid and is accepted for now.

## Testing

- Unit test for `fillBitmap`: alive and dead pixels get the expected colours (no window needed).
- Update `GameOfLifeCommandTest` for the new `runSimulation` signature and constructor.
- Manual check: `./gradlew run --args="-r korge"` opens the window and the simulation animates.
