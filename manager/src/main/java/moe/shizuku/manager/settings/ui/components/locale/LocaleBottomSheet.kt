package moe.shizuku.manager.settings.ui.components.locale

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.databinding.LocaleBottomSheetBinding

class LocaleBottomSheet(
    private val context: Context
) {

    fun show() {
        val items = LocaleHelper.getLocaleEntries(context)
        val currentTag = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: ""

        val binding = LocaleBottomSheetBinding.inflate(LayoutInflater.from(context))
        val dialog = BottomSheetDialog(context)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LocaleAdapter(items, currentTag) { item ->
                dialog.dismiss()
                LocaleHelper.setLocale(item)
            }
        }

        binding.title.setText(R.string.settings_language)

        dialog.setContentView(binding.root)
        dialog.show()
    }
}
