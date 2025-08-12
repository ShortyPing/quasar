package dev.vanadium.quasarplatform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.dao.CannotAcquireLockException
import org.springframework.transaction.support.TransactionTemplate

private val logger = LoggerFactory.getLogger("TransactionUtils")

suspend fun <T> executeTxWithRetry(maxRetries: Int = 3, operation: suspend () -> T): T {



    var attempts = 0
    var lastException: Exception? = null

    while (attempts < maxRetries) {
        try {
            return operation()
        } catch (e: Exception) {
            lastException = e

            if (e is CannotAcquireLockException ||
                (e.cause?.message?.contains("could not serialize access") == true)) {

                attempts++
                logger.debug("Transaction serialization failure, retrying (attempt $attempts/$maxRetries)")

                // Exponential backoff
                delay((10L * (1L shl attempts)).coerceAtMost(1000L))
                continue
            }

            throw e
        }
    }

    throw lastException ?: RuntimeException("Transaction failed after $maxRetries retries")
}


suspend fun <T> TransactionTemplate.executeWithinCoroutine(action: suspend () -> T): T {
    // Capture the current coroutine context
    val currentContext = currentCoroutineContext()

    // Execute the transaction, ensuring the coroutine context is propagated
    return withContext(currentContext) {
        // Use the standard execute method but wrap the action to handle suspension
        execute { status ->
            // Create a new coroutine scope with the current context
            val result = CoroutineScope(currentContext).runCatching {
                // Run the suspending action
                kotlinx.coroutines.runBlocking(currentContext) { action() }
            }

            // Handle the result
            result.getOrElse { exception ->
                // Mark transaction for rollback
                status.setRollbackOnly()
                // Re-throw the exception
                throw exception
            }
        }!!
    }
}