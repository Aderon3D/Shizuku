package moe.shizuku.manager.settings.ui.components.listitems

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.models.UpdateChannel
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

data class UpdateChannelListItem(
    override val value: UpdateChannel
) : ListSelectionItem {

    @StringRes
    override val labelRes: Int = when (value) {
        UpdateChannel.STABLE -> R.string.settings_update_channel_stable
        UpdateChannel.BETA -> R.string.settings_update_channel_beta
    }

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO
}