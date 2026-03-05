package moe.shizuku.manager.settings.ui.components

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import moe.shizuku.manager.core.ui.components.BaseBottomSheet
import moe.shizuku.manager.databinding.RadioButtonBottomSheetBinding
import moe.shizuku.manager.databinding.RadioButtonListItemBinding

class RadioButtonBottomSheet<T>(
    context: Context,
    @get:StringRes private val titleRes: Int,
    private val entries: List<T>,
    private val getLabel: (T) -> String,
    @get:StringRes private val getDescription: (T) -> Int? = { null },
    @get:StringRes private val footerRes: Int? = null,
    private val isEnabled: (T) -> Boolean = { true },
    private val onConfirm: (T) -> Unit
) : BaseBottomSheet(context) {

    private val binding = RadioButtonBottomSheetBinding.inflate(LayoutInflater.from(context))
    private val itemBindings = mutableListOf<RadioButtonListItemBinding>()

    init {
        title = titleRes
        setContentView(binding.root)

        val layoutInflater = LayoutInflater.from(context)
        entries.forEach { entry ->
            val itemBinding = RadioButtonListItemBinding.inflate(layoutInflater, binding.container, true)
            itemBinding.apply {
                title.text = getLabel(entry)

                val descriptionRes = getDescription(entry)
                description.text = descriptionRes?.let { context.getString(it) }
                description.isVisible = descriptionRes != null

                root.setOnClickListener {
                    if (isEnabled(entry)) {
                        onConfirm(entry)
                        dismiss()
                    }
                }
            }
            itemBindings.add(itemBinding)
        }

        binding.apply {
            footer.isVisible = footerRes != null
            footerRes?.let { footerText.setText(it) }
        }
    }

    fun show(currentValue: T) {
        entries.forEachIndexed { index, entry ->
            val itemBinding = itemBindings[index]
            val enabled = isEnabled(entry)

            itemBinding.apply {
                radioButton.isChecked = entry == currentValue
                root.isEnabled = enabled
                root.alpha = if (enabled) 1f else 0.38f
            }
        }

        super.show()
    }
}
