package dev.ryuya.legacysdkcompat

import android.app.Application
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal data class Connection(
    val framework: String? = null,
    val allowLowTargetSdk: Boolean = false,
    val allowSameSignatureDowngrade: Boolean = false,
    val savingKey: String? = null,
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
                                allowSameSignatureDowngrade = settings.getBoolean(
                                    Settings.ALLOW_SAME_SIGNATURE_DOWNGRADE,
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

    internal fun setEnabled(key: String, enabled: Boolean) {
        require(
            key == Settings.ALLOW_LOW_TARGET_SDK ||
                key == Settings.ALLOW_SAME_SIGNATURE_DOWNGRADE,
        )
        val settings = preferences ?: return
        val previous = connection.value ?: return
        if (previous.savingKey != null) return
        connection.value = previous.copy(savingKey = key, saveFailed = false)
        worker.execute {
            val saved = runCatching {
                settings.edit().putBoolean(key, enabled).commit()
            }.getOrDefault(false)
            main.post {
                if (preferences !== settings) return@post
                connection.value = previous.copy(
                    allowLowTargetSdk = if (saved && key == Settings.ALLOW_LOW_TARGET_SDK) {
                        enabled
                    } else previous.allowLowTargetSdk,
                    allowSameSignatureDowngrade = if (
                        saved && key == Settings.ALLOW_SAME_SIGNATURE_DOWNGRADE
                    ) {
                        enabled
                    } else previous.allowSameSignatureDowngrade,
                    savingKey = null,
                    saveFailed = !saved,
                )
            }
        }
    }
}
