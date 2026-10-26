package com.jerry.electrical_checklist;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public final class AboutActivity extends AppCompatActivity {
    private UpdateUi updates;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        SettingsActivity.preparePage(this, findViewById(R.id.about_root));
        findViewById(R.id.about_back).setOnClickListener(v -> finish());
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, version));
        } catch (android.content.pm.PackageManager.NameNotFoundException error) {
            throw new IllegalStateException(error);
        }
        findViewById(R.id.github_button).setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)))); }
            catch (ActivityNotFoundException error) { Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show(); }
        });
        updates = new UpdateUi(this, () -> false);
        findViewById(R.id.check_updates_button).setOnClickListener(v -> updates.checkManually());
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
}
