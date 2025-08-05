package dev.vanadium.quasarplatform.bpmn.parser.exception

class BpmnMissingXmlAttributeException(attributeName: String): BpmnParseException
    ("The attribute `$attributeName` is missing") {
}