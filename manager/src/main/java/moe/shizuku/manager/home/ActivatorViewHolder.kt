package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.R
import moe.shizuku.manager.activator.ActivatorService
import moe.shizuku.manager.databinding.HomeActivatorBinding
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

/**
 * Card that provides one-tap Shizuku activation without Wi-Fi.
 *
 * Uses the multi-strategy activator chain:
 * 1. Try direct adb_wifi_enabled (works on stock Android)
 * 2. Try local-only hotspot (works on Samsung/Xiaomi/Oppo)
 * 3. Guide user to enable hotspot (last resort)
 *
 * After ADB is running, switches to TCP mode so Wi-Fi is
 * never needed again until the next reboot.
 */
class ActivatorViewHolder(
    binding: HomeActivatorBinding,
    root: View
) : BaseViewHolder<Any?>(root) {

    companion object {
        fun creator(): Creator<Any> {
            return Creator { inflater: LayoutInflater, parent: ViewGroup? ->
                val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
                val inner = HomeActivatorBinding.inflate(inflater, outer.root, true)
                ActivatorViewHolder(inner, outer.root)
            }
        }
    }

    init {
        binding.button1.setOnClickListener { v ->
            val context = v.context
            ActivatorService.start(context)
        }
    }
}
