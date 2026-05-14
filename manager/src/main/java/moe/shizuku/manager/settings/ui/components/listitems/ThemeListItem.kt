package moe.shizuku.manager.settings.ui.components.listitems

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.models.Theme
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

data class ThemeListItem(
    override val value: Theme
) : ListSelectionItem {

    @StringRes
    override val labelRes: Int = when (value) {
        Theme.SYSTEM -> R.string.settings_system
        Theme.LIGHT -> R.string.settings_theme_light
        Theme.DARK -> R.string.settings_theme_dark
    }

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO

}