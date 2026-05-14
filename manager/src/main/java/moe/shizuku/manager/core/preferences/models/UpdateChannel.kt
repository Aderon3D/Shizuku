package moe.shizuku.manager.core.preferences.models

import moe.shizuku.manager.core.preferences.data.IntEnum

enum class UpdateChannel(
    override val value: Int
) : IntEnum {
    STABLE(0),
    BETA(1)
}