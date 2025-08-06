package dev.vanadium.quasarplatform.persistence.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "process_definition_revision", schema = "public")
class ProcessDefinitionRevision(
    @Column(name = "name")
    val name: String?,
    @Column(name = "bpmn_id")
    val bpmnId: String,
    @Column(name = "revision_number")
    val revisionNumber: Int,
    @Column(name = "bpmn_xml")
    val bpmnXml: String,
    @Column(name = "bpmn_sha")
    val bpmnSha: String,
) {

    @Id
    @Column(name = "process_definition_revision_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()
}