package com.jerry.electrical_checklist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

/** Display choices are independent of checklist progress and flight-path options. */
public final class DisplaySettings {
    private static final float[] FONT_SCALES = {0.85f, 1f, 1.25f, 1.5f, 1.75f};
    private final SharedPreferences preferences;
    public DisplaySettings(Context context) {
        preferences = context.getSharedPreferences("display_settings", Context.MODE_PRIVATE);
    }
    public int fontChoice() { return Math.max(0, Math.min(FONT_SCALES.length - 1, preferences.getInt("font", 1))); }
    public float fontScale() { return FONT_SCALES[fontChoice()]; }
    public void setFontChoice(int choice) {
        if (choice < 0 || choice >= FONT_SCALES.length) throw new IllegalArgumentException("Invalid font size");
        preferences.edit().putInt("font", choice).apply();
    }
    public int nightMode() {
        int mode = preferences.getInt("night", AppCompatDelegate.MODE_NIGHT_YES);
        return mode == AppCompatDelegate.MODE_NIGHT_NO || mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ? mode : AppCompatDelegate.MODE_NIGHT_YES;
    }
    public void setNightMode(int mode) {
        if (mode != AppCompatDelegate.MODE_NIGHT_YES && mode != AppCompatDelegate.MODE_NIGHT_NO
                && mode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) throw new IllegalArgumentException("Invalid theme");
        preferences.edit().putInt("night", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
    public static void styleSystemBars(Activity activity, View root) {
        boolean day = (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(root);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(day);
            controller.setAppearanceLightNavigationBars(day);
        }
    }
}
