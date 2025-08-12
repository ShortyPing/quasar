package dev.vanadium.quasarplatform.persistence.model

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.OptimisticLock
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "process_token", schema = "public")
class ProcessTokenModel(
    @JoinColumn(name = "process_definition")
    @ManyToOne
    val processDefinition: ProcessDefinitionRevision,
    @Column(name = "current_activity_name")
    var currentActivityName: String?,
    @Column(name = "current_activity")
    var currentActivityId: String,
    @JoinColumn(name = "parent_process_token")
    @ManyToOne
    var parentProcessToken: ProcessTokenModel?,
    @Column(name = "variables")
    @Type(JsonType::class)
    var variables: MutableMap<String, String> = mutableMapOf(),
) {

    @Id
    @Column(name = "process_token_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column(name = "lock_worker_id")
    @OptimisticLock(excluded = true)
    var lockWorkerId: String? = null

    @Column(name = "lock_expiration")
    @OptimisticLock(excluded = true)
    var lockExpiration: Instant? = null

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    var status: TokenStatus = TokenStatus.RUNNING

    @Column(name = "created_at")
    val createdAt: Instant = Instant.now()

    @Column(name = "is_scope")
    var isScope: Boolean = false

    @Column(name = "is_concurrent")
    var isConcurrent: Boolean = false

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Version
    @Column(name = "version")
    var version: Long = 0L

}