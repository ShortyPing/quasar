package dev.vanadium.quasarplatform.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DbBacked<DB_OBJECT : Any, REPOSITORY : JpaRepository<DB_OBJECT, UUID>> {

    var id: UUID
    var representation: DB_OBJECT
    var repository: REPOSITORY

    fun save() {
        representation = repository.save(representation)
    }



}