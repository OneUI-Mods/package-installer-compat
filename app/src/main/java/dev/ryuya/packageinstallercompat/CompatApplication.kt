package dev.ryuya.packageinstallercompat

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.android.material.color.DynamicColors
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal data class Connection(
    val framework: String? = null,
    val allowLowTargetSdk: Boolean = false,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
)

class CompatApplication : Application() {
    internal val connection = MutableLiveData(Connection())
    private val main = Handler(Looper.getMainLooper())
    private val worker = ThreadPoolExecutor(1, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue())
        .apply { allowCoreThreadTimeOut(true) }
    private var service: XposedService? = null
    private var preferences: SharedPreferences? = null

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(bound: XposedService) {
                main.post {
                    service = bound
                    preferences = null
                    connection.value = Connection()
                    worker.execute {
                        val state = runCatching {
                            val settings = bound.getRemotePreferences(Settings.GROUP)
                            settings to Connection(
                                framework = bound.frameworkName,
                                allowLowTargetSdk = settings.getBoolean(
                                    Settings.ALLOW_LOW_TARGET_SDK,
                                    false,
                                ),
                            )
                        }.getOrNull() ?: return@execute
                        main.post {
                            if (service === bound) {
                                preferences = state.first
                                connection.value = state.second
                            }
                        }
                    }
                }
            }

            override fun onServiceDied(dead: XposedService) {
                main.post {
                    if (service === dead) {
                        service = null
                        preferences = null
                        connection.value = Connection()
                    }
                }
            }
        })
    }

    internal fun setLowTargetSdkAllowed(enabled: Boolean) {
        val settings = preferences ?: return
        val previous = connection.value ?: return
        if (previous.saving) return
        connection.value = previous.copy(saving = true, saveFailed = false)
        worker.execute {
            val saved = runCatching {
                settings.edit().putBoolean(Settings.ALLOW_LOW_TARGET_SDK, enabled).commit()
            }.getOrDefault(false)
            main.post {
                if (preferences !== settings) return@post
                connection.value = previous.copy(
                    allowLowTargetSdk = if (saved) enabled else previous.allowLowTargetSdk,
                    saving = false,
                    saveFailed = !saved,
                )
            }
        }
    }
}
