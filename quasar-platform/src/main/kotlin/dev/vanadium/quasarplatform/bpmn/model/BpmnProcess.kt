package dev.vanadium.quasarplatform.bpmn.model

class BpmnProcess(
    val id: String,
    val name: String?,
    val startEvent: BpmnStartEvent,
    val activities: List<Activity>,
    val sequenceFlows: List<BpmnSequenceFlow>,
) {


}