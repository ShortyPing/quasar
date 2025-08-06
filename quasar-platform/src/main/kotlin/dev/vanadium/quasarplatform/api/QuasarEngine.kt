package dev.vanadium.quasarplatform.api

import dev.vanadium.quasarplatform.persistence.impl.ProcessToken


interface QuasarEngine {
    val workerId: String

    fun startProcess(processDefinitionId: String): ProcessToken
}