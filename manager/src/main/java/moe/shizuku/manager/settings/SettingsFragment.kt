package moe.shizuku.manager.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.app.SnackbarHelper
import moe.shizuku.manager.core.android.receivers.NotifCancelReceiver
import moe.shizuku.manager.core.android.settings.SystemSettingsHelper
import moe.shizuku.manager.core.data.preferences.PreferenceKeys
import moe.shizuku.manager.core.data.preferences.PreferenceSync
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.toast
import moe.shizuku.manager.core.ui.ThemeHelper
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.watchdog.services.WatchdogService
import rikka.core.util.ResourceUtils
import rikka.material.app.LocaleDelegate
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.manager.ShizukuLocales
import java.util.Locale
import kotlin.coroutines.resume

class SettingsFragment :
    PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var startOnBootPreference: TwoStatePreference
    private lateinit var watchdogPreference: TwoStatePreference
    private lateinit var tcpModePreference: TwoStatePreference
    private lateinit var tcpPortPreference: EditTextPreference
    private lateinit var languagePreference: ListPreference
    private lateinit var themePreference: IntegerSimpleMenuPreference
    private lateinit var amoledBlackPreference: TwoStatePreference
    private lateinit var dynamicColorPreference: TwoStatePreference
    private lateinit var updateChannelPreference: IntegerSimpleMenuPreference
    private lateinit var legacyPairingPreference: TwoStatePreference
    private lateinit var advancedCategory: PreferenceCategory

    private lateinit var batteryOptimizationListener: ActivityResultLauncher<Intent>
    private var batteryOptimizationContinuation: CancellableContinuation<Boolean>? = null

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            tcpModePreference.icon = maybeGetRestartIcon(PreferenceKeys.TCP_MODE.key)
            tcpPortPreference.icon = maybeGetRestartIcon(PreferenceKeys.TCP_PORT.key)
        }
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        val context = requireContext()

        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = "settings"
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)

        startOnBootPreference = findPreference(PreferenceKeys.START_ON_BOOT.key)!!
        watchdogPreference = findPreference(PreferenceKeys.WATCHDOG.key)!!
        tcpModePreference = findPreference(PreferenceKeys.TCP_MODE.key)!!
        tcpPortPreference = findPreference(PreferenceKeys.TCP_PORT.key)!!
        languagePreference = findPreference(PreferenceKeys.LANGUAGE.key)!!
        themePreference = findPreference(PreferenceKeys.THEME.key)!!
        amoledBlackPreference = findPreference(PreferenceKeys.AMOLED_BLACK.key)!!
        dynamicColorPreference = findPreference(PreferenceKeys.DYNAMIC_COLOR.key)!!
        updateChannelPreference = findPreference(PreferenceKeys.UPDATE_CHANNEL.key)!!
        legacyPairingPreference = findPreference(PreferenceKeys.LEGACY_PAIRING.key)!!
        advancedCategory = findPreference("category_advanced")!!

        batteryOptimizationListener =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val accepted = SystemSettingsHelper.isIgnoringBatteryOptimizations(requireContext())
                batteryOptimizationContinuation?.resume(accepted)
            }

        startOnBootPreference.apply {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
                EnvironmentUtils.isTelevision() ||
                EnvironmentUtils.isRooted()
            ) {
                isChecked = PreferenceSync.isBootReceiverEnabled(context)

                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        val doToggle = {
                            maybeToggleBatterySensitiveSetting(newValue) { result ->
                                if (result) {
                                    PreferencesRepository.setStartOnBoot(newValue)
                                    isChecked = PreferenceSync.isBootReceiverEnabled(context)
                                }
                            }
                        }

                        // https://r.android.com/2128832
                        if (
                            newValue &&
                            !EnvironmentUtils.isTelevision() &&
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        ) {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(android.R.string.dialog_alert_title)
                                .setMessage(R.string.settings_start_on_boot_bug)
                                .setPositiveButton(android.R.string.ok) { _, _ -> doToggle() }
                                .setNegativeButton(android.R.string.cancel) { _, _ ->
                                    isChecked = !newValue
                                }
                                .show()
                        } else {
                            doToggle()
                        }
                    }
                    false
                }
            } else {
                isEnabled = false
                isChecked = false
                summary = context.getString(R.string.settings_start_on_boot_summary)
            }
        }

        watchdogPreference.apply {
            isChecked = WatchdogService.isRunning()

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    maybeToggleBatterySensitiveSetting(newValue) { result ->
                        if (result) {
                            PreferencesRepository.setWatchdog(newValue)
                            isChecked = WatchdogService.isRunning()
                        }
                    }
                }
                false
            }
        }

        tcpModePreference.apply {
            if (EnvironmentUtils.isTlsSupported()) {
                summary = context.getString(R.string.settings_tcp_mode_summary)
                icon = maybeGetRestartIcon(PreferenceKeys.TCP_MODE.key)
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        val applyChange: () -> Unit = {
                            PreferencesRepository.setTcpMode(newValue)
                            isChecked = newValue
                            isEnabled = true
                            summary = context.getString(R.string.settings_tcp_mode_summary)
                            icon = maybeGetRestartIcon(PreferenceKeys.TCP_MODE.key)
                            tcpPortPreference.isVisible = newValue
                        }

                        if (!newValue && !ShizukuStateMachine.isRunning() && needsRestart(
                                PreferenceKeys.TCP_MODE.key,
                                newValue
                            )
                        ) {
                            promptStopTcp { applyChange() }
                        } else {
                            maybePromptRestart(
                                PreferenceKeys.TCP_MODE.key,
                                newValue
                            ) { applyChange() }
                        }
                    }
                    false
                }
            } else if (EnvironmentUtils.isTelevision()) {
                isEnabled = false
                isChecked = true
            } else {
                isVisible = false
            }
        }

        tcpPortPreference.apply {
            isVisible = tcpModePreference.isVisible && tcpModePreference.isChecked
            icon = maybeGetRestartIcon(PreferenceKeys.TCP_PORT.key)

            setOnBindEditTextListener { editText ->
                editText.hint = PreferenceKeys.TCP_PORT.default.toString()
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

            setOnPreferenceChangeListener { _, newValue ->
                val port = (newValue as? String)?.toIntOrNull() ?: PreferenceKeys.TCP_PORT.default
                if (port in 1..65535) {
                    val applyChange: () -> Unit = {
                        PreferencesRepository.setTcpPort(port)
                        text = port.toString()
                        icon = maybeGetRestartIcon(PreferenceKeys.TCP_PORT.key)
                    }
                    maybePromptRestart(PreferenceKeys.TCP_PORT.key, port) { applyChange() }
                } else {
                    SnackbarHelper.show(
                        context,
                        requireView(),
                        context.getString(R.string.tcp_error_invalid_port)
                    )
                }
                false
            }
        }

        languagePreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                val locale: Locale =
                    if ("SYSTEM" == newValue) {
                        LocaleDelegate.systemLocale
                    } else {
                        Locale.forLanguageTag(newValue)
                    }
                LocaleDelegate.defaultLocale = locale
                activity?.recreate()
            }
            true
        }

        setupLocalePreference(languagePreference)

        themePreference.apply {
            value = ShizukuSettings.getNightMode()
            setOnPreferenceChangeListener { _, value ->
                if (value is Int) {
                    if (ShizukuSettings.getNightMode() != value) {
                        AppCompatDelegate.setDefaultNightMode(value)
                        activity?.recreate()
                    }
                }
                true
            }
        }

        amoledBlackPreference.apply {
            if (ShizukuSettings.getNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                isChecked = ThemeHelper.isBlackNightTheme()
                setOnPreferenceChangeListener { _, _ ->
                    if (ResourceUtils.isNightMode(context.resources.configuration)) {
                        activity?.recreate()
                    }
                    true
                }
            } else {
                isVisible = false
            }
        }

        dynamicColorPreference.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isChecked = ThemeHelper.isUsingSystemColor()
                setOnPreferenceChangeListener { _, value ->
                    if (value is Boolean) {
                        if (ThemeHelper.isUsingSystemColor() != value) {
                            activity?.recreate()
                        }
                    }
                    true
                }
            } else {
                isVisible = false
            }
        }

        updateChannelPreference.value = PreferencesRepository.getUpdateChannel().value

        legacyPairingPreference.apply {
            isVisible = !EnvironmentUtils.isTelevision()
        }

        advancedCategory.isVisible = legacyPairingPreference.isVisible
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        ShizukuStateMachine.addListener(stateListener)
    }

    override fun onPause() {
        ShizukuStateMachine.removeListener(stateListener)
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String?,
    ) {
        when (key) {
            PreferenceKeys.WATCHDOG.key -> watchdogPreference.isChecked =
                WatchdogService.isRunning()
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { _, insets ->
            val systemBarsInsets = insets.getInsets(Type.systemBars() or Type.displayCutout())
            recyclerView.addItemSpacing(
                left = systemBarsInsets.left.toFloat(),
                right = systemBarsInsets.right.toFloat(),
            )
            recyclerView.setPadding(
                recyclerView.paddingLeft,
                recyclerView.paddingTop,
                recyclerView.paddingRight,
                systemBarsInsets.bottom,
            )
            insets
        }

        recyclerView.fixEdgeEffect()

        return recyclerView
    }

    private fun needsRestart(
        setting: String,
        newValue: Any? = null,
    ): Boolean {
        val currentPort = EnvironmentUtils.getAdbTcpPort()
        return when (setting) {
            PreferenceKeys.TCP_MODE.key -> {
                val newMode = newValue as? Boolean ?: PreferencesRepository.getTcpMode()
                (currentPort > 0) != newMode
            }

            PreferenceKeys.TCP_PORT.key -> {
                val newPort = newValue as? Int ?: PreferencesRepository.getTcpPort()
                (currentPort > 0) && (currentPort != newPort)
            }

            else -> {
                false
            }
        }
    }

    private fun maybeGetRestartIcon(setting: String): Drawable? {
        val context = requireContext()
        if (!needsRestart(setting)) return null

        val icon = context.getDrawable(R.drawable.ic_server_restart)
        return tint(icon)
    }

    private fun tint(icon: Drawable?): Drawable? {
        val context = requireContext()
        val tintColor = TypedValue()
        context.theme.resolveAttribute(R.attr.colorOnSurfaceVariant, tintColor, true)
        icon?.mutate()?.setTint(tintColor.data)
        return icon
    }

    private fun promptStopTcp(applyChange: () -> Unit) {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(context.getString(R.string.tcp_close_port_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    AdbStarter.stopTcp(context, EnvironmentUtils.getAdbTcpPort())
                    if (EnvironmentUtils.getAdbTcpPort() <= 0) {
                        applyChange()
                    } else {
                        context.toast(R.string.tcp_error_closing)
                    }
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun maybePromptRestart(
        setting: String,
        newValue: Any? = null,
        applyChange: () -> Unit,
    ) {
        val context = requireContext()
        if (!ShizukuStateMachine.isRunning() || !needsRestart(setting, newValue)) {
            applyChange()
            context.sendBroadcast(Intent(context, NotifCancelReceiver::class.java))
        } else {
            val message =
                buildString {
                    append(context.getString(R.string.tcp_restart_required_message))
                    if (setting == PreferenceKeys.TCP_MODE.key) {
                        append(context.getString(R.string.tcp_restart_required_message_wifi_required))
                    }
                }

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.tcp_restart_required)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    applyChange()
                    ShizukuReceiverStarter.start(context, true)
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun maybeToggleBatterySensitiveSetting(
        newValue: Boolean,
        onResult: (Boolean) -> Unit,
    ) {
        val context = requireContext()
        if (!newValue || SystemSettingsHelper.isIgnoringBatteryOptimizations(context) || EnvironmentUtils.isTelevision()) {
            onResult(true)
            return
        }

        lifecycleScope.launch {
            val result =
                suspendCancellableCoroutine { continuation ->
                    batteryOptimizationContinuation = continuation
                    SnackbarHelper.show(
                        context,
                        requireView(),
                        msg = context.getString(R.string.settings_battery_optimization),
                        duration = 6000,
                        actionText = context.getString(R.string.fix),
                        action = {
                            SystemSettingsHelper.requestIgnoreBatteryOptimizations(
                                context,
                                batteryOptimizationListener
                            )
                        },
                        onDismiss = { event ->
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION && continuation.isActive) {
                                continuation.resume(false)
                            }
                        },
                    )
                }
            onResult(result)
        }
    }

    private fun setupLocalePreference(languagePreference: ListPreference) {
        val localeTags = ShizukuLocales.LOCALES
        val displayLocaleTags = ShizukuLocales.DISPLAY_LOCALES

        languagePreference.entries = displayLocaleTags
        languagePreference.entryValues = localeTags

        val currentLocaleTag = languagePreference.value
        val currentLocaleIndex = localeTags.indexOf(currentLocaleTag)
        val currentLocale = ShizukuSettings.getLocale()
        val localizedLocales = mutableListOf<CharSequence>()

        for ((index, displayLocale) in displayLocaleTags.withIndex()) {
            if (index == 0) {
                localizedLocales.add(getString(R.string.settings_follow_system))
                continue
            }

            val locale = Locale.forLanguageTag(displayLocale.toString())

            localizedLocales.add(
                if (!TextUtils.isEmpty(locale.script)) {
                    locale.getDisplayScript(currentLocale)
                } else {
                    locale.getDisplayName(currentLocale)
                }
            )
        }

        languagePreference.entries = localizedLocales.toTypedArray()

        languagePreference.summary =
            when {
                TextUtils.isEmpty(currentLocaleTag) || "SYSTEM" == currentLocaleTag -> {
                    getString(R.string.settings_follow_system)
                }

                currentLocaleIndex != -1 -> {
                    val localizedLocale = localizedLocales[currentLocaleIndex]
                    val newLineIndex = localizedLocale.indexOf('\n')
                    if (newLineIndex == -1) {
                        localizedLocale.toString()
                    } else {
                        localizedLocale.subSequence(0, newLineIndex).toString()
                    }
                }

                else -> {
                    ""
                }
            }
    }
}
