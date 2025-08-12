package dev.vanadium.quasarplatform.exception

import java.lang.RuntimeException

class ActivityCallFailedException(processId: String, activityId: String, message: String) : QuasarEngineException("Failed to call activity '${activityId}' for process '${processId}': $message") {
}