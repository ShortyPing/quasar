package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.event.BpmnEndEvent
import dev.vanadium.quasarplatform.bpmn.model.flow.BpmnSequenceFlow
import dev.vanadium.quasarplatform.bpmn.model.task.BpmnServiceTask
import dev.vanadium.quasarplatform.bpmn.model.event.BpmnStartEvent

class BpmnProcess(
    val id: String,
    val name: String?,
    val startEvent: BpmnStartEvent,
    val sequenceFlows: List<BpmnSequenceFlow>,
    val serviceTasks: List<BpmnServiceTask>,
    val endEvents: List<BpmnEndEvent>
) {


}