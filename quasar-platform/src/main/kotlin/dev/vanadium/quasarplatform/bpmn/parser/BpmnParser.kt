package dev.vanadium.quasarplatform.bpmn.parser

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.vanadium.quasarplatform.bpmn.model.BpmnProcess
import dev.vanadium.quasarplatform.bpmn.model.BpmnEndEvent
import dev.vanadium.quasarplatform.bpmn.model.BpmnSequenceFlow
import dev.vanadium.quasarplatform.bpmn.model.BpmnServiceTask
import dev.vanadium.quasarplatform.bpmn.model.BpmnStartEvent
import dev.vanadium.quasarplatform.bpmn.parser.exception.BpmnMissingXmlAttributeException
import dev.vanadium.quasarplatform.bpmn.parser.exception.BpmnParseException
import dev.vanadium.quasarplatform.runtime.SimpleProcessRuntime
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils

@Service
class BpmnParser {


    private val xmlMapper = XmlMapper()

    @PostConstruct
    fun init() {
        val file = ResourceUtils.getFile("classpath:simple-example.bpmn")

        val process = parseBpmn(file.readText())

        SimpleProcessRuntime(process).startProcess()

    }


    fun parseBpmn(xml: String): BpmnProcess {
        val tree = xmlMapper.readTree(xml)

        val process = tree.findValue("process") ?: throw BpmnParseException("BPMN must have a `process` node")

        val processId = process.getRequiredText("id")
        val name = process.get("name")?.asText()

        val bpmnProcess = BpmnProcess(
            processId,
            name,
            parseStartEvent(process),
            parseSequenceFlows(process),
            parseServiceTasks(process),
            parseEndEvent(process)
        )

        return bpmnProcess
    }


    fun parseStartEvent(process: JsonNode): BpmnStartEvent {
        val startEvent =
            process.findValue("startEvent") ?: throw BpmnParseException("BPMN process must have a `startEvent` node")

        val id = startEvent.getRequiredText("id")
        val name = startEvent.get("name")?.asText()

        return BpmnStartEvent(id, name, parseOutgoing(startEvent))
    }

    fun parseEndEvent(process: JsonNode): List<BpmnEndEvent> {
        val endEvents = process.get("endEvent")



        return parseOneOrMultipleOccurrences(endEvents) { node ->
            val id = node.getRequiredText("id")
            val name = node.get("name")?.asText()

            BpmnEndEvent(id, name, parseIncoming(node))
        }
    }

    fun parseSequenceFlows(process: JsonNode): List<BpmnSequenceFlow> {


        val sequenceFlowNode = process.get("sequenceFlow")

        return parseOneOrMultipleOccurrences(sequenceFlowNode) { node ->
            val id = node.getRequiredText("id")
            val name = node.get("name")?.asText()
            val source = node.getRequiredText("sourceRef")
            val target = node.getRequiredText("targetRef")

            BpmnSequenceFlow(id, name, source, target)
        }
    }

    fun parseServiceTasks(process: JsonNode): List<BpmnServiceTask> {
        val serviceTaskNode = process.get("serviceTask")

        return parseOneOrMultipleOccurrences(serviceTaskNode) { node ->
            val id = node.getRequiredText("id")
            val name = node.get("name")?.asText()
            val incoming = parseIncoming(node)
            val outgoing = parseOutgoing(node)

            val taskDefinition =
                node.get("extensionElements")?.get("taskDefinition")?.get("type")?.asText() ?: throw BpmnParseException(
                    "The service task with id '$id' is required to have a task definition"
                )

            BpmnServiceTask(id, name, incoming, outgoing, taskDefinition)
        }
    }

    fun <T> parseOneOrMultipleOccurrences(node: JsonNode?, factory: (node: JsonNode) -> T): List<T> {

        if (node == null)
            return listOf()

        if (!node.isArray) {
            return listOf(factory(node))
        }

        return node.mapNotNull { factory(it) }
    }


    fun parseOutgoing(node: JsonNode): List<String> {
        return parseOneOrMultipleOccurrences(node.get("outgoing")) { node -> node.asText() }
    }

    fun parseIncoming(node: JsonNode): List<String> {
        return parseOneOrMultipleOccurrences(node.get("incoming")) { node -> node.asText() }
    }


    private fun JsonNode.getRequiredText(key: String): String {
        return this.get(key)?.asText() ?: throw BpmnMissingXmlAttributeException(key)
    }
}