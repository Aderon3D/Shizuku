package moe.shizuku.manager.core.platform.services.user

data class DeviceUser(
    val id: Int,
    val name: String,
    val isCurrentUser: Boolean
)