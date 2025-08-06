package dev.vanadium.quasarplatform.runtime

import dev.vanadium.quasarplatform.bpmn.model.Activity
import dev.vanadium.quasarplatform.bpmn.model.BpmnEndEvent
import dev.vanadium.quasarplatform.bpmn.model.BpmnProcess
import dev.vanadium.quasarplatform.bpmn.model.BpmnServiceTask
import dev.vanadium.quasarplatform.bpmn.model.BpmnStartEvent
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class  SimpleProcessRuntime(val bpmnProcess: BpmnProcess) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val taskDefinitions: HashMap<String, () -> Boolean> = hashMapOf()
    private val tokens: ConcurrentHashMap<UUID, Token> = ConcurrentHashMap()

    val activeTokens = AtomicInteger(0)
    val doneLatch = CountDownLatch(1)

    fun startProcess() {
        taskDefinitions["sendMessage"] = {
            logger.info("This is the send message task firing up :D")
            true
        }
        taskDefinitions["deleteUser"] = {
            logger.info("Deleting the user")
            true
        }

        taskDefinitions["sleep"] = {
            logger.info("I am tired")
            Thread.sleep(5000)
            true
        }

        val start = bpmnProcess.startEvent

        createToken(start)

        doneLatch.await()

        logger.info("Process ${bpmnProcess.id} finished")
    }

    fun createToken(activity: Activity) {
        val id = UUID.randomUUID()
        tokens[id] = Token(activity, false)
        activeTokens.incrementAndGet()

        fun handleOutgoing(outgoing: Outgoing) {
            if(outgoing.outgoingFlow.isEmpty()) {
                throw RuntimeException("Outgoing activity has no outgoing flow")
            }

            if(outgoing.outgoingFlow.size == 1) {
                tokens[id]!!.currentActivity = getNextActivityFromFlow(outgoing.outgoingFlow.first())
                return
            }

            val flows = outgoing.outgoingFlow.toMutableList()

            tokens[id]!!.currentActivity = getNextActivityFromFlow(flows.removeFirst())
            flows.forEach {
                createToken(getNextActivityFromFlow(it))
            }
        }

        thread {
            while (!tokens[id]!!.finished) {
                when(val currentActivity = tokens[id]!!.currentActivity) {
                    is BpmnEndEvent -> {
                        tokens[id]!!.finished = true

                        val remaining = activeTokens.decrementAndGet()

                        logger.info("Token $id finished")

                        if(remaining == 0) {
                            doneLatch.countDown()
                        }
                        continue
                    }
                    is BpmnServiceTask -> {
                        val result = taskDefinitions[currentActivity.taskDefinition]?.invoke()
                            ?: throw RuntimeException("Task definition ${currentActivity.taskDefinition} missing")

                        if(!result) {
                            throw RuntimeException("Task definition ${currentActivity.taskDefinition} failed")
                        }
                        handleOutgoing(currentActivity)
                    }
                    is BpmnStartEvent -> {
                        handleOutgoing(currentActivity)
                    }
                }


            }
        }
    }

    fun getNextActivityFromFlow(flow: String): Activity {
        return bpmnProcess.sequenceFlows.find { it.id == flow }?.let { f ->
            bpmnProcess.serviceTasks.find { it.id == f.target }
                ?: bpmnProcess.endEvents.find { it.id == f.target }
                ?: throw RuntimeException("Target does not exist")
        } ?: throw RuntimeException("Flow does not exist")
    }

    data class Token(
        var currentActivity: Activity,
        var finished: Boolean
    )

}