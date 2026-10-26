package com.jerry.electrical_checklist;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import com.jerry.electrical_checklist.GitHubReleaseClient.Release;

/** Update choices are independent of checklist progress and navigation preferences. */
final class UpdatePreferences {
    private final SharedPreferences preferences;
    UpdatePreferences(Context context) { this(context, "update_preferences"); }
    UpdatePreferences(Context context, String name) { preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE); }
    boolean checkDue(long now) {
        return UpdateRules.checkDue(preferences.getLong("last_attempt", 0), preferences.getBoolean("successful", false), now);
    }
    void attempted(long now) { preferences.edit().putLong("last_attempt", now).putBoolean("successful", false).apply(); }
    void record(GitHubReleaseClient.Result result, long now) {
        SharedPreferences.Editor editor = preferences.edit().putBoolean("successful", result.successful());
        if (result.status == GitHubReleaseClient.Status.RELEASE) {
            editor.putString("version", result.release.version).putString("download", result.release.downloadUrl).putLong("cached_at", now);
        } else if (result.successful()) editor.remove("version").remove("download").remove("cached_at");
        editor.apply();
    }
    Release cached(long now) {
        long saved = preferences.getLong("cached_at", 0);
        String version = preferences.getString("version", ""), url = preferences.getString("download", "");
        return saved > 0 && now >= saved && now - saved < UpdateRules.DAY && AppVersion.parse(version) != null
                && GitHubReleaseClient.validDownload(url) ? new Release(version, url) : null;
    }
    boolean suppressed(Release release, long now) {
        AppVersion version = AppVersion.parse(release.version);
        return version == null || UpdateRules.suppressed(version, preferences.getStringSet("skipped", new HashSet<>()),
                preferences.getString("remind_version", ""), preferences.getLong("remind_after", 0), now);
    }
    void skip(Release release) {
        HashSet<String> skipped = new HashSet<>(preferences.getStringSet("skipped", new HashSet<>()));
        skipped.add(AppVersion.parse(release.version).key());
        preferences.edit().putStringSet("skipped", skipped).apply();
    }
    void remindLater(Release release, long now) {
        String key = AppVersion.parse(release.version).key();
        HashSet<String> skipped = new HashSet<>(preferences.getStringSet("skipped", new HashSet<>()));
        skipped.remove(key);
        preferences.edit().putStringSet("skipped", skipped).putString("remind_version", key)
                .putLong("remind_after", now + UpdateRules.DAY).apply();
    }
}
