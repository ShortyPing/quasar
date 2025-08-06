package dev.vanadium.quasarplatform.runtime

import dev.vanadium.quasarplatform.BpmnRegistry
import dev.vanadium.quasarplatform.api.QuasarEngine
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.persistence.model.ProcessTokenModel
import dev.vanadium.quasarplatform.persistence.repository.ProcessTokenModelRepository
import dev.vanadium.quasarplatform.properties.QuasarLockProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class QuasarEngineImpl(
    val bpmnRegistry: BpmnRegistry,
    private val quasarLockProperties: QuasarLockProperties,
    private val processTokenModelRepository: ProcessTokenModelRepository
) : QuasarEngine {



    private lateinit var _workerId: String
    private val logger = LoggerFactory.getLogger(javaClass)

    override val workerId: String
        get() = _workerId

    @PostConstruct
    fun init() {
        this._workerId = (quasarLockProperties.lockWorkerIdPrefix ?: "") + UUID.randomUUID().toString()

        logger.info("Initialized QuasarEngine (workerId=$workerId)")

        // TODO: Remove

        startProcess("Process_1uu8hhf")
    }

    override fun startProcess(processDefinitionId: String): ProcessToken {
        val process = bpmnRegistry.getProcess(processDefinitionId)
        val processDefinition = bpmnRegistry.getStoredProcessDefinition(processDefinitionId)

        val processToken = ProcessTokenModel(
            processDefinition,
            process.startEvent.name,
            process.startEvent.id,
            null,
            mutableMapOf()
        ).let { processTokenModelRepository.save(it) }


        return ProcessToken(
            processToken.id,
            processToken,
            processTokenModelRepository,
            this,
            quasarLockProperties,
            process
        ).also {
            it.acquireLock()
            it.initiateHeartbeat()
            it.run()
        }
    }




}