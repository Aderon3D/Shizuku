package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.content.pm.PackageInfo
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.utils.AppIconCache
import moe.shizuku.manager.core.utils.UserHandleCompat
import moe.shizuku.manager.databinding.AppListItemBinding
import moe.shizuku.manager.permission.PermissionManager
import moe.shizuku.manager.permission.ShizukuSystemApis
import rikka.shizuku.Shizuku

class AppViewHolder(
    private val binding: AppListItemBinding,
    private val permissionManager: PermissionManager,
    private val shizukuSystemApis: ShizukuSystemApis,
    private val userHandleCompat: UserHandleCompat,
    private val appIconCache: AppIconCache,
    private val onAuthorizationsChanged: () -> Unit
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    class Factory(
        private val permissionManager: PermissionManager,
        private val shizukuSystemApis: ShizukuSystemApis,
        private val userHandleCompat: UserHandleCompat,
        private val appIconCache: AppIconCache
    ) {
        fun create(
            parent: ViewGroup,
            onAuthorizationsChanged: () -> Unit
        ): AppViewHolder {
            return AppViewHolder(
                AppListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                permissionManager,
                shizukuSystemApis,
                userHandleCompat,
                appIconCache,
                onAuthorizationsChanged
            )
        }
    }

    private val icon get() = binding.icon
    private val name get() = binding.title
    private val pkg get() = binding.summary
    private val switchWidget get() = binding.switchWidget
    private val root get() = binding.requiresRoot

    private var _data: PackageInfo? = null
    private val data: PackageInfo get() = _data!!

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
        itemView.applySystemBarsPadding(start = true, end = true)
    }

    private inline val ai get() = data.applicationInfo!!
    private inline val uid get() = ai.uid

    private var loadIconJob: Job? = null

    override fun onClick(v: View) {
        val context = v.context
        try {
            if (permissionManager.granted(uid)) {
                permissionManager.revoke(uid)
            } else {
                permissionManager.grant(uid)
            }
        } catch (_: SecurityException) {
            val uid = try {
                Shizuku.getUid()
            } catch (_: Throwable) {
                return
            }
            if (uid != 0) {
                val dialog = MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.status_adb_restricted)
                    .setMessage(
                        context.getString(
                            R.string.status_adb_restricted_message,
                            "PLACEHOLDER",
                        )
                    ).setPositiveButton(android.R.string.ok, null)
                    .create()
                dialog.setOnShowListener {
                    (it as AlertDialog).findViewById<TextView>(android.R.id.message)?.movementMethod =
                        LinkMovementMethod.getInstance()
                }
                try {
                    dialog.show()
                } catch (_: Throwable) {
                }
            }
        }
        onAuthorizationsChanged()
    }

    fun bind(data: PackageInfo) {
        loadIconJob?.cancel()

        this._data = data
        val pm = itemView.context.packageManager
        val userId = userHandleCompat.getUserId(uid)
        icon.setImageResource(R.drawable.ic_default_app_icon)
        name.text = if (userId != userHandleCompat.myUserId()) {
            val userInfo = shizukuSystemApis.getUserInfo(userId)
            "${ai.loadLabel(pm)} - ${userInfo.name} ($userId)"
        } else {
            ai.loadLabel(pm)
        }
        pkg.text = ai.packageName
        updateCheckedState()
        root.visibility = if (ai.metaData != null && ai.metaData.getBoolean("moe.shizuku.client.V3_REQUIRES_ROOT")) View.VISIBLE else View.GONE

        loadIconJob = itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
            val size = icon.measuredWidth.takeIf { it > 0 }
                ?: itemView.context.resources.getDimensionPixelSize(R.dimen.default_app_icon_size)

            appIconCache.loadIcon(ai, userId, size)?.let {
                icon.setImageBitmap(it)
                return@launch
            } ?: icon.setImageResource(R.drawable.ic_default_app_icon)
        }
    }

    fun updateCheckedState() {
        switchWidget.isChecked = permissionManager.granted(uid)
    }
}
