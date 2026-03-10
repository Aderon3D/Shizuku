package moe.shizuku.manager.core.extensions

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Fragment.dialog() = MaterialAlertDialogBuilder(requireContext())

fun MaterialAlertDialogBuilder.addCancelButton() =
    setNegativeButton(android.R.string.cancel, null)


fun MaterialAlertDialogBuilder.addOkButton(onConfirm: () -> Unit = {}) =
    setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }