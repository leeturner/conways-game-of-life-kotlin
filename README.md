# Conway's Game of Life – Kotlin

A terminal-based implementation of [Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life)
written in Kotlin, using the Micronaut framework and functional programming with Arrow.

## Overview

This project implements the classic cellular automaton devised by mathematician John Conway. The simulation runs
in your terminal with colorized output, displaying generations of cells evolving, according to simple rules that
create complex and fascinating patterns.

### Features

- **Toroidal Universe**: The grid wraps around like a torus - cells at edges interact with cells on opposite edges
- **Optimized Algorithm**: Only checks alive cells and their neighbors instead of the entire grid for better performance
- **Terminal Rendering**: Real-time visualization in your terminal with ANSI color support
- **Configurable Grid Size**: Customize the universe dimensions with command-line options
- **Random Initial State**: Generates random starting configurations for exploration
- **Functional Error Handling**: Uses Arrow's `Either` type for robust error handling

## The Rules

Conway's Game of Life follows four simple rules:

1. **Underpopulation**: Any live cell with fewer than two live neighbours dies
2. **Survival**: Any live cell with two or three live neighbours lives on to the next generation
3. **Overpopulation**: Any live cell with more than three live neighbours dies
4. **Reproduction**: Any dead cell with exactly three live neighbours becomes a live cell

## Technology Stack

- **Language**: Kotlin with JVM 21
- **Framework**: Micronaut (for dependency injection and CLI)
- **CLI**: Picocli (command-line interface)
- **Functional Programming**: Arrow Core (for Either, functional error handling)
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 5 with Strikt assertions
- **Code Quality**: Detekt (static analysis) and Kotlinter (formatting)

## Requirements

- Java 21 or higher
- Gradle (wrapper included)

## Building

Build the project using the Gradle wrapper:

```bash
./gradlew build
```

This will:
- Compile the Kotlin source code
- Run all tests
- Run code quality checks (Detekt, Kotlinter)
- Create executable distributions

## Running

### Using Gradle

Run with default settings (64x64 grid):

```bash
./gradlew run
```

Run with custom grid size:

```bash
./gradlew run --args="--grid-size 32"
```

### Using the Distribution

After building, you can run the standalone application:

```bash
./build/distributions/golk/bin/golk
```

Or with options:

```bash
./build/distributions/golk/bin/golk --grid-size 100
```

### Command-Line Options

- `-g, --grid-size <size>`: Set the grid size (default: 64)
- `-h, --help`: Show help message and exit

## Project Structure

```
src/
├── main/kotlin/com/leeturner/cgol/
│   ├── GameOfLifeCommand.kt         # CLI entry point and simulation runner
│   ├── engine/
│   │   ├── Universe.kt              # Core game logic and rules
│   │   └── UniverseFactory.kt       # Factory for creating universes
│   └── ui/
│       └── UniverseRenderer.kt      # Terminal rendering with ANSI colors
└── test/kotlin/com/leeturner/cgol/
    └── engine/
        ├── UniverseCreationTests.kt # Tests for universe validation
        └── UniverseRulesTests.kt    # Tests for Conway's rules and patterns
```

## Configuration

The application can be configured via `src/main/resources/application.properties`:

- `universe.renderer.alive-cell-color`: ANSI color code for alive cells (default: green)
- `universe.renderer.dead-cell-color`: ANSI color code for dead cells (default: gray)

Available ANSI color codes:
- `\u001b[31m` - Red
- `\u001b[32m` - Green
- `\u001b[33m` - Yellow
- `\u001b[34m` - Blue
- `\u001b[35m` - Magenta
- `\u001b[36m` - Cyan
- `\u001b[90m` - Gray
- `\u001b[91m` - Bright Red
- `\u001b[92m` - Bright Green
- `\u001b[94m` - Bright Blue

## Testing

Run all tests:

```bash
./gradlew test
```

The test suite includes:
- **Rules Tests**: Verification of all four Conway's Game of Life rules
- **Toroidal Universe Tests**: Ensures proper wrapping behavior at grid edges
- **Still Life Patterns**: Tests for stable patterns (block, bee-hive, tub)
- **Oscillator Patterns**: Tests for repeating patterns (blinker, toad, beacon)
- **Universe Creation**: Validation tests for grid size, coordinates, and initial state

## Code Quality

Run static analysis with Detekt:

```bash
./gradlew detekt
```

Check code formatting with Kotlinter:

```bash
./gradlew lintKotlin
```

Auto-format code:

```bash
./gradlew formatKotlin
```

## Implementation Details

### Universe Representation

The universe is represented as a `Set<Coordinate>` containing only alive cells. This sparse representation:
- Reduces memory usage for typical Game of Life patterns
- Speeds up the tick algorithm by avoiding checks of empty space
- Allows for efficient lookup of cell states

### Tick Algorithm Optimization

Instead of checking all `gridSize × gridSize` cells each generation, the algorithm:
1. Collects all alive cells
2. Gathers all neighbors of alive cells
3. Only evaluates rules for this combined set
4. Typically processes far fewer cells than the full grid

This optimization is particularly effective for sparse populations, which are common in Conway's Game of Life.

### Error Handling

The project uses functional error handling with Arrow's `Either` type:
- `UniverseMinimumSizeError`: Grid size must be at least 3×3
- `UniverseCoordinatesOutOfBoundsError`: Initial cells must be within grid bounds
- `UniverseNoAliveCells`: Universe must have at least one alive cell

## CI/CD

The project includes GitHub Actions workflows:
- **Build Workflow**: Compiles and tests on every push/PR to main
- **Dependabot**: Automated dependency updates and security alerts

## License

This is a personal project for educational purposes.

## Author

Created by Lee Turner


