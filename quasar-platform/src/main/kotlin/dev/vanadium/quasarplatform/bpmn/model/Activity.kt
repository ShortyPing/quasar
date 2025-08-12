package dev.vanadium.quasarplatform.bpmn.model

import dev.vanadium.quasarplatform.bpmn.model.properties.Identifiable
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import dev.vanadium.quasarplatform.runtime.processor.QuasarAnnotationBeanProcessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed class Activity : Identifiable {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    internal abstract suspend fun handle(processToken: ProcessToken,  quasarAnnotationBeanProcessor: QuasarAnnotationBeanProcessor)
}