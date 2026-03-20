package moe.shizuku.manager.core.data.preferences

import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import moe.shizuku.manager.core.utils.EnvironmentUtils

enum class StartMode(
    override val value: Int, @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    WADB(0, R.string.wireless_debugging),
    ROOT(1, R.string.root);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO

    override val descriptionRes: Int?
        get() = when (this) {
            WADB -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                R.string.wireless_debugging_requirement
            } else {
                R.string.wireless_debugging_requirement_pre_11
            }

            else -> null
        }

    override val isEnabled: Boolean
        get() = when (this) {
            ROOT -> EnvironmentUtils.isRooted()
            else -> true
        }
}

enum class Theme(
    @param:AppCompatDelegate.NightMode override val value: Int,
    @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.settings_system),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, R.string.settings_theme_light),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, R.string.settings_theme_dark);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
}

enum class UpdateChannel(
    override val value: Int, @param:StringRes override val labelRes: Int
) : IntEnum, ListSelectionItem {
    STABLE(0, R.string.settings_update_channel_stable),
    BETA(1, R.string.settings_update_channel_beta);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
}
