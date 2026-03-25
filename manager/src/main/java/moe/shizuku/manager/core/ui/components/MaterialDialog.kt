package moe.shizuku.manager.core.ui.components

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialResultDialogBuilder(
    context: Context, private val onResult: (success: Boolean) -> Unit
) : MaterialAlertDialogBuilder(context) {

    override fun setPositiveButton(
        textId: Int,
        listener: DialogInterface.OnClickListener?
    ): MaterialResultDialogBuilder {
        return super.setPositiveButton(textId) { _, _ -> onResult(true) } as MaterialResultDialogBuilder
    }

    override fun setPositiveButton(
        text: CharSequence?,
        listener: DialogInterface.OnClickListener?
    ): MaterialResultDialogBuilder {
        return super.setPositiveButton(text) { _, _ -> onResult(true) } as MaterialResultDialogBuilder
    }

    override fun setNegativeButton(
        textId: Int,
        listener: DialogInterface.OnClickListener?
    ): MaterialResultDialogBuilder {
        return super.setNegativeButton(textId) { _, _ -> onResult(false) } as MaterialResultDialogBuilder
    }

    override fun setNegativeButton(
        text: CharSequence?,
        listener: DialogInterface.OnClickListener?
    ): MaterialResultDialogBuilder {
        return super.setNegativeButton(text) { _, _ -> onResult(false) } as MaterialResultDialogBuilder
    }
}

class MaterialResultDialogFragment : DialogFragment() {

    class DialogViewModel : ViewModel() {
        var builderBlock: (MaterialResultDialogBuilder.() -> Unit)? = null
    }

    private lateinit var viewModel: DialogViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[DialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val requestKey = requireArguments().getString(ARG_REQUEST_KEY)
            ?: throw IllegalStateException("No request key provided")

        val builder = MaterialResultDialogBuilder(requireContext()) { success ->
            val result = Bundle().apply { putBoolean(EXTRA_SUCCESS, success) }
            parentFragmentManager.setFragmentResult(requestKey, result)
        }

        // Apply the DSL block to our custom builder
        viewModel.builderBlock?.invoke(builder)

        return builder.create()
    }

    companion object {
        private const val ARG_REQUEST_KEY = "request_key"
        private const val EXTRA_SUCCESS = "success"
        const val KEY_SUCCESS = "success"

        fun show(
            fragmentManager: FragmentManager,
            key: String,
            builderBlock: MaterialResultDialogBuilder.() -> Unit
        ) {
            val fragment = MaterialResultDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_REQUEST_KEY, key) }
            }

            fragment.show(fragmentManager, key)
            fragmentManager.executePendingTransactions()

            val vm = ViewModelProvider(fragment)[DialogViewModel::class.java]
            vm.builderBlock = builderBlock
        }
    }
}

/**
 * --- EXTENSIONS ---
 */

// Fragment

fun <T : Enum<T>> Fragment.showMaterialDialog(
    key: T, builderBlock: MaterialResultDialogBuilder.() -> Unit
) = MaterialResultDialogFragment.show(childFragmentManager, key.name, builderBlock)

inline fun <reified T : Enum<T>> Fragment.handleMaterialDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) = enumValues<T>().forEach { enumValue ->
    childFragmentManager.setFragmentResultListener(
        enumValue.name,
        viewLifecycleOwner
    ) { _, bundle ->
        onResult(enumValue, bundle.getBoolean(MaterialResultDialogFragment.KEY_SUCCESS, false))
    }
}

// Activity

fun <T : Enum<T>> FragmentActivity.showMaterialDialog(
    key: T, builderBlock: MaterialResultDialogBuilder.() -> Unit
) = MaterialResultDialogFragment.show(supportFragmentManager, key.name, builderBlock)

inline fun <reified T : Enum<T>> FragmentActivity.handleMaterialDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) = enumValues<T>().forEach { enumValue ->
    supportFragmentManager.setFragmentResultListener(enumValue.name, this) { _, bundle ->
        onResult(enumValue, bundle.getBoolean(MaterialResultDialogFragment.KEY_SUCCESS, false))
    }
}
