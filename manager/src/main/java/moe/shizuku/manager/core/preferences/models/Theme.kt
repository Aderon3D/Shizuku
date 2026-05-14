package moe.shizuku.manager.core.preferences.models

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import moe.shizuku.manager.R
import moe.shizuku.manager.core.preferences.data.IntEnum
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionItem

enum class Theme(
    @param:AppCompatDelegate.NightMode override val value: Int
) : IntEnum {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES)
}