package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Named

class BpmnSequenceFlow(override val id: String, override val name: String?, val source: String, val target: String) : Identifiable, Named {
}