package com.leeturner.cgol

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class GameOfLifeCommandTest {
    @Test
    fun testWithCommandLineOption() {
        ApplicationContext.run(Environment.CLI, Environment.TEST).use { ctx ->
            ByteArrayOutputStream().use { baos ->
                System.setOut(PrintStream(baos))

                val args = arrayOf("-v")
                val exitCode = PicocliRunner.call(GameOfLifeCommand::class.java, ctx, *args)

                expectThat(exitCode).isEqualTo(0)
                expectThat(baos.toString()).contains("Hi!")
            }
        }
    }
}
