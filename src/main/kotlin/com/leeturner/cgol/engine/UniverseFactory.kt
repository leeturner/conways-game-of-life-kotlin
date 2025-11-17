package com.leeturner.cgol.engine

import arrow.core.Either
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class UniverseFactory {
    @Singleton
    fun universeCreator(): UniverseCreator =
        object : UniverseCreator {
            override fun create(): Either<UniverseCreationError, Universe> = Universe.create()

            override fun create(gridSize: Int): Either<UniverseCreationError, Universe> =
                Universe.create(gridSize = gridSize)

            override fun create(
                gridSize: Int,
                aliveCells: Set<Coordinate>,
            ): Either<UniverseCreationError, Universe> = Universe.create(gridSize = gridSize, aliveCells = aliveCells)
        }
}

interface UniverseCreator {
    fun create(): Either<UniverseCreationError, Universe>

    fun create(gridSize: Int): Either<UniverseCreationError, Universe>

    fun create(
        gridSize: Int,
        aliveCells: Set<Coordinate>,
    ): Either<UniverseCreationError, Universe>
}
