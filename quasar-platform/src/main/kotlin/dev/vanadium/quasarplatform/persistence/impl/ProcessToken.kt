package dev.vanadium.quasarplatform.persistence.impl

import dev.vanadium.quasarplatform.api.QuasarEngine
import dev.vanadium.quasarplatform.bpmn.model.Activity
import dev.vanadium.quasarplatform.bpmn.model.BpmnEndEvent
import dev.vanadium.quasarplatform.bpmn.model.BpmnProcess
import dev.vanadium.quasarplatform.bpmn.model.BpmnServiceTask
import dev.vanadium.quasarplatform.bpmn.model.BpmnStartEvent
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import dev.vanadium.quasarplatform.exception.OptimisticLockException
import dev.vanadium.quasarplatform.persistence.DbBacked
import dev.vanadium.quasarplatform.persistence.model.ProcessTokenModel
import dev.vanadium.quasarplatform.persistence.repository.ProcessTokenModelRepository
import dev.vanadium.quasarplatform.properties.QuasarLockProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class ProcessToken internal constructor(
    override var id: UUID,
    override var representation: ProcessTokenModel,
    override var repository: ProcessTokenModelRepository,
    private val quasarEngine: QuasarEngine,
    private val lockProperties: QuasarLockProperties,
    private val bpmnProcess: BpmnProcess
) : DbBacked<ProcessTokenModel, ProcessTokenModelRepository> {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val heartbeatScope = CoroutineScope(Dispatchers.Default)
    private val runtimeScope = CoroutineScope(Dispatchers.Default)

    private val activeTokens = AtomicInteger(0)
    private val doneLatch = CountDownLatch(1)

    private var currentActivity: Activity = bpmnProcess.activities.first { it.id == representation.currentActivityId }

    /**
     * Sets a process variable for token
     */
    fun setVariable(key: String, value: String) {
        this.representation.variables[key] = value
        this.save()
    }

    /**
     * Retrieves a process variable by key
     */
    fun getVariable(key: String): String? {
        return this.representation.variables[key]
    }


    internal fun run() {
        runtimeScope.launch {
            while (true) {
                delay(1)
                handleActivity()
            }
        }
    }

    internal fun isLocked(): Boolean {
        return representation.lockWorkerId != null
                && representation.lockWorkerId != quasarEngine.workerId
                && representation.lockExpiration != null
                && representation.lockExpiration!!.isAfter(Instant.now())
    }

    internal fun acquireLock() {

        if (isLocked()) {
            throw OptimisticLockException("Tried to acquire lock for process token $id, but it is currently locked by worker ${representation.lockWorkerId}")
        }

        representation.lockWorkerId = quasarEngine.workerId
        representation.lockExpiration = Instant.now() + lockProperties.lockExpiration
        this.save()
    }

    internal fun releaseLock() {
        destroyHeartbeat()


        representation.lockWorkerId = null
        representation.lockExpiration = null
        this.save()
    }

    internal fun initiateHeartbeat() {
        this.heartbeatScope.launch {
            while (true) {
                delay(5_000)
                acquireLock()
            }
        }
    }

    private fun destroyHeartbeat() {
        this.heartbeatScope.cancel("Heartbeat explicitly destroyed via 'destroyHeartbeat' method")
    }


    private suspend fun handleActivity() {
        when (currentActivity) {
            is BpmnStartEvent -> {}
            is BpmnEndEvent -> {
                runtimeScope.cancel()
                // TODO: Implement proper cancellation in db
            }

            is BpmnServiceTask -> {
                logger.info("Running service task ${(currentActivity as BpmnServiceTask).taskDefinition}")
                delay(1000)
            }
        }

        advanceActivity()
    }

    private fun advanceActivity() {

        val atomicCurrentActivity = currentActivity

        if (atomicCurrentActivity !is Outgoing) {
            runtimeScope.cancel()

            logger.info("TODO: Stop the process")
            return
        }

        if (atomicCurrentActivity.outgoingFlow.isEmpty()) {
            runtimeScope.cancel()

            logger.info("TODO: Stop the process")
            return
        }

        if (atomicCurrentActivity.outgoingFlow.size > 1) {
            TODO("Multiple tokens")
        }

        val nextFlow = atomicCurrentActivity.outgoingFlow.first()

        val nextActivity = bpmnProcess.sequenceFlows.find { it.id == nextFlow }?.let { f ->
            bpmnProcess.activities.find { it.id == f.target }
                ?: throw IllegalStateException("The activity ${f.target} was not found in process, but is referenced in a flow")
        }
            ?: throw IllegalStateException("The flow $nextFlow was not found in process, but is referenced in activity ${atomicCurrentActivity.id}")

        representation.currentActivityId = nextActivity.id

        if (nextActivity is Named) {
            representation.currentActivityName = nextActivity.name
        }

        currentActivity = nextActivity

        save()

    }


    override fun save() {
        if (isLocked()) {
            throw OptimisticLockException("Tried to modify database state of locked process token $id")
        }
        super.save()
    }
}