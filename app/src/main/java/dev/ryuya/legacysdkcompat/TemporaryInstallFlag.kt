package dev.ryuya.legacysdkcompat

import java.lang.reflect.Field

internal class TemporaryInstallFlags(
    private val argsField: Field,
    private val flagsField: Field,
    private val supportedMask: Int,
) {
    /** The install request owns its args. Leave unrelated framework flags intact. */
    fun addTo(request: Any, requestedMask: Int): AutoCloseable? {
        require(requestedMask and supportedMask.inv() == 0) { "Unsupported install flag requested" }
        if (requestedMask == 0) return null
        val args = argsField.get(request) ?: return null
        val flags = flagsField.getInt(args)
        val addedMask = requestedMask and flags.inv()
        if (addedMask == 0) return null
        flagsField.setInt(args, flags or addedMask)
        return AutoCloseable {
            flagsField.setInt(args, flagsField.getInt(args) and addedMask.inv())
        }
    }
}
