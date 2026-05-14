package moe.shizuku.manager.start.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import moe.shizuku.manager.core.ui.helpers.UiText

data class StartStepItem(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val status: StartStepUiStatus
)
