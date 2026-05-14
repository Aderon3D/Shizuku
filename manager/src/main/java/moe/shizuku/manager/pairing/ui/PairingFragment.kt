package moe.shizuku.manager.pairing.ui

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.TAG
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.platform.device.RomInfo
import moe.shizuku.manager.core.platform.settings.SystemSettingsHelper
import moe.shizuku.manager.core.ui.helpers.viewBinding
import moe.shizuku.manager.databinding.PairingFragmentBinding
import moe.shizuku.manager.pairing.AdbPairingNotificationProvider
import moe.shizuku.manager.pairing.services.AdbPairingService

@RequiresApi(Build.VERSION_CODES.R)
class PairingFragment : Fragment(R.layout.pairing_fragment) {
    private val binding by viewBinding(PairingFragmentBinding::bind)

    private var notificationEnabled: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        notificationEnabled = isNotificationEnabled()

        if (notificationEnabled) {
            startPairingService()
        }

        binding.apply {
            miui.isVisible = RomInfo.isMiui

            developerOptions.setOnClickListener {
                SystemSettingsHelper.launchOrHighlightWirelessDebugging(requireContext())
            }
        }
    }

    private fun isNotificationEnabled(): Boolean {
        val nm =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(AdbPairingNotificationProvider.CHANNEL_ID)
        return nm.areNotificationsEnabled() &&
                (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)
    }

    override fun onResume() {
        super.onResume()
        startPairingService()
    }

    private fun startPairingService() {
        val intent = AdbPairingService.startIntent(requireContext())
        try {
            requireContext().startForegroundService(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "startForegroundService", e)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                // TODO
            }
        }
    }
}