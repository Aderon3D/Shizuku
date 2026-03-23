package moe.shizuku.manager.core.ui.components

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialResultDialogBuilder(
    context: android.content.Context,
    private val onResult: (success: Boolean) -> Unit
) : MaterialAlertDialogBuilder(context) {

    override fun setPositiveButton(textId: Int, listener: android.content.DialogInterface.OnClickListener?): MaterialResultDialogBuilder {
        return super.setPositiveButton(textId) { _, _ -> onResult(true) } as MaterialResultDialogBuilder
    }

    override fun setPositiveButton(text: CharSequence?, listener: android.content.DialogInterface.OnClickListener?): MaterialResultDialogBuilder {
        return super.setPositiveButton(text) { _, _ -> onResult(true) } as MaterialResultDialogBuilder
    }

    override fun setNegativeButton(textId: Int, listener: android.content.DialogInterface.OnClickListener?): MaterialResultDialogBuilder {
        return super.setNegativeButton(textId) { _, _ -> onResult(false) } as MaterialResultDialogBuilder
    }

    override fun setNegativeButton(text: CharSequence?, listener: android.content.DialogInterface.OnClickListener?): MaterialResultDialogBuilder {
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

        fun <T : Enum<T>> show(
            fragmentManager: FragmentManager,
            key: T,
            builderBlock: MaterialResultDialogBuilder.() -> Unit
        ) {
            val requestKey = key.name
            val fragment = MaterialResultDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_REQUEST_KEY, requestKey) }
            }

            fragment.show(fragmentManager, requestKey)
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

fun <T : Enum<T>> Fragment.showMaterialDialog(key: T, builderBlock: MaterialResultDialogBuilder.() -> Unit) {
    MaterialResultDialogFragment.show(childFragmentManager, key, builderBlock)
}

inline fun <reified T : Enum<T>> Fragment.handleMaterialDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) {
    enumValues<T>().forEach { enumValue ->
        childFragmentManager.setFragmentResultListener(enumValue.name, viewLifecycleOwner) { _, bundle ->
            onResult(enumValue, bundle.getBoolean(MaterialResultDialogFragment.KEY_SUCCESS, false))
        }
    }
}

// Activity

fun <T : Enum<T>> ComponentActivity.showMaterialDialog(key: T, builderBlock: MaterialResultDialogBuilder.() -> Unit) {
    val fm = (this as? FragmentActivity)?.supportFragmentManager
        ?: throw IllegalStateException("Activity must be a FragmentActivity")
    MaterialResultDialogFragment.show(fm, key, builderBlock)
}

inline fun <reified T : Enum<T>> ComponentActivity.handleMaterialDialogResults(
    crossinline onResult: (key: T, success: Boolean) -> Unit
) {
    val fm = (this as? FragmentActivity)?.supportFragmentManager ?: return
    enumValues<T>().forEach { enumValue ->
        fm.setFragmentResultListener(enumValue.name, this) { _, bundle ->
            onResult(enumValue, bundle.getBoolean(MaterialResultDialogFragment.KEY_SUCCESS, false))
        }
    }
}

// View

fun <T : Enum<T>> View.showMaterialDialog(key: T, builderBlock: MaterialResultDialogBuilder.() -> Unit) {
    val activity = context as? FragmentActivity
        ?: throw IllegalStateException("View context must be a FragmentActivity")
    MaterialResultDialogFragment.show(activity.supportFragmentManager, key, builderBlock)
}