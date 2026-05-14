package moe.shizuku.manager.updater.models

import moe.shizuku.manager.core.utils.Version

data class AppRelease(
    val version: Version,
    val filename: String,
    val url: String,
    val digest: String
)
