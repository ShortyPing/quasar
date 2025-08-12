package dev.vanadium.quasarplatform.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

abstract class DbBacked<DB_OBJECT : Any, REPOSITORY : JpaRepository<DB_OBJECT, UUID>> internal constructor(
    internal open var id: UUID,
    internal open var delegate: DB_OBJECT,
    internal open var repository: REPOSITORY
) {

    protected open fun save() {
        saveWithoutChecks()
    }

    protected fun saveWithoutChecks() {
        delegate = repository.save(delegate)
    }



}