package moe.shizuku.manager.home

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.core.android.settings.PowerManagerHelper
import moe.shizuku.manager.core.data.preferences.PreferencesRepository
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.extensions.openUrl
import moe.shizuku.manager.core.extensions.snackbar
import moe.shizuku.manager.core.extensions.viewBinding
import moe.shizuku.manager.core.ui.components.listselection.ListSelectionViewModel
import moe.shizuku.manager.core.utils.EnvironmentUtils
import moe.shizuku.manager.databinding.HomeFragmentBinding
import moe.shizuku.manager.databinding.HomeSimpleCardBinding
import moe.shizuku.manager.databinding.HomeStatusCardBinding
import moe.shizuku.manager.home.models.HomeEvent
import moe.shizuku.manager.permission.ui.authorizedapps.AuthorizedAppsViewModel
import moe.shizuku.manager.shizukuservice.models.ServiceStatus
import moe.shizuku.manager.shizukuservice.services.AdbPairingService
import moe.shizuku.manager.shizukuservice.ui.AdbPairDialogFragment
import moe.shizuku.manager.shizukuservice.ui.showAccessibilityDialog
import moe.shizuku.manager.updater.UpdateHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.lifecycle.Status
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

class HomeFragment : Fragment(R.layout.home_fragment) {
    companion object {
        const val ARG_SHOW_PAIRING_DIALOG = "show_pairing_dialog"
        const val ARG_START_SERVICE = "start_service"
    }

    private val homeModel: HomeViewModel by viewModels()
    private val listSelectionModel: ListSelectionViewModel by viewModels()
    private val appsModel: AuthorizedAppsViewModel by viewModels()

    private val binding by viewBinding(HomeFragmentBinding::bind)

    private val stateListener: (ShizukuStateMachine.State) -> Unit = {
        if (ShizukuStateMachine.isRunning()) {
            homeModel.reload()
            appsModel.load()
        } else if (ShizukuStateMachine.isDead()) {
            homeModel.reload()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.applySystemBarsPadding(bottom = true, start = true, end = true)

        setupCards()

        requireActivity().addMenuProvider(
            HomeMenuProvider(this),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        val shouldShowAccessibilityPairingDialog =
            arguments?.getBoolean(ARG_SHOW_PAIRING_DIALOG, false) ?: false
        if (shouldShowAccessibilityPairingDialog) {
            showAccessibilityDialog(requireContext())
            arguments?.putBoolean(ARG_SHOW_PAIRING_DIALOG, false)
        }

        val shouldStartService = arguments?.getBoolean(ARG_START_SERVICE, false) ?: false
        if (shouldStartService) {
            val nm =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AdbPairingService.NOTIFICATION_ID)
        }

        homeModel.serviceStatus.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                val status = homeModel.serviceStatus.value?.data ?: return@observe
                binding.statusCard.update(status)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    homeModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
                launch {
                    listSelectionModel.results.collect {
                        homeModel.handleSelectionResult(it)
                    }
                }
            }
        }

        homeModel.checkBatteryOptimization()

        appsModel.grantedCount.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                val grantedCount = it.data ?: 0
                binding.authorizedAppsCard.title.text = resources.getQuantityString(
                    R.plurals.authorized_apps_count,
                    grantedCount,
                    grantedCount,
                )
            }
        }

        lifecycleScope.launch {
            if (UpdateHelper.isCheckForUpdatesEnabled() && UpdateHelper.isNewUpdateAvailable()) {
                snackbar(
                    msg = getString(R.string.update_available),
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.update) {
                    lifecycleScope.launch {
                        UpdateHelper.update()
                    }
                }.show()
                UpdateHelper.updateLastPromptedVersion()
            }
        }

        ShizukuStateMachine.addListener(stateListener)
    }

    private fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OpenUrl -> openUrl(event.url)

            HomeEvent.ShowRebootDialog -> {
                showExitDialog(
                    getString(R.string.home_reboot_required),
                    getString(R.string.home_reboot_required_message),
                )
            }

            HomeEvent.ShowUninstallDialog -> {
                showExitDialog(
                    getString(R.string.home_duplicate_app_detected),
                    getString(R.string.home_duplicate_app_detected_message),
                )
            }

            HomeEvent.ShowBatteryOptimizationSnackbar -> {
                snackbar(
                    msg = getString(R.string.home_battery_optimization),
                    duration = Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.fix) {
                    val intent = PowerManagerHelper.getBatteryOptimizationIntent()
                    startActivity(intent)
                }.show()
            }
        }
    }

    private fun setupCards() = with(binding) {
        statusCard.apply {
            buttonStart.setOnClickListener {

            }

            if (EnvironmentUtils.isTlsSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                buttonPair.setOnClickListener {
                    onPairClicked(requireContext())
                }
            } else {
                buttonPair.visibility = GONE
            }
        }

        stealthCard.setup(
            title = getString(R.string.stealth_mode),
            icon = R.drawable.ic_visibility_off_outline_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_stealth)
            }
        )

        authorizedAppsCard.setup(
            title = getString(R.string.authorized_apps),
            icon = R.drawable.ic_settings_outline_24dp,
            onClick = {
                findNavController().navigate(R.id.navigate_to_authorized_apps)
            }
        )

        terminalCard.setup(
            title = getString(R.string.terminal_apps),
            icon = R.drawable.ic_terminal_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_terminal_apps)
            }
        )

        intentsCard.setup(
            title = getString(R.string.intents),
            icon = R.drawable.ic_integration_instructions_24,
            onClick = {
                findNavController().navigate(R.id.navigate_to_intents)
            }
        )
    }

    private fun HomeStatusCardBinding.update(status: ServiceStatus) {
            val ok = status.isRunning
            val isRoot = status.uid == 0
            val apiVersion = status.apiVersion
            val patchVersion = status.patchVersion
            if (ok) {
                icon.setImageResource(R.drawable.ic_server_ok_24dp)
            } else {
                icon.setImageResource(R.drawable.ic_server_error_24dp)
            }
            val user = if (isRoot) "root" else "adb"
            val title =
                if (ok) {
                    getString(R.string.status_running)
                } else {
                    getString(R.string.status_stopped)
                }
            val versionStr =
                getString(
                    R.string.status_version,
                    "$apiVersion.$patchVersion",
                    user,
                )
            val updateStr =
                getString(
                    R.string.status_version_update,
                    "${Shizuku.getLatestServiceVersion()}.${ShizukuApiConstants.SERVER_PATCH_VERSION}",
                )
            val summary =
                if (ok) {
                    if (apiVersion != Shizuku.getLatestServiceVersion() ||
                        status.patchVersion != ShizukuApiConstants.SERVER_PATCH_VERSION
                    ) {
                        "$versionStr. $updateStr"
                    } else {
                        versionStr
                    }
                } else {
                    ""
                }
            this.title.text = title
            this.summary.text = summary
    }

    private fun HomeSimpleCardBinding.setup(
        title: CharSequence?,
        icon: Int,
        onClick: () -> Unit
    ) {
        this.title.text = title
        this.icon.setImageResource(icon)
        root.setOnClickListener { onClick() }
    }

    private fun showExitDialog(
        title: String,
        message: String,
    ) {
        val dialog =
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.exit, null)
                .setOnDismissListener {
                    requireActivity().finishAffinity()
                }.create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked(context: Context) {
        if (EnvironmentUtils.isTelevision()) {
            showAccessibilityDialog(context)
        } else if (PreferencesRepository.legacyPairing.get()) {
            (context as? FragmentActivity)?.supportFragmentManager?.let {
                AdbPairDialogFragment().show(it)
            }
        } else {
            findNavController().navigate(R.id.navigate_to_pairing)
        }
    }
    private fun stopButton() {
        if (ShizukuStateMachine.isRunning()) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.stop_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                    runCatching { Shizuku.exit() }
                }.setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }


    override fun onResume() {
        super.onResume()
        homeModel.reload()
        appsModel.load()
    }

    override fun onDestroyView() {
        ShizukuStateMachine.removeListener(stateListener)
        super.onDestroyView()
    }
}
