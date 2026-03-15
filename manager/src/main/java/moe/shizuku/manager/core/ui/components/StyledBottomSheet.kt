package moe.shizuku.manager.core.ui.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import moe.shizuku.manager.databinding.StyledBottomSheetBinding

abstract class StyledBottomSheet : BottomSheetDialogFragment() {

    var title: Int?
        @StringRes get() = arguments?.getInt("arg_title")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_title", value ?: 0)
        }

    var footer: Int?
        @StringRes get() = arguments?.getInt("arg_footer")?.takeIf { it != 0 }
        set(value) {
            val args = arguments ?: Bundle().also { arguments = it }
            args.putInt("arg_footer", value ?: 0)
        }

    abstract fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup?): View

    private var _binding: StyledBottomSheetBinding? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = StyledBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.isVisible = title != null
        title?.let {
            binding.title.setText(it)
            binding.buttonClose.setOnClickListener { dismiss() }
        }

        binding.contentContainer.addView(onCreateContentView(layoutInflater, binding.contentContainer))

        binding.footer.isVisible = footer != null
        footer?.let {
            binding.footerText.setText(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
