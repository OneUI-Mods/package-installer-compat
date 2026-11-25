package dev.ryuya.packageinstallercompat

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.R as MaterialR
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val lightBackground = nightMode != Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightBackground
            isAppearanceLightNavigationBars = lightBackground
        }

        val app = application as CompatApplication
        val scroll = findViewById<View>(R.id.settings_scroll)
        val lowTargetToggle = findViewById<MaterialSwitch>(R.id.low_target_toggle)
        val statusCard = findViewById<MaterialCardView>(R.id.status_card)
        val status = findViewById<TextView>(R.id.connection_status)

        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val safeArea = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            val bars = insets.getInsets(safeArea)
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        app.connection.observe(this) { state ->
            lowTargetToggle.setOnCheckedChangeListener(null)
            lowTargetToggle.isEnabled = state.framework != null && !state.saving
            lowTargetToggle.isChecked = state.allowLowTargetSdk
            lowTargetToggle.setOnCheckedChangeListener { _, enabled ->
                app.setLowTargetSdkAllowed(enabled)
            }
            renderConnectionStatus(state, statusCard, status)
        }
    }

    private fun renderConnectionStatus(state: Connection, card: MaterialCardView, text: TextView) {
        val message = when {
            state.saveFailed -> getString(R.string.setting_error)
            state.framework != null -> getString(R.string.framework_connected, state.framework)
            else -> getString(R.string.connection_required)
        }
        val (containerColor, textColor) = when {
            state.saveFailed -> MaterialR.attr.colorErrorContainer to MaterialR.attr.colorOnErrorContainer
            state.framework != null -> MaterialR.attr.colorSecondaryContainer to MaterialR.attr.colorOnSecondaryContainer
            else -> MaterialR.attr.colorSurfaceContainer to MaterialR.attr.colorOnSurfaceVariant
        }
        card.setCardBackgroundColor(MaterialColors.getColor(card, containerColor))
        text.setTextColor(MaterialColors.getColor(text, textColor))
        text.text = message
    }
}
