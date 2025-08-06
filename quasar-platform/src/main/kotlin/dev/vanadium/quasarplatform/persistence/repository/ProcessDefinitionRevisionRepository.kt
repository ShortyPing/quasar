package dev.vanadium.quasarplatform.persistence.repository

import dev.vanadium.quasarplatform.persistence.model.ProcessDefinitionRevision
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProcessDefinitionRevisionRepository : JpaRepository<ProcessDefinitionRevision, UUID> {
    fun findByBpmnId(bpmnId: String): List<ProcessDefinitionRevision>
}