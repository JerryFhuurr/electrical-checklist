package com.jerry.electrical_checklist;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;

public final class ChecklistApplication extends Application {
    private UpdateRepository updates;
    synchronized UpdateRepository updates() {
        if (updates == null) updates = new UpdateRepository(this);
        return updates;
    }
    synchronized void setUpdates(UpdateRepository repository) { updates = repository; }
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new DisplaySettings(this).nightMode());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int started;
            private boolean changingConfiguration;
            @Override public void onActivityStarted(Activity activity) {
                if (started++ == 0 && !changingConfiguration) updates().check(true, result -> { });
            }
            @Override public void onActivityStopped(Activity activity) {
                started--;
                changingConfiguration = activity.isChangingConfigurations();
            }
            @Override public void onActivityCreated(Activity activity, Bundle state) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
}
