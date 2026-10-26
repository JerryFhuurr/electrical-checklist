package com.jerry.electrical_checklist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.List;
import com.jerry.electrical_checklist.ChecklistCatalog.Aircraft;

public final class SettingsActivity extends AppCompatActivity {
    private UpdateUi updates;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        preparePage(this, findViewById(R.id.settings_root));
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());
        DisplaySettings settings = new DisplaySettings(this);
        Spinner font = findViewById(R.id.font_size_selector);
        font.setSaveEnabled(false);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.checklist_font_sizes,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        font.setAdapter(adapter);
        font.setSelection(settings.fontChoice());
        TextView preview = findViewById(R.id.font_preview);
        preview.setTextSize(12 * settings.fontScale());
        font.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.setFontChoice(position);
                preview.setTextSize(12 * settings.fontScale());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        RadioGroup theme = findViewById(R.id.theme_choices);
        int current = settings.nightMode();
        theme.check(current == AppCompatDelegate.MODE_NIGHT_NO ? R.id.theme_day
                : current == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? R.id.theme_system : R.id.theme_night);
        theme.setOnCheckedChangeListener((group, checked) -> {
            int next = checked == R.id.theme_day ? AppCompatDelegate.MODE_NIGHT_NO
                    : checked == R.id.theme_system ? AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM : AppCompatDelegate.MODE_NIGHT_YES;
            if (next != settings.nightMode()) settings.setNightMode(next);
        });
        findViewById(R.id.about_button).setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        bindStartupSettings();
        updates = new UpdateUi(this, () -> false);
        findViewById(R.id.check_updates_button).setOnClickListener(v -> updates.checkManually());
        findViewById(R.id.view_releases_button).setOnClickListener(v -> BrowserLauncher.open(this, GitHubReleaseClient.RELEASES_URL));
        updates.restoreState(state);
    }
    @Override protected void onResume() {
        super.onResume();
        if (updates != null) getWindow().getDecorView().post(updates::maybePresent);
    }
    @Override protected void onPause() {
        super.onPause();
        if (updates != null) updates.hidePrompt();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused && updates != null) updates.maybePresent();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (updates != null) updates.saveState(state);
    }
    @Override protected void onDestroy() {
        if (updates != null) updates.dispose();
        super.onDestroy();
    }
    private void bindStartupSettings() {
        NavigationPreferences navigation = new NavigationPreferences(this);
        List<Aircraft> favorites = new ArrayList<>();
        Aircraft preferred = null;
        try {
            ChecklistCatalog catalog = ChecklistCatalog.load(this);
            favorites.addAll(navigation.favorites(catalog)); preferred = navigation.startupFavorite(catalog);
        } catch (Exception error) {
            ((TextView) findViewById(R.id.startup_favorites_empty)).setText(R.string.load_error_message);
        }
        Spinner selector = findViewById(R.id.startup_favorite_selector);
        selector.setSaveEnabled(false);
        List<String> names = new ArrayList<>();
        int selection = 0;
        for (int i = 0; i < favorites.size(); i++) {
            names.add(favorites.get(i).name);
            if (favorites.get(i) == preferred) selection = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row_checklist_option_value, names);
        adapter.setDropDownViewResource(R.layout.row_checklist_option_value);
        selector.setAdapter(adapter);
        if (!favorites.isEmpty()) selector.setSelection(selection);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                navigation.setStartupFavorite(favorites.get(position).id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        RadioGroup choices = findViewById(R.id.startup_choices);
        String mode = navigation.startupMode();
        choices.check(mode.equals(NavigationPreferences.FAVORITE) ? R.id.startup_favorite
                : mode.equals(NavigationPreferences.LAST_CHECKLIST) ? R.id.startup_last_checklist : R.id.startup_manufacturers);
        showStartupFavorite(mode, !favorites.isEmpty());
        choices.setOnCheckedChangeListener((group, checked) -> {
            String next = checked == R.id.startup_favorite ? NavigationPreferences.FAVORITE
                    : checked == R.id.startup_last_checklist ? NavigationPreferences.LAST_CHECKLIST : NavigationPreferences.MANUFACTURERS;
            navigation.setStartupMode(next); showStartupFavorite(next, !favorites.isEmpty());
        });
    }
    private void showStartupFavorite(String mode, boolean hasFavorites) {
        boolean favorite = mode.equals(NavigationPreferences.FAVORITE);
        findViewById(R.id.startup_favorite_choice).setVisibility(favorite && hasFavorites ? View.VISIBLE : View.GONE);
        findViewById(R.id.startup_favorites_empty).setVisibility(favorite && !hasFavorites ? View.VISIBLE : View.GONE);
    }
    static void preparePage(AppCompatActivity activity, View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        DisplaySettings.styleSystemBars(activity, root);
    }
}
