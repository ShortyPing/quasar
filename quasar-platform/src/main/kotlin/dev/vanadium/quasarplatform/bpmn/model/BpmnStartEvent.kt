package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing

class BpmnStartEvent(
    override val id: String,
    override val name: String?,
    override var outgoingFlow: List<String>
) : Identifiable, Named, Outgoing, Activity {



}