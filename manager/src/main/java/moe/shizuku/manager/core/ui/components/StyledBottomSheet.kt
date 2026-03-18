package moe.shizuku.manager.core.ui.components

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import androidx.annotation.StringRes
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.core.extensions.applySystemBarsPadding
import moe.shizuku.manager.core.ui.ThemeHelper.isNightMode
import moe.shizuku.manager.databinding.StyledBottomSheetBinding

abstract class StyledBottomSheet : BottomSheetDialogFragment() {

    var titleRes: Int?
        @StringRes get() = arguments?.getInt("arg_title")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_title", value ?: 0)
        }

    var footerRes: Int?
        @StringRes get() = arguments?.getInt("arg_footer")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_footer", value ?: 0)
        }

    abstract fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View

    private var _binding: StyledBottomSheetBinding? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = StyledBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            val contentView = onCreateContentView(layoutInflater, contentContainer)

            header.isVisible = titleRes != null
            titleRes?.let {
                title.setText(it)
                buttonClose.setOnClickListener { dismiss() }
            }

            contentContainer.addView(contentView)

            footer.isVisible = footerRes != null
            footerRes?.let {
                footerText.setText(it)
            }

            enableEdgeToEdge(contentView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // We must handle edge-to-edge manually
    // The default implementation doesn't work properly
    private fun StyledBottomSheetBinding.enableEdgeToEdge(contentView: View) {
        val window = dialog?.window ?: return

        if (footer.isVisible) {
            // Apply insets to the whole bottom sheet to move the footer above the nav bar
            root.applySystemBarsPadding(bottom = true)

            // Also use a transparent nav bar since content won't scroll under the footer
            window.setNavBarScrim(false)
        } else {
            // Apply insets to the scrollable content so that it fully scrolls above the nav bar
            contentView.applySystemBarsPadding(bottom = true)

            // Apply a scrim to the nav bar only if the content is scrollable
            val updateNavigationScrim = ViewTreeObserver.OnScrollChangedListener {
                val canScroll =
                    contentView.canScrollVertically(1) || contentView.canScrollVertically(-1)
                window.setNavBarScrim(canScroll)
            }
            contentView.viewTreeObserver.addOnScrollChangedListener(updateNavigationScrim)
            contentView.post { updateNavigationScrim.onScrollChanged() }
        }
    }

    @Suppress("DEPRECATION")
    private fun Window.setNavBarScrim(scrimEnabled: Boolean) {
        val darkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
        val lightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)

        // Light nav bar icons are not supported below API 26, so we must always show a dark scrim
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            navigationBarColor = darkScrim
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isNavigationBarContrastEnforced = scrimEnabled
        } else {
            navigationBarColor = if (scrimEnabled) {
                if (resources.isNightMode()) darkScrim
                else lightScrim
            } else Color.TRANSPARENT
        }

        WindowInsetsControllerCompat(this, requireView()).run {
            isAppearanceLightNavigationBars = !resources.isNightMode()
        }
    }
}
