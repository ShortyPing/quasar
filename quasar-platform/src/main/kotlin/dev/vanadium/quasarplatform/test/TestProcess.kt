package dev.vanadium.quasarplatform.test

import dev.vanadium.quasarplatform.api.QuasarEngine
import dev.vanadium.quasarplatform.api.annotation.Process
import dev.vanadium.quasarplatform.api.annotation.ServiceTask
import dev.vanadium.quasarplatform.persistence.impl.ProcessToken
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@Process(id = "Process_1uu8hhf")
@Service
class TestProcess {

    private val logger = LoggerFactory.getLogger(javaClass)


    @ServiceTask("createUser")
    suspend fun createUser(processToken: ProcessToken) {
        logger.info("Creating user")
    }

    @ServiceTask("sendConfirmationEmail")
    suspend fun sendConfirmationEmail(processToken: ProcessToken) {
        delay(30000)
        logger.info("Sending confirmation email")
    }

    @ServiceTask("sendNewPasswordEmail")
    fun sendNewPasswordEmail(processToken: ProcessToken) {
        logger.info("Sending new password email")
    }

    @ServiceTask("activateUser")
    suspend fun activateUser(processToken: ProcessToken) {
        logger.info("Activating user")
    }

    @ServiceTask("sendPromo")
    suspend fun sendPromo(processToken: ProcessToken) {
        logger.info("Sending promo")
    }

    @ServiceTask("saveUser")
    suspend fun saveUser(processToken: ProcessToken) {
        delay(2000)
        logger.info("Saving user")
    }
}


@RestController
class TestController(private val quasarEngine: QuasarEngine) {

    @GetMapping("/test")
    fun testRun() {
        quasarEngine.startProcess("Process_1uu8hhf")
    }
}