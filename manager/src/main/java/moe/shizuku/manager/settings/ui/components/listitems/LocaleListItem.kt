package moe.shizuku.manager.settings.ui.components.listitems

import moe.shizuku.manager.R
import moe.shizuku.manager.core.locale.models.LocaleEntry
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

data class LocaleListItem(
    override val value: LocaleEntry
): ListSelectionItem {
    override val label: CharSequence?
        get() = value.nameOwnLocale.takeUnless { it.isBlank() }

    override val labelRes: Int
        get() = if (value.nameOwnLocale.isBlank()) R.string.settings_system else 0

    override val description: CharSequence?
        get() = value.nameCurrentLocale.takeUnless { it.isBlank() }

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.RADIO

}
