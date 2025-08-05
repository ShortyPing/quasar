package dev.vanadium.quasarplatform.bpmn.model.event

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Incoming
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing

class BpmnEndEvent(
    override val id: String,
    override val name: String?,
    override val incomingFlow: List<String>,
) : Identifiable, Named, Incoming {


}