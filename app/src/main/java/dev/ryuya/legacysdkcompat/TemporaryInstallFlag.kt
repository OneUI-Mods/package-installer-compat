package dev.ryuya.legacysdkcompat

import java.lang.reflect.Field

internal class TemporaryInstallFlag(
    private val argsField: Field,
    private val flagsField: Field,
    private val mask: Int,
) {
    /** The install request owns its args. Leave any other flags changed by the framework intact. */
    fun addTo(request: Any): AutoCloseable? {
        val args = argsField.get(request) ?: return null
        val flags = flagsField.getInt(args)
        if (flags and mask != 0) return null
        flagsField.setInt(args, flags or mask)
        return AutoCloseable { flagsField.setInt(args, flagsField.getInt(args) and mask.inv()) }
    }
}
