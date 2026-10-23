package com.jerry.electrical_checklist;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;

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
    public Map<String, String> loadOptions(ChecklistCatalog.Aircraft model) {
        Map<String, String> values = new LinkedHashMap<>();
        for (ChecklistCatalog.ChecklistOption option : model.options) {
            String saved = preferences.getString("option/" + model.id + "/" + option.id, option.defaultValue);
            values.put(option.id, option.values.contains(saved) ? saved : option.defaultValue);
        }
        return values;
    }
    public void saveOptions(String aircraftId, Map<String, String> values) {
        SharedPreferences.Editor editor = preferences.edit();
        for (Map.Entry<String, String> entry : values.entrySet())
            editor.putString("option/" + aircraftId + "/" + entry.getKey(), entry.getValue());
        editor.apply();
    }
}
