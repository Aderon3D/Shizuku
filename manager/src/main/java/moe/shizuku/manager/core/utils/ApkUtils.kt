package moe.shizuku.manager.core.utils

import android.content.Context
import android.util.Log
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.common.Namespace
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.getAppLabel
import java.io.File

class ApkUtils(
    private val context: Context,
    private val apkSigner: ApkSigner
) {

    companion object {
        const val ORIGINAL_PACKAGE_NAME: String = "moe.shizuku.privileged.api"
    }

    private val workDir by lazy {
        File(context.cacheDir, "patcher").also {
            it.deleteRecursively()
            it.mkdirs()
        }
    }

    fun getSelfApkFile(): File = File(context.applicationInfo.sourceDir)

    fun changePackageName(apkFile: File, newPkgName: String, maybeCreateSigningKey: Boolean = false): File {
        Log.i(TAG, "Loading APK")
        val module = ApkModule.loadApkFile(apkFile)
        val manifest = module.androidManifest

        Log.i(TAG, "Changing package name")
        val oldPkgName = manifest.packageName
        manifest.packageName = newPkgName

        Log.i(TAG, "Updating provider authorities")
        val providers = manifest.getApplicationElement().getElements("provider")
        for (provider in providers) {
            val attr = provider.searchAttribute(Namespace.URI_ANDROID, "authorities")
            val auth = attr?.valueAsString ?: continue

            if (auth.startsWith(oldPkgName)) {
                val newAuth = auth.replace(oldPkgName, newPkgName)
                attr.setValueAsString(newAuth)
            }
        }

        Log.i(TAG, "Inserting signing key")
        val key = apkSigner.getSigningKey(maybeCreateSigningKey)
        val keystore = apkSigner.keystoreFile
        val keyInputSource = FileInputSource(keystore, "assets/${keystore.name}")
        module.add(keyInputSource)

        val outFile = File(workDir, "signed.apk")
        return buildAndSign(module, outFile, maybeCreateSigningKey)
    }

    fun createStubApk(pkgName: String): File {
        val outFile = File(context.filesDir, "stub.apk")

        val tableBlock = TableBlock()
        val manifest = AndroidManifestBlock()
        val dummyDex = ByteInputSource(ByteArray(0), "classes.dex")

        val module = ApkModule().apply {
            setTableBlock(tableBlock)
            setManifest(manifest)
            add(dummyDex)
        }

        val packageBlock = tableBlock.newPackage(0x7f, pkgName)
        val appName = packageBlock.getOrCreate("", "string", "app_name").apply {
            setValueAsString("${getAppLabel()} Stub")
        }
        val appIcon = packageBlock.getOrCreate("", "drawable", "ic_launcher").apply {
            setValueAsReference(R.drawable.ic_launcher)
        }

        manifest.apply {
            setPackageName(pkgName)
            versionCode = 1
            versionName = "1.0.0"
            setApplicationLabel(appName.getResourceId())
            setIconResourceId(appIcon.getResourceId())
            setTargetSdkVersion(context.applicationInfo.targetSdkVersion)
            setMinSdkVersion(context.applicationInfo.minSdkVersion)
        }

        return buildAndSign(module, outFile, true)
    }

    private fun buildAndSign(
        module: ApkModule,
        outFile: File,
        maybeCreateSigningKey: Boolean = false
    ): File {
        Log.i(TAG, "Building new APK")
        val unsignedApk = File(workDir, "unsigned.apk")
        module.writeApk(unsignedApk)

        Log.i(TAG, "Signing APK")
        val key = apkSigner.getSigningKey(maybeCreateSigningKey)
        apkSigner.sign(unsignedApk, outFile, key)

        return outFile
    }

    private fun getAppLabel(): String =
        context.getAppLabel(context.applicationInfo)

    fun getVersionName(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    fun buildApkFilename(): String {
        val safeLabel = getAppLabel()
            .lowercase()
            .replace("[^a-z0-9._-]".toRegex(), "-")

        return "$safeLabel-${getVersionName()}"
    }
}
