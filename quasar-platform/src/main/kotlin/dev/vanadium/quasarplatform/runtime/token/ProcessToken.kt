package dev.vanadium.quasarplatform.runtime.token

import java.util.UUID

class ProcessToken(
    processInstance: ProcessToken?
) {
    val processInstance: ProcessToken = processInstance ?: this
    val processTokenId = UUID.randomUUID()

}