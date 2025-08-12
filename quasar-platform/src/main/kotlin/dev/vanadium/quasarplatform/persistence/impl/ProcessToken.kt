package dev.vanadium.quasarplatform.persistence.impl

import dev.vanadium.quasarplatform.api.QuasarEngine
import dev.vanadium.quasarplatform.bpmn.model.Activity
import dev.vanadium.quasarplatform.bpmn.model.BpmnProcess
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import dev.vanadium.quasarplatform.exception.OptimisticLockException
import dev.vanadium.quasarplatform.exception.ProcessFinishedException
import dev.vanadium.quasarplatform.executeTxWithRetry
import dev.vanadium.quasarplatform.persistence.DbBacked
import dev.vanadium.quasarplatform.persistence.model.ProcessTokenModel
import dev.vanadium.quasarplatform.persistence.model.TokenStatus
import dev.vanadium.quasarplatform.persistence.repository.ProcessTokenModelRepository
import dev.vanadium.quasarplatform.properties.QuasarLockProperties
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.pow

class ProcessToken internal constructor(
    override var id: UUID,
    override var delegate: ProcessTokenModel,
    override var repository: ProcessTokenModelRepository,
    internal val bpmnProcess: BpmnProcess,
    private val quasarEngine: QuasarEngine,
    private val lockProperties: QuasarLockProperties,
    private val quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor,
    internal val transactionTemplate: TransactionTemplate
) : DbBacked<ProcessTokenModel, ProcessTokenModelRepository>(id, delegate, repository) {


    private val logger = LoggerFactory.getLogger(javaClass)
    internal val heartbeatScope = CoroutineScope(Dispatchers.Default)
    internal val runtimeScope = CoroutineScope(Dispatchers.Default)

    internal val activeTokens = AtomicInteger(0)
    internal val doneLatch = CountDownLatch(1)

    private var currentActivity: Activity = bpmnProcess.activities.first { it.id == delegate.currentActivityId }

    /**
     * Sets a process variable for token
     */
    fun setVariable(key: String, value: String) {
        this.delegate.variables[key] = value
        this.save()
    }

    /**
     * Retrieves a process variable by key
     */
    fun getVariable(key: String): String? {
        return this.delegate.variables[key]
    }


    fun end() {

        try {

            if (!tryAcquireLock()) {
                return
            }

            if (!delegate.isActive && delegate.status == TokenStatus.FINISHED) {
                return
            }

            val freshDelegate = repository.findById(id).orElse(null) ?: return

            if (!freshDelegate.isActive && freshDelegate.status == TokenStatus.FINISHED) {
                delegate = freshDelegate
                return
            }

            delegate = freshDelegate

            runtimeScope.cancel()
            heartbeatScope.cancel()
            releaseLock()


            val updated = repository.atomicFinish(id, TokenStatus.FINISHED)

            if (updated > 0)
                logger.info("Process token finished: $id")

        } catch (e: jakarta.persistence.OptimisticLockException) {
            logger.debug("Token {} was already ended by another thread", id)
        }
    }


    fun isFinished() = delegate.status == TokenStatus.FINISHED

    internal fun run() {
        runtimeScope.launch {
            while (true) {
                step()
            }
        }
    }

    internal fun runWithStart(activity: Activity) {
        runtimeScope.launch {
            moveTo(activity)
            advanceActivity()
            step()
        }
    }

    private suspend fun step() {
        logger.debug("{} - coroutineActive: {}", id, runtimeScope.isActive)
        delay(1000)
        logger.info("Handling $id at ${(currentActivity as Named).name}")
        handleActivity()
    }


    internal fun isLocked(): Boolean {
        return delegate.lockWorkerId != null
                && delegate.lockWorkerId != quasarEngine.workerId
                && delegate.lockExpiration != null
                && delegate.lockExpiration!!.isAfter(Instant.now())
    }

    internal fun acquireLock() {
        val success = tryAcquireLock()

        if (!success) {
            throw OptimisticLockException("Tried to acquire lock for process token $id, but it is currently locked by worker ${delegate.lockWorkerId}")
        }
    }

    internal fun tryAcquireLock(): Boolean {
        if (isLocked())
            return false

        delegate.lockWorkerId = quasarEngine.workerId
        delegate.lockExpiration = Instant.now() + lockProperties.lockExpiration
        this.save()

        return true
    }

    internal fun releaseLock() {
        destroyHeartbeat()


        delegate.lockWorkerId = null
        delegate.lockExpiration = null
        this.save()
    }


    internal fun initiateHeartbeat() {
        heartbeatScope.launch {
            try {
                while (isActive) {
                    delay(5_000)
                    val updated = repository.refreshLockIfPossible(
                        id, quasarEngine.workerId, Instant.now() + lockProperties.lockExpiration, Instant.now()
                    )
                    if (updated == 0) {
                        logger.warn("Heartbeat lost lock on token {}", id)
                        cancel()
                    }
                }
            } catch (ex: CancellationException) {
                // normal
            } catch (ex: Exception) {
                logger.debug("Heartbeat stopping for {} due to {}", id, ex.toString())
                cancel()
            }
        }
    }

    private fun destroyHeartbeat() {
        this.heartbeatScope.cancel("Heartbeat explicitly destroyed via 'destroyHeartbeat' method")
    }


    private suspend fun handleActivity() {
        currentActivity.handle(this, quasarAnnotationBeanProcessor)
        // check whether any task ended the process to cancel early preventing it from modifying any state later
        currentCoroutineContext().ensureActive()
        advanceActivity()
    }

    internal fun moveTo(activity: Activity) {
        delegate.currentActivityId = activity.id
        delegate.currentActivityName = if (activity is Named) activity.name else null
        currentActivity = activity
        save()
    }


    internal suspend fun advanceActivity() {
        val atomicCurrentActivity = currentActivity

        if (atomicCurrentActivity !is Outgoing) {
            end()
            return
        }

        if (atomicCurrentActivity.outgoingFlow.isEmpty()) {
            end()
            return
        }

        if (atomicCurrentActivity.outgoingFlow.size > 1) {
            throw IllegalStateException("Implicit forks are not supported to adhere to BPMN 2.0 best practices")
        }

        val nextFlow = atomicCurrentActivity.outgoingFlow.first()

        val nextActivity = bpmnProcess.sequenceFlows.find { it.id == nextFlow }?.let { f ->
            bpmnProcess.activities.find { it.id == f.target }
                ?: throw IllegalStateException("The activity ${f.target} was not found in process, but is referenced in a flow")
        }
            ?: throw IllegalStateException("The flow $nextFlow was not found in process, but is referenced in activity ${atomicCurrentActivity.id}")

        moveTo(nextActivity)
    }

    internal fun suspendExecution() {
        runtimeScope.cancel()
        heartbeatScope.cancel()
        releaseLock()
    }

    internal fun fork(outgoingFlows: List<String>) {

        val targets = outgoingFlows.map {
            resolveFlowTarget(it) ?: throw IllegalStateException("Flow $it target not found")
        }

        suspendExecution()

        delegate.isScope = true
        delegate.isActive = false

        saveWithoutChecks()


        val children = targets.map { activity ->
            val name = if (activity is Named) activity.name else null


            val childProcess = ProcessTokenModel(
                delegate.processDefinition,
                name,
                activity.id,
                delegate,
                delegate.variables
            )

            childProcess.isScope = false
            childProcess.isActive = true
            childProcess.isConcurrent = true

            return@map childProcess
        }

        repository.saveAll(children).map {
            createProcessTokenDomainModel(it)
        }.forEach {
            it.acquireLock()
            it.initiateHeartbeat()
            it.run()
            // TODO: Change to token scheduler later
        }
    }

    internal fun createProcessTokenDomainModel(model: ProcessTokenModel): ProcessToken = ProcessToken(
        model.id,
        model,
        repository,
        bpmnProcess,
        quasarEngine,
        lockProperties,
        quasarAnnotationBeanProcessor,
        transactionTemplate
    )


    private fun resolveFlowTarget(flowId: String): Activity? {
        return bpmnProcess.activities.firstOrNull { activity ->
            activity.id ==
                    bpmnProcess.sequenceFlows.firstOrNull { it.id == flowId }?.target
        }
    }

    override fun save() {
        if (isLocked()) {
            throw OptimisticLockException("Tried to modify database state of locked process token $id")
        }

        if (isFinished()) {
            throw ProcessFinishedException(id, "cannot mutate process state")
        }

        super.save()
    }


}