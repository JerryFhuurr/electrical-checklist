package com.jerry.electrical_checklist;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;

public final class ProgressStore {
    private final SharedPreferences preferences;
    public ProgressStore(Context context) {
        preferences = context.getSharedPreferences("checklist_progress_v1", Context.MODE_PRIVATE);
    }
    public ChecklistProgress load() {
        return new ChecklistProgress(preferences.getStringSet("checked", Collections.emptySet()),
                preferences.getStringSet("completed", Collections.emptySet()));
    }
    public void save(ChecklistProgress progress) {
        preferences.edit().putStringSet("checked", progress.checkedSnapshot())
                .putStringSet("completed", progress.completedSnapshot()).apply();
    }
}
