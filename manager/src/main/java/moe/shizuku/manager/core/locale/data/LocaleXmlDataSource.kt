package moe.shizuku.manager.core.locale.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

class LocaleXmlDataSource(
    private val context: Context
) {
    fun getLocales(): List<Locale> {
        // locales_config.xml is generated at build time
        // Thus, the compiler doesn't have access to locale config file
        // So, we must use resources.getIdentifier, which is a discouraged API
        @SuppressLint("DiscouragedApi")
        val resId = context.resources.getIdentifier(
            "_generated_res_locale_config", "xml", context.packageName
        )
        val xpp = context.resources.getXml(resId)

        return parseLocaleConfig(xpp)
    }

    private fun parseLocaleConfig(xpp: XmlResourceParser): List<Locale> {
        var eventType = xpp.eventType
        val locales = mutableListOf<Locale>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val isLocaleTag = eventType == XmlPullParser.START_TAG &&
                    xpp.name == "locale"
            if (isLocaleTag) {
                val name = xpp.getAttributeValue(
                    "http://schemas.android.com/apk/res/android", "name"
                )
                name?.let {
                    val locale = Locale.forLanguageTag(it)
                    locales.add(locale)
                }
            }
            eventType = xpp.next()
        }

        return locales.toList()
    }
}