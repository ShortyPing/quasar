package dev.vanadium.quasarplatform.runtime.processor

import dev.vanadium.quasarplatform.api.annotation.Process
import dev.vanadium.quasarplatform.api.annotation.ServiceTask
import dev.vanadium.quasarplatform.exception.ActivityCallFailedException
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Service
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.javaType

@Service
class QuasarAnnotationBeanProcessor : ApplicationContextAware {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val registeredServiceTasks = hashMapOf<String, HashMap<String, ProcessedServiceTaskMethod>>()
    private lateinit var applicationContext: ApplicationContext


    @PostConstruct
    fun init() {
        scanProcesses()
    }



    suspend fun callServiceTask(processId: String, taskDefinition: String, processToken: ProcessToken) {
        val method = this.registeredServiceTasks[processId]?.get(taskDefinition) ?: throw ActivityCallFailedException(processId, taskDefinition, "No service task delegated method found!")
        method.call(processToken)

    }

    private fun scanProcesses() {
        applicationContext.getBeansWithAnnotation<Process>().forEach { (_, bean) ->
            val clazz = bean::class
            val processAnnotation = clazz.findAnnotation<Process>()
                ?: throw IllegalStateException("Annotation Process cannot be found in class ${clazz.qualifiedName} even though earlier check succeeded, this should not happen")

            registeredServiceTasks.putIfAbsent(processAnnotation.id, hashMapOf())

            registerServiceTasks(bean, clazz, processAnnotation)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun registerServiceTasks(
        bean: Any,
        clazz: KClass<out Any>,
        processAnnotation: Process
    ) {
        clazz.memberFunctions.filter { it.hasAnnotation<ServiceTask>() }.forEach {
            val serviceTaskAnnotation = it.findAnnotation<ServiceTask>()
                ?: throw IllegalStateException("Annotation ServiceTask cannot be found in class ${clazz.qualifiedName} even though earlier check succeeded, this should not happen")

            val processTokenParameter =
                it.parameters.firstOrNull { parameter -> parameter.type.javaType == ProcessToken::class.java }
                    ?: throw NoSuchMethodException("The method ${it.name} on bean ${clazz.qualifiedName} does not have a parameter of type ProcessToken")

            registeredServiceTasks[processAnnotation.id]!![serviceTaskAnnotation.value] =
                ProcessedServiceTaskMethod(bean, it, processTokenParameter)

            logger.info("Registered service task ${serviceTaskAnnotation.value} in process ${processAnnotation.id}")
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}