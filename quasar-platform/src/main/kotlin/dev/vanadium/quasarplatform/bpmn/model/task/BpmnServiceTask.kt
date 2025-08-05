package dev.vanadium.quasarplatform.bpmn.model.task

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Incoming
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing

class BpmnServiceTask(
    override val id: String,
    override val name: String?,
    override val incomingFlow: List<String>,
    override val outgoingFlow: List<String>,
    val taskDefinition: String
) : Identifiable, Named, Incoming, Outgoing {
}