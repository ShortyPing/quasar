package dev.vanadium.quasarplatform.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "quasar-platform.lock")
class QuasarLockProperties {

    var lockWorkerIdPrefix: String? = null
    var lockExpiration = Duration.ofMinutes(5)
}