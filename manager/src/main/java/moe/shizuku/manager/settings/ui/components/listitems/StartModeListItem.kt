package moe.shizuku.manager.settings.ui.components.listitems

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.platform.device.AndroidVersion
import moe.shizuku.manager.core.preferences.models.StartMode
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem
import moe.shizuku.manager.core.utils.root.RootUtils

data class StartModeListItem(
    override val value: StartMode
) : ListSelectionItem {

    @StringRes
    override val labelRes: Int = when (value) {
        StartMode.WADB -> R.string.wireless_debugging
        StartMode.ROOT -> R.string.root
    }

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO

    @StringRes
    override val descriptionRes: Int? = when (value) {
        StartMode.WADB -> if (AndroidVersion.isAtLeast11) {
            R.string.wireless_debugging_requirement
        } else {
            R.string.wireless_debugging_requirement_pre_11
        }

        else -> null
    }

    override val isEnabled: Boolean = when (value) {
        // Default to true if unknown (null) so permission can be requested
        StartMode.ROOT -> RootUtils.isRooted() ?: true
        else -> true
    }

}