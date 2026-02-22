package moe.shizuku.manager.core.models.preferences

interface IntEnum {
    val value: Int
}

enum class StartMode(override val value: Int): IntEnum {
    PC(0),
    WADB(1),
    ROOT(2)
}

enum class Theme(override val value: Int): IntEnum {
    SYSTEM(-1),
    LIGHT(1),
    DARK(2)
}

enum class UpdateChannel(override val value: Int): IntEnum {
    STABLE(0),
    BETA(1)
}