package moe.shizuku.manager.settings.ui.components

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import moe.shizuku.manager.core.data.preferences.StartMode

class RadioButtonBottomSheet(
    private val context: Context,
    private val onConfirm: (StartMode) -> Unit
) {
    fun show(currentValue: StartMode) {
        val dialog = BottomSheetDialog(context)

    }
}