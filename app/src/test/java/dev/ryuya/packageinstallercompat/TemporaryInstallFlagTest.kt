package dev.ryuya.packageinstallercompat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemporaryInstallFlagTest {
    private class Args(@JvmField var flags: Int)
    private class Request(@JvmField val args: Args?)

    private val lowTargetMask = 1 shl 24
    private val requestDowngradeMask = 1 shl 7
    private val allowDowngradeMask = 1 shl 20
    private val scope = TemporaryInstallFlag(
        Request::class.java.getDeclaredField("args").apply { isAccessible = true },
        Args::class.java.getDeclaredField("flags").apply { isAccessible = true },
        lowTargetMask,
    )

    @Test
    fun restoresOnlyTheAddedBitEvenWhenPreparationThrows() {
        val args = Args(4)
        val request = Request(args)
        runCatching {
            requireNotNull(scope.addTo(request)).use {
                assertEquals(4 or lowTargetMask, args.flags)
                args.flags = args.flags or 32
                throw IllegalStateException("Synthetic install rejection")
            }
        }
        assertEquals(36, args.flags)
    }

    @Test
    fun leavesAnAlreadyAuthorizedFlagAndAbsentInstallArgumentsAlone() {
        val args = Args(lowTargetMask or 4)
        assertNull(scope.addTo(Request(args)))
        assertEquals(lowTargetMask or 4, args.flags)
        assertNull(scope.addTo(Request(null)))
    }

    @Test
    fun preservesExistingRequestFlagsWithoutAddingDowngradePermissions() {
        val args = Args(requestDowngradeMask or 4)
        requireNotNull(scope.addTo(Request(args))).use {
            assertEquals(requestDowngradeMask or lowTargetMask or 4, args.flags)
        }
        assertEquals(requestDowngradeMask or 4, args.flags)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMultipleFlags() {
        TemporaryInstallFlag(
            Request::class.java.getDeclaredField("args"),
            Args::class.java.getDeclaredField("flags"),
            lowTargetMask or allowDowngradeMask,
        )
    }
}
