package moe.shizuku.manager.home.models

import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

data class HelpListItem(
    override val value: HelpItem
) : ListSelectionItem {

    override val labelRes: Int = when (value) {
        HelpItem.USER_GUIDE -> R.string.help_user_guide
        HelpItem.TROUBLESHOOTING -> R.string.help_troubleshooting
        HelpItem.BUG_REPORT -> R.string.help_bug_report
        HelpItem.FEATURE_REQUEST -> R.string.help_feature_request
        HelpItem.TRANSLATE -> R.string.help_translate
        HelpItem.EMAIL -> R.string.help_email
        HelpItem.PRIVACY -> R.string.help_privacy
    }

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.LINK
}
