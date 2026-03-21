package dev.mokksy.mokksy.serializers

import kotlinx.serialization.SerializationException

internal object SerializerUtils {
    internal fun <T : Any> T?.shouldNotBeNull(message: String): T {
        if (this == null) {
            throw SerializationException(message)
        }
        return requireNotNull(this)
    }
}
