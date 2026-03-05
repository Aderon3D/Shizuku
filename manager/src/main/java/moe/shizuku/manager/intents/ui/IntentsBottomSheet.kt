package moe.shizuku.manager.intents.ui

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.components.BaseBottomSheet
import moe.shizuku.manager.databinding.IntentsBottomSheetBinding
import moe.shizuku.manager.intents.data.TokenRepository

class IntentsBottomSheet(
    context: Context,
) : BaseBottomSheet(context) {

    private val sheetBinding = IntentsBottomSheetBinding.inflate(LayoutInflater.from(context))

    init {
        title = R.string.intents

        val authToken = TokenRepository.getAuthToken()

        sheetBinding.apply {
            fieldAction.text = getIntentAction(buttonGroup.checkedButtonId)
            fieldPackage.text = context.packageName
            fieldExtra.text = authToken

            buttonRegenerateExtra.setOnClickListener {
                promptRegenerateToken {
                    val newToken = TokenRepository.regenerateAuthToken()
                    fieldExtra.text = newToken
                }
            }

            buttonGroup.addOnButtonCheckedListener { _, buttonId, isChecked ->
                if (isChecked) {
                    fieldAction.text = getIntentAction(buttonId)
                }
            }
        }

        setContentView(sheetBinding.root)
    }

    private fun getIntentAction(buttonId: Int): String =
        when (buttonId) {
            R.id.buttonStart -> "${BuildConfig.APPLICATION_ID}.START"
            R.id.buttonStop -> "${BuildConfig.APPLICATION_ID}.STOP"
            else -> ""
        }

    private fun promptRegenerateToken(onConfirm: () -> Unit = {}) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.intents_token_regenerate)
            .setMessage(R.string.intents_token_regenerate_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onConfirm()
            }
            .show()
    }
}
