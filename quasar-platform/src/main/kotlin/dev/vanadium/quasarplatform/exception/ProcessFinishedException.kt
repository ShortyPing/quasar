package dev.vanadium.quasarplatform.exception

import java.util.UUID

class ProcessFinishedException(processId: UUID, message: String) : QuasarEngineException("Process $processId is finished: $message") {
}