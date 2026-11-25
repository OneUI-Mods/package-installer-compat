package dev.ryuya.packageinstallercompat

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CompatApplication
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(body) }
        ViewCompat.setOnApplyWindowInsetsListener(body) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left + dp(24), bars.top + dp(24), bars.right + dp(24), bars.bottom + dp(24))
            insets
        }
        body.addView(TextView(this).apply {
            setText(R.string.app_name)
            textSize = 26f
        })
        body.addView(TextView(this).apply {
            setText(R.string.description)
            setPadding(0, dp(12), 0, dp(24))
        })
        val lowTargetToggle = MaterialSwitch(this).apply {
            setText(R.string.allow_old_sdk)
            minHeight = dp(52)
        }
        body.addView(lowTargetToggle)
        body.addView(TextView(this).apply {
            setText(R.string.low_target_detail)
            setPadding(0, dp(8), 0, dp(20))
        })
        val downgradeToggle = MaterialSwitch(this).apply {
            setText(R.string.allow_same_signature_downgrade)
            minHeight = dp(52)
        }
        body.addView(downgradeToggle)
        body.addView(TextView(this).apply {
            setText(R.string.downgrade_detail)
            setPadding(0, dp(8), 0, dp(20))
        })
        val status = TextView(this)
        body.addView(status)
        setContentView(scroll)

        app.connection.observe(this) { state ->
            lowTargetToggle.setOnCheckedChangeListener(null)
            lowTargetToggle.isEnabled = state.framework != null && state.savingKey == null
            lowTargetToggle.isChecked = state.allowLowTargetSdk
            lowTargetToggle.setOnCheckedChangeListener { _, enabled ->
                app.setEnabled(Settings.ALLOW_LOW_TARGET_SDK, enabled)
            }
            downgradeToggle.setOnCheckedChangeListener(null)
            downgradeToggle.isEnabled = state.framework != null && state.savingKey == null
            downgradeToggle.isChecked = state.allowSameSignatureDowngrade
            downgradeToggle.setOnCheckedChangeListener { _, enabled ->
                app.setEnabled(Settings.ALLOW_SAME_SIGNATURE_DOWNGRADE, enabled)
            }
            status.text = when {
                state.saveFailed -> getString(R.string.setting_error)
                state.framework != null -> getString(R.string.framework_connected, state.framework)
                else -> getString(R.string.connection_required)
            }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
