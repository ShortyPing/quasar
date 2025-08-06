package dev.vanadium.quasarplatform

import dev.vanadium.quasarplatform.bpmn.model.BpmnProcess
import dev.vanadium.quasarplatform.bpmn.parser.BpmnParser
import dev.vanadium.quasarplatform.exception.ProcessDefinitionNotFoundException
import dev.vanadium.quasarplatform.persistence.model.ProcessDefinitionRevision
import dev.vanadium.quasarplatform.persistence.repository.ProcessDefinitionRevisionRepository
import dev.vanadium.quasarplatform.utils.sha256
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils

@Component
class BpmnRegistry(
    private val processDefinitionRevisionRepository: ProcessDefinitionRevisionRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val bpmnRegistry = mutableMapOf<String, BpmnProcess>()

    @PostConstruct
    fun bootstrap() {
        logger.info("Bootstrapping Quasar engine")

        this.loadBpmns()
    }

    private fun loadBpmns() {

        val parser = BpmnParser()

        val bpmnDirectory = ResourceUtils.getFile("classpath:bpmn")

        if (!bpmnDirectory.exists()) {
            throw RuntimeException("Directory 'bpmn' does not exist in resources")
        }

        bpmnDirectory.listFiles()?.forEach { file ->
            logger.info("Loading BPMN file ${file.name}")

            val xml = file.readText()
            val hash = xml.sha256()

            val parsedProcess = parser.parseBpmn(xml)

            val storedRevisions = processDefinitionRevisionRepository.findByBpmnId(parsedProcess.id)
            var currentRevision = 0

            if (!storedRevisions.isEmpty()) {
                val latestRevision = storedRevisions.maxBy { it.revisionNumber }

                if (latestRevision.bpmnSha == hash) {
                    logger.info("Process ${parsedProcess.id} is up to date with revision ${latestRevision.revisionNumber}")
                    bpmnRegistry[parsedProcess.id] = parsedProcess
                    return@forEach
                }
                logger.info("Found revision ${latestRevision.revisionNumber} for ${parsedProcess.id} with different hash, creating new revision")
                currentRevision = latestRevision.revisionNumber
            }


            val revision =
                ProcessDefinitionRevision(
                    parsedProcess.name,
                    parsedProcess.id,
                    currentRevision + 1,
                    xml,
                    hash
                )

            processDefinitionRevisionRepository.save(revision)
            logger.info("Saved revision ${revision.revisionNumber} for '${revision.bpmnId}'")
            bpmnRegistry[parsedProcess.id] = parsedProcess
        }
    }


    fun getProcess(id: String): BpmnProcess {
        return bpmnRegistry[id] ?: throw ProcessDefinitionNotFoundException(id)
    }

    fun getStoredProcessDefinition(id: String): ProcessDefinitionRevision {
        return processDefinitionRevisionRepository.findByBpmnId(id).maxBy { it.revisionNumber }
    }
}