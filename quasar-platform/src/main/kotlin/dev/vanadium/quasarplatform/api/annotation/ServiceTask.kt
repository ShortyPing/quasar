package dev.vanadium.quasarplatform.api.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ServiceTask(
    val value: String
)
