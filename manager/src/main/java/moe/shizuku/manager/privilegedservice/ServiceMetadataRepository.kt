package moe.shizuku.manager.privilegedservice

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.map
import moe.shizuku.manager.core.utils.Version
import moe.shizuku.manager.core.utils.resultOfPrivilegedService
import moe.shizuku.manager.privilegedservice.models.PrivilegedServiceError
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

class ServiceMetadataRepository {

    fun getUid(): Result<Int, PrivilegedServiceError> =
        resultOfPrivilegedService { Shizuku.getUid() }

    fun isRoot(): Result<Boolean, PrivilegedServiceError> =
        getUid().map { it == 0 }

    fun getCurrentVersion(): Result<Version, PrivilegedServiceError> =
        resultOfPrivilegedService {
            val majorVersion = Shizuku.getVersion()
            val minorVersion = Shizuku.getServerPatchVersion().takeIf { it >= 0 } ?: 0

            Version(majorVersion, minorVersion)
        }

    fun getLatestVersion(): Result<Version, PrivilegedServiceError> =
        resultOfPrivilegedService {
            val majorVersion = Shizuku.getLatestServiceVersion()
            val minorVersion = ShizukuApiConstants.SERVER_PATCH_VERSION

            Version(majorVersion, minorVersion)
        }

    fun isLatestVersion(): Result<Boolean, PrivilegedServiceError> {
        val currentVersion = getCurrentVersion().getOrElse { return Err(it) }
        val latestVersion = getLatestVersion().getOrElse { return Err(it) }

        return Ok(currentVersion >= latestVersion)
    }
}