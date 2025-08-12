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

    @ServiceTask("sendMessage")
    suspend fun test(processToken: ProcessToken) {
        logger.info("Yay send message")
        delay(1000)
        processToken.setVariable("userId", UUID.randomUUID().toString())
    }

    @ServiceTask("deleteUser")
    fun delete(processToken: ProcessToken) {
        logger.info("Yay delete user ${processToken.getVariable("userId")}")
    }
}


@RestController
class TestController(private val quasarEngine: QuasarEngine) {

    @GetMapping("/test")
    fun testRun() {
        quasarEngine.startProcess("Process_1uu8hhf")
    }
}