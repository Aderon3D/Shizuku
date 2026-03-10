package moe.shizuku.manager.core.extensions

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import moe.shizuku.manager.core.ui.ThemeHelper

fun Fragment.snackbar(msg: CharSequence, duration: Int = Snackbar.LENGTH_SHORT) =
    Snackbar.make(requireView(), msg, duration).apply {
        ThemeHelper.applySnackbarTheme(requireContext(), this)
    }

fun Fragment.snackbar(@StringRes msg: Int, duration: Int = Snackbar.LENGTH_SHORT) =
    snackbar(getString(msg), duration)

fun Fragment.showSnackbar(@StringRes msg: Int, duration: Int = Snackbar.LENGTH_SHORT) =
    snackbar(msg, duration).show()