package dev.vanadium.quasarplatform.utils

import java.security.MessageDigest


@OptIn(ExperimentalStdlibApi::class)
fun String.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(this.toByteArray()).toHexString()
}