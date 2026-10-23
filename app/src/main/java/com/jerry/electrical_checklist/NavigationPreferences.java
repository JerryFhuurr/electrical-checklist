package com.jerry.electrical_checklist;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.jerry.electrical_checklist.ChecklistCatalog.Aircraft;

/** Navigation preferences and history are independent of checklist progress. */
public final class NavigationPreferences {
    public static final String MANUFACTURERS = "manufacturers", FAVORITE = "favorite", LAST_CHECKLIST = "last-checklist";
    private final SharedPreferences preferences;
    public NavigationPreferences(Context context) {
        preferences = context.getSharedPreferences("navigation_preferences", Context.MODE_PRIVATE);
    }
    public String startupMode() {
        String value = preferences.getString("startup", MANUFACTURERS);
        return FAVORITE.equals(value) || LAST_CHECKLIST.equals(value) ? value : MANUFACTURERS;
    }
    public void setStartupMode(String mode) {
        if (!MANUFACTURERS.equals(mode) && !FAVORITE.equals(mode) && !LAST_CHECKLIST.equals(mode))
            throw new IllegalArgumentException("Invalid startup mode");
        preferences.edit().putString("startup", mode).apply();
    }
    public boolean isFavorite(String id) { return preferences.getStringSet("favorites", new HashSet<>()).contains(id); }
    public void setFavorite(String id, boolean favorite) {
        Set<String> ids = new HashSet<>(preferences.getStringSet("favorites", new HashSet<>()));
        if (favorite) ids.add(id); else ids.remove(id);
        preferences.edit().putStringSet("favorites", ids).apply();
    }
    public List<Aircraft> favorites(ChecklistCatalog catalog) {
        List<Aircraft> models = new ArrayList<>();
        for (Aircraft model : catalog.aircraft) if (isFavorite(model.id)) models.add(model);
        models.sort((left, right) -> left.name.compareToIgnoreCase(right.name));
        return models;
    }
    public void setStartupFavorite(String id) { preferences.edit().putString("startup_favorite", id).apply(); }
    public Aircraft startupFavorite(ChecklistCatalog catalog) {
        List<Aircraft> models = favorites(catalog);
        String preferred = preferences.getString("startup_favorite", "");
        for (Aircraft model : models) if (model.id.equals(preferred)) return model;
        return models.isEmpty() ? null : models.get(0);
    }
    private List<String> recentIds() {
        List<String> ids = new ArrayList<>();
        try {
            JSONArray saved = new JSONArray(preferences.getString("recent", "[]"));
            for (int i = 0; i < saved.length(); i++) {
                String id = saved.getString(i);
                if (!ids.contains(id)) ids.add(id);
            }
        } catch (JSONException ignored) { ids.clear(); }
        return ids;
    }
    public void recordAircraft(String id) {
        List<String> ids = recentIds();
        ids.remove(id); ids.add(0, id);
        if (ids.size() > 5) ids = new ArrayList<>(ids.subList(0, 5));
        preferences.edit().putString("recent", new JSONArray(ids).toString()).apply();
    }
    public List<Aircraft> recentAircraft(ChecklistCatalog catalog) {
        List<Aircraft> models = new ArrayList<>();
        for (String id : recentIds()) for (Aircraft model : catalog.aircraft)
            if (model.id.equals(id)) { models.add(model); break; }
        return models;
    }
    public void saveLastChecklist(String aircraft, boolean extended, String category, String checklist) {
        preferences.edit().putString("last_aircraft", aircraft).putBoolean("last_extended", extended)
                .putString("last_category", category).putString("last_checklist", checklist).apply();
    }
    public LastChecklist lastChecklist() {
        String aircraft = preferences.getString("last_aircraft", ""), checklist = preferences.getString("last_checklist", "");
        if (aircraft.isEmpty() || checklist.isEmpty()) return null;
        return new LastChecklist(aircraft, preferences.getBoolean("last_extended", false),
                preferences.getString("last_category", ""), checklist);
    }
    public static final class LastChecklist {
        public final String aircraftId, categoryId, checklistId;
        public final boolean extended;
        private LastChecklist(String aircraft, boolean extended, String category, String checklist) {
            aircraftId = aircraft; this.extended = extended; categoryId = category; checklistId = checklist;
        }
    }
}
