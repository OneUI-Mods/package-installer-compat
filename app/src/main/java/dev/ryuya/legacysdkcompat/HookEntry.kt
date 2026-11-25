package dev.ryuya.legacysdkcompat

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

    private fun enabled(): Boolean = runCatching {
        val settings = preferences ?: getRemotePreferences(Settings.GROUP).also { preferences = it }
        settings.getBoolean(Settings.ALLOW_LOW_TARGET_SDK, false)
    }.getOrDefault(false)

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
        val mask = PackageManager::class.java.getDeclaredField("INSTALL_BYPASS_LOW_TARGET_SDK_BLOCK")
            .apply { isAccessible = true }.getInt(null)
        check(Integer.bitCount(mask) == 1)
        val exception = TemporaryInstallFlag(args, flags, mask)

        hook(prepare).setId("legacy-sdk-package-prepare")
            .setExceptionMode(ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                val change = if (enabled()) {
                    runCatching { exception.addTo(chain.getArg(0)) }.getOrNull()
                } else null
                try {
                    chain.proceed()
                } finally {
                    if (change != null) runCatching { change.close() }.onFailure {
                        Log.w(TAG, "Could not restore the scoped install flag", it)
                    }
                }
            }
        hostLoader = loader
        Log.i(TAG, "LOW_TARGET_HOOK_READY ${prepare.name}")
        true
    }.getOrElse {
        Log.w(TAG, "Package installer layout unsupported", it)
        false
    }

    private companion object {
        const val TAG = "LegacySdkCompat"
    }
}
