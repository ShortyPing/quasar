package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.bpmn.model.properties.Incoming
import dev.vanadium.quasarplatform.bpmn.model.properties.Named
import dev.vanadium.quasarplatform.bpmn.model.properties.Outgoing
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.persistence.model.TokenStatus
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor
import kotlin.jvm.optionals.getOrNull

class BpmnGateway(
    override val id: String,
    override val name: String?,
    override val incomingFlow: List<String>,
    override val outgoingFlow: List<String>,
    val gatewayType: GatewayType,
) : Identifiable, Named, Incoming, Outgoing, Activity() {
    override suspend fun handle(
        processToken: ProcessToken,
        quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor
    ) {

        when (gatewayType) {
            GatewayType.PARALLEL -> {
                if (incomingFlow.size > 1) {
                    processToken.transactionTemplate.execute {

                        handleJoin(processToken)
                    }
                } else {
                    processToken.fork(outgoingFlow)
                }
            }
        }
    }


    private fun handleJoin(
        processToken: ProcessToken
    ) {

        val parentToken = processToken.repository.findById(
            processToken.delegate.parentProcessToken?.id ?: return
        ).getOrNull()
            ?: return

        if (parentToken.isActive) {
            processToken.end()
            return
        }
        val siblingTokens = processToken.repository.findByParentProcessToken(parentToken)
            .filter { it.isConcurrent }
            .map { processToken.createProcessTokenDomainModel(it) }

        val tokensAtGateway = siblingTokens.count { it.delegate.currentActivityId == id }

        if (tokensAtGateway == incomingFlow.size) {

            val activated = processToken.repository.activateParentAtGateway(
                parentToken.id,
                id,
                name
            )
            logger.debug("Activated parent {} to gateway({};{})", parentToken.id, id, name)

            if (activated == 1) {
                // we won; best-effort cancel children first
                val finished = processToken.repository.finishConcurrentChildren(parentToken.id)
                logger.debug("Finished {} concurrent children for parent {}", finished, parentToken.id)

                // Now resume the parent safely
                val freshParent = processToken.repository.findById(parentToken.id).getOrNull() ?: return

                logger.debug("Fresh parent loaded from database {} Act({};{})", freshParent.id, freshParent.currentActivityId, freshParent.currentActivityName)
                val parentDomain = processToken.createProcessTokenDomainModel(freshParent)
                // Acquire lock before running parent
                parentDomain.acquireLock()
                parentDomain.initiateHeartbeat()
                parentDomain.run(advance = true)
            } else {
                // someone else activated the parent; just suspend
                processToken.suspendExecution()
            }

        } else {
            processToken.suspendExecution()
        }


    }
}


enum class GatewayType {
    PARALLEL
}