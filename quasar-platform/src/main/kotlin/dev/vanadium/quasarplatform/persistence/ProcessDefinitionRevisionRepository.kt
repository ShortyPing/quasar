package dev.vanadium.quasarplatform.persistence

import dev.vanadium.quasarplatform.model.ProcessDefinitionRevision
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface ProcessDefinitionRevisionRepository : JpaRepository<ProcessDefinitionRevision, UUID> {
    fun findByBpmnId(bpmnId: String): List<ProcessDefinitionRevision>
}