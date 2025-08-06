package dev.vanadium.quasarplatform.exception

class ProcessDefinitionNotFoundException(val processDefinitionId: String) : QuasarEngineException("The process definition $processDefinitionId could not be found") {
}