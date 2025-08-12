package dev.vanadium.quasarplatform.persistence.repository

import dev.vanadium.quasarplatform.persistence.model.ProcessTokenModel
import dev.vanadium.quasarplatform.persistence.model.TokenStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

interface ProcessTokenModelRepository : JpaRepository<ProcessTokenModel, UUID> {


    @Query("""select count(*) >= 1 from ProcessTokenModel t where t.parentProcessToken = :processToken and t.status = 'RUNNING' and t.isActive = true""")
    fun hasRunningChildProcesses(processToken: ProcessTokenModel): Boolean

    @Modifying
    @Query(
        """
    UPDATE ProcessTokenModel p 
    SET p.status = :status, 
        p.isActive = false,
        p.lockWorkerId = NULL,
        p.lockExpiration = NULL
    WHERE p.id = :id 
    AND p.isActive = true 
    AND p.status <> :status
"""
    )
    @Transactional
    fun atomicFinish(@Param("id") id: UUID, @Param("status") status: TokenStatus): Int

    @Modifying
    @Query("""
update ProcessTokenModel t
set t.lockWorkerId = :workerId, t.lockExpiration = :newExpiration
where t.id = :id
  and (
    t.lockWorkerId is null
    or t.lockWorkerId = :workerId
    or t.lockExpiration < :now
  )
  and t.status <> dev.vanadium.quasarplatform.persistence.model.TokenStatus.FINISHED
""")
    fun refreshLockIfPossible(
        @Param("id") id: UUID,
        @Param("workerId") workerId: String,
        @Param("newExpiration") newExpiration: Instant,
        @Param("now") now: Instant
    ): Int


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
update ProcessTokenModel t
set t.isActive = true, t.currentActivityId = :gatewayId, t.currentActivityName = :gatewayName
where t.id = :parentId and t.isActive = false
""")
    fun activateParentAtGateway(
        @Param("parentId") parentId: UUID,
        @Param("gatewayId") gatewayId: String,
        @Param("gatewayName") gatewayName: String?
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
update ProcessTokenModel t
set t.status = dev.vanadium.quasarplatform.persistence.model.TokenStatus.FINISHED,
    t.isActive = false
where t.parentProcessToken.id = :parentId
  and t.isConcurrent = true
  and t.status = dev.vanadium.quasarplatform.persistence.model.TokenStatus.RUNNING
""")
    fun finishConcurrentChildren(@Param("parentId") parentId: UUID): Int


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProcessTokenModel p where p.id = :id")
    fun findByIdWithPessimisticLocking(id: UUID): ProcessTokenModel?

    fun findByParentProcessToken(processToken: ProcessTokenModel): List<ProcessTokenModel>
}