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

public final class SettingsActivity extends AppCompatActivity {
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
