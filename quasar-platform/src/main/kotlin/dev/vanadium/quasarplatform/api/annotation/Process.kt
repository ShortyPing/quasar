package dev.vanadium.quasarplatform.api.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Process(
    val id: String
)
