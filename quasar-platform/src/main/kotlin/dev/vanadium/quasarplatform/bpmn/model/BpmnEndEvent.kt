package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Incoming
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor
import kotlinx.coroutines.cancel

class BpmnEndEvent(
    override val id: String,
    override val name: String?,
    override val incomingFlow: List<String>,
) : Identifiable, Named, Incoming, Activity() {


    override suspend fun handle(processToken: ProcessToken, quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor) {
        processToken.end()
    }

}