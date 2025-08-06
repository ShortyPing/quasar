package dev.vanadium.quasarplatform

import dev.vanadium.quasarplatform.properties.QuasarLockProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    QuasarLockProperties::class
)
class QuasarPlatformApplication

fun main(args: Array<String>) {
    runApplication<QuasarPlatformApplication>(*args)
}
