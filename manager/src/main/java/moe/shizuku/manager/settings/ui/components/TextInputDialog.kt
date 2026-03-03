package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.databinding.TextInputDialogBinding

class TextInputDialog(
    private val context: Context,
    @get:StringRes private val titleRes: Int,
    private val placeholder: String? = null,
    private val inputType: Int = InputType.TYPE_CLASS_TEXT,
    private val maxLength: Int? = null,
    private val inputValidation: ((String?) -> Int?)? = null,
    private val onConfirm: (String) -> Unit
) {
    fun show(currentValue: Any) {
        val binding = TextInputDialogBinding.inflate(LayoutInflater.from(context))

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val input = binding.editText.text.toString()
                onConfirm(input)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        binding.inputLayout.placeholderText = placeholder

        binding.editText.apply {
            this.inputType = this@TextInputDialog.inputType
            this.filters = if (maxLength != null) {
                arrayOf(InputFilter.LengthFilter(maxLength))
            } else {
                emptyArray()
            }

            setText(currentValue.toString())
            setSelection(text?.length ?: 0)

            addTextChangedListener { text ->
                val input = text.toString()
                val errorRes = inputValidation?.invoke(input)
                val isValid = errorRes == null

                val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton?.isEnabled = isValid

                binding.inputLayout.error =
                    if (isValid) null else context.getString(errorRes)
            }
        }

        dialog.show()
        binding.editText.requestFocus()
    }
}