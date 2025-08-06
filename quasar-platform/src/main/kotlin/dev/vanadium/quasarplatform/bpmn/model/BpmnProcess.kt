package dev.vanadium.quasarplatform.bpmn.model

class BpmnProcess(
    val id: String,
    val name: String?,
    val startEvent: BpmnStartEvent,
    val sequenceFlows: List<BpmnSequenceFlow>,
    val serviceTasks: List<BpmnServiceTask>,
    val endEvents: List<BpmnEndEvent>
) {


}