package dev.vanadium.quasarplatform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QuasarPlatformApplication

fun main(args: Array<String>) {
    runApplication<QuasarPlatformApplication>(*args)
}
