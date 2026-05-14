package moe.shizuku.manager.core.locale.models

import moe.shizuku.manager.core.extensions.capitalize
import java.util.Locale

data class LocaleEntry(
    val nameOwnLocale: String,
    val nameCurrentLocale: String,
    val tag: String
) {
    companion object {
        val SystemDefault: LocaleEntry = LocaleEntry("", "", "")
    }
}

fun Locale.toLocaleEntry(): LocaleEntry {
    val currentLocale = Locale.getDefault()
    return LocaleEntry(
        nameOwnLocale = getDisplayName(this).capitalize(this),
        nameCurrentLocale = getDisplayName(currentLocale).capitalize(currentLocale),
        tag = toLanguageTag()
    )
}

fun String.toLocaleEntry(): LocaleEntry {
    return if (this.isBlank()) LocaleEntry.SystemDefault
    else Locale.forLanguageTag(this).toLocaleEntry()
}