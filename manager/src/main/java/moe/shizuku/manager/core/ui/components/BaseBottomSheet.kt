package moe.shizuku.manager.core.ui.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import moe.shizuku.manager.databinding.BottomSheetHeaderBinding

open class BaseBottomSheet(
    protected val context: Context
) {
    private val binding = BottomSheetHeaderBinding.inflate(LayoutInflater.from(context))
    protected val dialog = BottomSheetDialog(context)

    init {
        dialog.setContentView(binding.root)
        binding.buttonClose.setOnClickListener {
            dialog.dismiss()
        }
    }

    var title: Int? = null
        set(@StringRes value) {
            field = value
            value?.let {
                binding.title.setText(it)
            }
        }

    protected fun setContentView(view: View) {
        binding.contentContainer.removeAllViews()
        binding.contentContainer.addView(view)
    }

    open fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }
}
