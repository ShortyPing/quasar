package dev.vanadium.quasarplatform.runtime.processor

import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter

class ProcessedServiceTaskMethod(
    val beanInstance: Any,
    val delegate: KFunction<*>,
    val processTokenParameter: KParameter,
) {

    suspend fun call(processToken: ProcessToken) {
        delegate.callSuspendBy(mapOf(processTokenParameter to processToken, delegate.instanceParameter!! to beanInstance))

    }
}