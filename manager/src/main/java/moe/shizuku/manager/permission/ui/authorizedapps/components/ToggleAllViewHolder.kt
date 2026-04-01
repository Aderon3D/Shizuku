package moe.shizuku.manager.permission.ui.authorizedapps.components

import android.content.pm.PackageInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.databinding.AppListToggleAllBinding
import moe.shizuku.manager.permission.PermissionManager

class ToggleAllViewHolder(
    private val permissionManager: PermissionManager,
    private val binding: AppListToggleAllBinding,
    private val getItems: () -> List<Any>,
    private val onAuthorizationsChanged: () -> Unit
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    class Factory(
        private val permissionManager: PermissionManager
    ) {
        fun create(
            parent: ViewGroup,
            getItems: () -> List<Any>,
            onAuthorizationsChanged: () -> Unit
        ): ToggleAllViewHolder {
            return ToggleAllViewHolder(
                permissionManager,
                AppListToggleAllBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                getItems,
                onAuthorizationsChanged
            )
        }
    }

    private val switchWidget get() = binding.switchWidget

    init {
        itemView.filterTouchesWhenObscured = true
        itemView.setOnClickListener(this)
        itemView.applySystemBarsPadding(start = true, end = true)
    }

    override fun onClick(v: View) {
        setAllEnabled(!areAllEnabled())
    }

    fun bind() {
        switchWidget.isChecked = areAllEnabled()
    }

    private fun setAllEnabled(enabled: Boolean) {
        val items = getItems()
        for (item in items) {
            if (item is PackageInfo) {
                try {
                    if (enabled) {
                        permissionManager.grant(item.applicationInfo!!.uid)
                    } else {
                        permissionManager.revoke(item.applicationInfo!!.uid)
                    }
                } catch (_: Exception) {
                }
            }
        }
        onAuthorizationsChanged()
    }

    private fun areAllEnabled(): Boolean {
        val items = getItems()
        val apps = items.filterIsInstance<PackageInfo>()
        if (apps.isEmpty()) {
            return false
        }
        for (item in apps) {
            try {
                if (!permissionManager.granted(item.applicationInfo!!.uid)) {
                    return false
                }
            } catch (_: Exception) {
                return false
            }
        }
        return true
    }
}
