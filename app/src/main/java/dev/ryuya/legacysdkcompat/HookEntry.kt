package dev.ryuya.legacysdkcompat

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.HotReloadedParam
import io.github.libxposed.api.XposedModuleInterface.HotReloadingParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Modifier

class HookEntry : XposedModule() {
    private var hostLoader: ClassLoader? = null
    @Volatile private var preferences: SharedPreferences? = null

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        register(param.classLoader)
    }

    override fun onHotReloading(param: HotReloadingParam): Boolean {
        val loader = hostLoader ?: return false
        param.setSavedInstanceState(loader)
        return true
    }

    override fun onHotReloaded(param: HotReloadedParam) {
        if (param.processName != "system_server") return
        val loader = param.savedInstanceState as? ClassLoader ?: return
        if (register(loader)) {
            param.oldHookHandles.forEach { it.unhook() }
            Log.i(TAG, "ONEUI_MODS_HOT_RELOAD package=${BuildConfig.APPLICATION_ID} version=${BuildConfig.VERSION_CODE} pid=${Process.myPid()}")
        }
    }

    private data class Options(
        val allowLowTargetSdk: Boolean,
        val allowSameSignatureDowngrade: Boolean,
    )

    private fun options(): Options = runCatching {
        val settings = preferences ?: getRemotePreferences(Settings.GROUP).also { preferences = it }
        Options(
            allowLowTargetSdk = settings.getBoolean(Settings.ALLOW_LOW_TARGET_SDK, false),
            allowSameSignatureDowngrade = settings.getBoolean(
                Settings.ALLOW_SAME_SIGNATURE_DOWNGRADE,
                false,
            ),
        )
    }.getOrDefault(Options(false, false))

    @SuppressLint("BlockedPrivateApi", "SoonBlockedPrivateApi")
    private fun register(loader: ClassLoader): Boolean = runCatching {
        val request = loader.loadClass("com.android.server.pm.InstallRequest")
        val helper = loader.loadClass("com.android.server.pm.InstallPackageHelper")
        val prepare = listOf("preparePackage", "preparePackageLI").firstNotNullOfOrNull { name ->
            runCatching { helper.getDeclaredMethod(name, request) }.getOrNull()
        } ?: error("Unrecognized package preparation method")
        check(prepare.returnType == Void.TYPE)
        val args = request.getDeclaredField("mInstallArgs").apply { isAccessible = true }
        check(args.type.name == "com.android.server.pm.InstallArgs")
        val flags = args.type.getDeclaredField("mInstallFlags").apply { isAccessible = true }
        check(flags.type == Int::class.javaPrimitiveType && !Modifier.isFinal(flags.modifiers))
        val lowTargetMask = PackageManager::class.java
            .getDeclaredField("INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK")
            .apply { isAccessible = true }.getInt(null)
        val requestDowngradeMask = PackageManager::class.java
            .getDeclaredField("INSTALL_REQUEST_DOWNGRADE")
            .apply { isAccessible = true }.getInt(null)
        val allowDowngradeMask = PackageManager::class.java
            .getDeclaredField("INSTALL_ALLOW_DOWNGRADE")
            .apply { isAccessible = true }.getInt(null)
        val supportedMask = lowTargetMask or requestDowngradeMask or allowDowngradeMask
        check(Integer.bitCount(lowTargetMask) == 1)
        check(Integer.bitCount(requestDowngradeMask) == 1)
        check(Integer.bitCount(allowDowngradeMask) == 1)
        check(Integer.bitCount(supportedMask) == 3)
        val scopedFlags = TemporaryInstallFlags(args, flags, supportedMask)

        hook(prepare).setId("legacy-sdk-package-prepare")
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val selected = options()
                val requestedMask =
                    (if (selected.allowLowTargetSdk) lowTargetMask else 0) or
                    (if (selected.allowSameSignatureDowngrade) {
                        requestDowngradeMask or allowDowngradeMask
                    } else 0)
                val change = runCatching {
                    scopedFlags.addTo(chain.getArg(0), requestedMask)
                }.getOrNull()
                try {
                    chain.proceed()
                } finally {
                    if (change != null) runCatching { change.close() }.onFailure {
                        Log.w(TAG, "Could not restore the scoped install flag", it)
                    }
                }
            }
        hostLoader = loader
        Log.i(TAG, "INSTALL_COMPAT_HOOK_READY ${prepare.name}")
        true
    }.getOrElse {
        Log.w(TAG, "Package installer layout unsupported", it)
        false
    }

    private companion object {
        const val TAG = "LegacySdkCompat"
    }
}
