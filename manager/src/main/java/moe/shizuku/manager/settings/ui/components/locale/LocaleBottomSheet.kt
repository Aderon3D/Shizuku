package moe.shizuku.manager.settings.ui.components.locale

import android.content.Context
import android.view.ViewGroup.LayoutParams
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.core.ui.LocaleHelper
import moe.shizuku.manager.core.ui.components.BaseBottomSheet

class LocaleBottomSheet(
    context: Context
) : BaseBottomSheet(context) {

    init {
        title = R.string.settings_language

        val items = LocaleHelper.getLocaleEntries(context)
        val currentTag = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: ""

        val recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            layoutManager = LinearLayoutManager(context)
            adapter = LocaleAdapter(items, currentTag) { item ->
                dismiss()
                LocaleHelper.setLocale(item)
            }
        }

        setContentView(recyclerView)
    }
}
