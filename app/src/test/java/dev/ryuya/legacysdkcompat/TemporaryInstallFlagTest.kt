package dev.ryuya.legacysdkcompat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemporaryInstallFlagTest {
    private class Args(@JvmField var flags: Int)
    private class Request(@JvmField val args: Args?)

    private val mask = 1 shl 24
    private val scope = TemporaryInstallFlag(
        Request::class.java.getDeclaredField("args").apply { isAccessible = true },
        Args::class.java.getDeclaredField("flags").apply { isAccessible = true },
        mask,
    )

    @Test
    fun restoresOnlyTheAddedBitEvenWhenPreparationThrows() {
        val args = Args(4)
        val request = Request(args)
        runCatching {
            requireNotNull(scope.addTo(request)).use {
                assertEquals(4 or mask, args.flags)
                args.flags = args.flags or 32
                throw IllegalStateException("Synthetic install rejection")
            }
        }
        assertEquals(36, args.flags)
    }

    @Test
    fun leavesAnAlreadyAuthorizedFlagAndAbsentInstallArgumentsAlone() {
        val args = Args(mask or 4)
        assertNull(scope.addTo(Request(args)))
        assertEquals(mask or 4, args.flags)
        assertNull(scope.addTo(Request(null)))
    }
}
