package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.RadioButtonDialogBinding
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class RadioButtonDialog<T>(
    private val context: Context,
    @get:StringRes private val titleRes: Int,
    private val entries: List<T>,
    private val getLabel: (T) -> String,
    private val getDescription: (T) -> Int? = { null },
    private val isEnabled: (T) -> Boolean = { true },
    @get:StringRes private val positiveLabel: Int = R.string.save,
    private val onConfirm: (T) -> Unit
) {
    fun show(currentValue: T) {
        val layoutInflater = LayoutInflater.from(context)
        val binding = RadioButtonDialogBinding.inflate(layoutInflater)

        var selectedValue = currentValue
        val itemBindings = mutableListOf<RadioButtonListItemBinding>()

        entries.forEach { entry ->
            val itemBinding = RadioButtonListItemBinding.inflate(layoutInflater, binding.container, false)
            val enabled = isEnabled(entry)

            itemBinding.apply {
                title.text = getLabel(entry)

                val descriptionText = getDescription(entry)
                description.text = descriptionText?.let {
                    context.getString(it)
                }
                description.isVisible = descriptionText != null

                radioButton.isChecked = entry == selectedValue

                root.isEnabled = enabled
                title.isEnabled = enabled
                description.isEnabled = enabled
                radioButton.isEnabled = enabled

                root.setOnClickListener {
                    if (enabled) {
                        selectedValue = entry
                        itemBindings.forEachIndexed { index, b ->
                            b.radioButton.isChecked = entries[index] == selectedValue
                        }
                    }
                }
            }
            itemBindings.add(itemBinding)
            binding.container.addView(itemBinding.root)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setView(binding.root)
            .setPositiveButton(positiveLabel) { _, _ ->
                onConfirm(selectedValue)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
