package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor

class BpmnStartEvent(
    override val id: String,
    override val name: String?,
    override var outgoingFlow: List<String>
) : Identifiable, Named, Outgoing, Activity() {

    override suspend fun handle(processToken: ProcessToken, quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor) {}

}