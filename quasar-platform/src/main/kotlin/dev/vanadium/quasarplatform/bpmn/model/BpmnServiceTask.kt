package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Incoming
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor
import kotlinx.coroutines.delay

class BpmnServiceTask(
    override val id: String,
    override val name: String?,
    override val incomingFlow: List<String>,
    override val outgoingFlow: List<String>,
    val taskDefinition: String
) : Identifiable, Named, Incoming, Outgoing, Activity() {

    override suspend fun handle(
        processToken: ProcessToken,
        quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor
    ) {
        quasarAnnotationBeanProcessor.callServiceTask(processToken.bpmnProcess.id, taskDefinition, processToken)
    }
}