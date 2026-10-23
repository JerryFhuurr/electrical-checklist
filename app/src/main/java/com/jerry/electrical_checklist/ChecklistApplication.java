package com.jerry.electrical_checklist;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public final class ChecklistApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new DisplaySettings(this).nightMode());
    }
}
