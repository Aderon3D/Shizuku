package moe.shizuku.manager.core.ui.helpers

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.isNightMode
import moe.shizuku.manager.core.extensions.isWatch
import moe.shizuku.manager.core.preferences.data.PreferencesRepository

class ThemeHelper(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) {
    val recreateTrigger: Flow<Unit> = combine(
        preferencesRepository.theme.flow,
        preferencesRepository.amoledBlack.flow,
        preferencesRepository.dynamicColor.flow
    ) { theme, amoled, dynamic ->
        Triple(theme, amoled, dynamic)
    }
        .drop(1)
        .distinctUntilChanged()
        .map { }

    fun applyTheme(activity: AppCompatActivity) {
        val theme = preferencesRepository.theme.get().value
        val currentTheme = AppCompatDelegate.getDefaultNightMode()
        if (theme != currentTheme)
            AppCompatDelegate.setDefaultNightMode(theme)

        activity.setTheme(R.style.Theme_App)

        // Dynamic color overrides theme overlays, apply first
        val dynamicColor = preferencesRepository.dynamicColor.get()
        if (dynamicColor) {
            DynamicColors.applyToActivityIfAvailable(activity)
        }

        val amoledBlack = preferencesRepository.amoledBlack.get() ||
                context.isWatch
        if (activity.resources.isNightMode && amoledBlack) {
            activity.theme.applyStyle(R.style.ThemeOverlay_App_AmoledBlack, true)
        }
    }
}
