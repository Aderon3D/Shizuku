package moe.shizuku.manager;

import android.text.TextUtils;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

import moe.shizuku.manager.core.utils.EnvironmentUtils;

public class ShizukuSettings {

    @AppCompatDelegate.NightMode
    public static int getNightMode() {
        int defValue = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (EnvironmentUtils.isWatch()) {
            defValue = AppCompatDelegate.MODE_NIGHT_YES;
        }
        return getPreferences().getInt(Keys.KEY_THEME, defValue);
    }

    public static Locale getLocale() {
        String tag = getPreferences().getString(Keys.KEY_LANGUAGE, null);
        if (TextUtils.isEmpty(tag) || "SYSTEM".equals(tag)) {
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(tag);
    }
}
