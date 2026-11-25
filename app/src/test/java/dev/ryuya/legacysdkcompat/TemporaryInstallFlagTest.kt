package dev.ryuya.legacysdkcompat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemporaryInstallFlagTest {
    private class Args(@JvmField var flags: Int)
    private class Request(@JvmField val args: Args?)

    private val lowTargetMask = 1 shl 24
    private val requestDowngradeMask = 1 shl 7
    private val allowDowngradeMask = 1 shl 20
    private val supportedMask = lowTargetMask or requestDowngradeMask or allowDowngradeMask
    private val scope = TemporaryInstallFlags(
        Request::class.java.getDeclaredField("args").apply { isAccessible = true },
        Args::class.java.getDeclaredField("flags").apply { isAccessible = true },
        supportedMask,
    )

    @Test
    fun restoresOnlyTheAddedBitEvenWhenPreparationThrows() {
        val args = Args(4)
        val request = Request(args)
        runCatching {
            requireNotNull(scope.addTo(request, lowTargetMask)).use {
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
        assertNull(scope.addTo(Request(args), lowTargetMask))
        assertEquals(lowTargetMask or 4, args.flags)
        assertNull(scope.addTo(Request(null), lowTargetMask))
        assertNull(scope.addTo(Request(args), 0))
    }

    @Test
    fun addsBothDowngradeBitsAndRestoresOnlyTheMissingOne() {
        val args = Args(requestDowngradeMask or 4)
        requireNotNull(
            scope.addTo(Request(args), requestDowngradeMask or allowDowngradeMask),
        ).use {
            assertEquals(requestDowngradeMask or allowDowngradeMask or 4, args.flags)
            args.flags = args.flags or 32
        }
        assertEquals(requestDowngradeMask or 4 or 32, args.flags)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSignatureVerificationBypassFlag() {
        val installDisableVerification = 0x00080000
        scope.addTo(Request(Args(0)), installDisableVerification)
    }
}
