package moe.shizuku.manager.core.data.preferences

import moe.shizuku.manager.core.data.IntEnumStore as enumPrefs
import moe.shizuku.manager.core.data.KeyValueDataSource as prefs

object PreferencesRepository {

    // -------------------------
    // GETTERS
    // -------------------------

    fun getStartMode() = enumPrefs.get(PreferenceKeys.START_MODE)

    fun getStartOnBoot() = prefs.get(PreferenceKeys.START_ON_BOOT)

    fun getWatchdog() = prefs.get(PreferenceKeys.WATCHDOG)

    fun getTcpMode() = prefs.get(PreferenceKeys.TCP_MODE)

    fun getTcpPort() = prefs.get(PreferenceKeys.TCP_PORT)

    fun getAutoDisableUsbDebugging() = prefs.get(PreferenceKeys.AUTO_DISABLE_USB_DEBUGGING)

    fun getLanguage() = prefs.get(PreferenceKeys.LANGUAGE)

    fun getTheme() = enumPrefs.get(PreferenceKeys.THEME)

    fun getAmoledBlack() = prefs.get(PreferenceKeys.AMOLED_BLACK)

    fun getDynamicColor() = prefs.get(PreferenceKeys.DYNAMIC_COLOR)

    fun getCheckForUpdates() = prefs.get(PreferenceKeys.CHECK_FOR_UPDATES)

    fun getUpdateChannel() = enumPrefs.get(PreferenceKeys.UPDATE_CHANNEL)

    fun getLegacyPairing() = prefs.get(PreferenceKeys.LEGACY_PAIRING)

    // -------------------------
    // FLOWS
    // -------------------------

    fun observeStartOnBoot() = prefs.observe(PreferenceKeys.START_ON_BOOT)

    fun observeWatchdog() = prefs.observe(PreferenceKeys.WATCHDOG)

    // -------------------------
    // SETTERS
    // -------------------------

    fun setStartMode(value: StartMode) =
        enumPrefs.set(
            PreferenceKeys.START_MODE,
            value,
        )

    fun setStartOnBoot(value: Boolean) =
        prefs.set(
            PreferenceKeys.START_ON_BOOT,
            value,
        )

    fun setWatchdog(value: Boolean) =
        prefs.set(
            PreferenceKeys.WATCHDOG,
            value,
        )

    fun setTcpMode(value: Boolean) =
        prefs.set(
            PreferenceKeys.TCP_MODE,
            value,
        )

    fun setTcpPort(value: Int) =
        prefs.set(
            PreferenceKeys.TCP_PORT,
            value,
        )

    fun setAutoDisableUsbDebugging(value: Boolean) =
        prefs.set(
            PreferenceKeys.AUTO_DISABLE_USB_DEBUGGING,
            value,
        )

    fun setLanguage(value: String?) =
        prefs.set(
            PreferenceKeys.LANGUAGE,
            value,
        )

    fun setTheme(value: Theme) =
        enumPrefs.set(
            PreferenceKeys.THEME,
            value,
        )

    fun setAmoledBlack(value: Boolean) =
        prefs.set(
            PreferenceKeys.AMOLED_BLACK,
            value,
        )

    fun setDynamicColor(value: Boolean) =
        prefs.set(
            PreferenceKeys.DYNAMIC_COLOR,
            value,
        )

    fun setCheckForUpdates(value: Boolean) =
        prefs.set(
            PreferenceKeys.CHECK_FOR_UPDATES,
            value,
        )

    fun setUpdateChannel(value: UpdateChannel) =
        enumPrefs.set(
            PreferenceKeys.UPDATE_CHANNEL,
            value,
        )

    fun setLegacyPairing(value: Boolean) =
        prefs.set(
            PreferenceKeys.LEGACY_PAIRING,
            value,
        )

}
