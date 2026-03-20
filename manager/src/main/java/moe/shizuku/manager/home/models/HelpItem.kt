package moe.shizuku.manager.home.models

import androidx.annotation.StringRes
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

enum class HelpItem(@param:StringRes override val labelRes: Int) : ListSelectionItem {
    USER_GUIDE(R.string.help_user_guide),
    EMAIL(R.string.help_email),
    BUG_REPORT(R.string.help_bug_report),
    FEATURE_REQUEST(R.string.help_feature_request),
    TRANSLATE(R.string.help_translate),
    PRIVACY(R.string.help_privacy);

    override val type: ListSelectionItem.Type = ListSelectionItem.Type.LINK
}
