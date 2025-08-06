package dev.vanadium.quasarplatform.persistence.repository

import dev.vanadium.quasarplatform.persistence.model.ProcessTokenModel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProcessTokenModelRepository : JpaRepository<ProcessTokenModel, UUID> {
}