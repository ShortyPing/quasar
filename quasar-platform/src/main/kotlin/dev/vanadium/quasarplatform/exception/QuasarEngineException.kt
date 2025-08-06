package dev.vanadium.quasarplatform.exception

sealed class QuasarEngineException(message: String) : RuntimeException(message) {
}