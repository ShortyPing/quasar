package dev.vanadium.quasarplatform.bpmn.parser.exception

import java.lang.RuntimeException

open class BpmnParseException(message: String) : RuntimeException(message) {
}