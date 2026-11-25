package dev.ryuya.packageinstallercompat

import java.lang.reflect.Field

internal class TemporaryInstallFlag(
    private val argsField: Field,
    private val flagsField: Field,
    private val mask: Int,
) {
    init {
        require(Integer.bitCount(mask) == 1)
    }

    /** The install request owns its args. Leave unrelated framework flags intact. */
    fun addTo(request: Any): AutoCloseable? {
        val args = argsField.get(request) ?: return null
        val flags = flagsField.getInt(args)
        if (flags and mask != 0) return null
        flagsField.setInt(args, flags or mask)
        return AutoCloseable {
            flagsField.setInt(args, flagsField.getInt(args) and mask.inv())
        }
    }
}
