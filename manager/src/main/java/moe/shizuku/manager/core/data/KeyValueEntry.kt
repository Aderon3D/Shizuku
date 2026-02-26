package moe.shizuku.manager.core.data

data class KeyValueEntry<T>(
    val key: String,
    val default: T,
)