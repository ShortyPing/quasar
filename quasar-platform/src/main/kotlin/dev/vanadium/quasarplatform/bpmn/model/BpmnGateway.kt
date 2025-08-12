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

            parentToken.isActive = true
            parentToken.currentActivityId = id
            processToken.repository.save(parentToken)

            logger.debug(
                "Siblings {}",
                siblingTokens.map { it.id to it.delegate.isActive to it.delegate.status })

            siblingTokens.filter { it.delegate.isActive }.forEach {
                it.suspendExecution()
                processToken.repository.atomicFinish(it.id, TokenStatus.FINISHED)
            }

            logger.debug(
                "Siblings (after atomicFinish()) {}",
                siblingTokens.map { it.id to it.delegate.isActive to it.delegate.status })


            val parentProcessToken = processToken.createProcessTokenDomainModel(parentToken)



            parentProcessToken.runWithStart(this)

        } else {
            processToken.suspendExecution()
        }


    }
}


enum class GatewayType {
    PARALLEL
}