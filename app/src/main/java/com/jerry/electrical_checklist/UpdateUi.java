package com.jerry.electrical_checklist;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;
import com.jerry.electrical_checklist.GitHubReleaseClient.Release;
import com.jerry.electrical_checklist.GitHubReleaseClient.Result;
import com.jerry.electrical_checklist.GitHubReleaseClient.Status;

/** Foreground prompts only; automatic prompts wait for Home and an unobstructed window. */
final class UpdateUi {
    private final AppCompatActivity activity;
    private final UpdateRepository repository;
    private final BooleanSupplier automaticAllowed;
    private final String installed;
    private AlertDialog dialog;
    private Release pendingManual;
    private long manualStarted;
    private boolean disposed;
    private boolean restoringPrompt;

    UpdateUi(AppCompatActivity activity, BooleanSupplier automaticAllowed) {
        this.activity = activity; this.automaticAllowed = automaticAllowed;
        repository = ((ChecklistApplication) activity.getApplication()).updates();
        try { installed = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName; }
        catch (android.content.pm.PackageManager.NameNotFoundException error) { throw new IllegalStateException(error); }
        status(activity.getString(R.string.update_installed_version, installed));
        View networkSettings = activity.findViewById(R.id.app_network_settings_button);
        if (networkSettings != null) networkSettings.setOnClickListener(v -> {
            try {
                activity.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + activity.getPackageName())));
            } catch (android.content.ActivityNotFoundException error) {
                android.widget.Toast.makeText(activity, R.string.update_settings_unavailable, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }
    void checkAutomatically() {
        request(false, false); maybePresent();
    }
    void checkManually() {
        manualStarted = repository.now(); pendingManual = null;
        networkHelp(false);
        checking(true); request(true, true);
    }
    private void request(boolean force, boolean manual) {
        WeakReference<UpdateUi> reference = new WeakReference<>(this);
        long started = manualStarted;
        repository.check(force, result -> {
            UpdateUi ui = reference.get();
            if (ui != null && !ui.disposed) ui.received(result, manual, started);
        });
    }
    private void received(Result result, boolean manual, long started) {
        if (manual) {
            if (started != manualStarted) return;
            checking(false);
            networkHelp(result.status == Status.NETWORK_UNAVAILABLE || result.status == Status.DNS_ERROR);
            if (result.status == Status.RELEASE && UpdateRules.newer(result.release.version, installed)) {
                pendingManual = result.release;
                status(activity.getString(R.string.update_available_status, result.release.version));
            } else {
                manualStarted = 0; pendingManual = null;
                int message = result.status == Status.RELEASE ? R.string.update_no_newer
                        : result.status == Status.NO_RELEASE ? R.string.update_no_release
                        : result.status == Status.NO_APK ? R.string.update_no_apk
                        : result.status == Status.RATE_LIMITED ? R.string.update_rate_limited
                        : result.status == Status.NETWORK_UNAVAILABLE ? R.string.update_network_unavailable
                        : result.status == Status.DNS_ERROR ? R.string.update_dns_error
                        : result.status == Status.CONNECTION_TIMEOUT ? R.string.update_timeout
                        : result.status == Status.SECURE_CONNECTION_ERROR ? R.string.update_secure_error : R.string.update_check_failed;
                status(activity.getString(message));
            }
        }
        maybePresent();
    }
    void maybePresent() {
        if (disposed || activity.isFinishing() || activity.isDestroyed() || dialog != null
                || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)
                || (!activity.getWindow().getDecorView().hasWindowFocus() && !restoringPrompt)) return;
        if (pendingManual != null) { show(pendingManual); return; }
        if (!automaticAllowed.getAsBoolean()) return;
        Release release = repository.preferences.cached(repository.now());
        if (release != null && UpdateRules.newer(release.version, installed)
                && !repository.preferences.suppressed(release, repository.now())) show(release);
    }
    private void show(Release release) {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_app_update, null);
        ((TextView) content.findViewById(R.id.update_version_message)).setText(
                activity.getString(R.string.update_version_message, release.version, installed));
        AlertDialog prompt = new MaterialAlertDialogBuilder(activity).setTitle(R.string.update_available_title)
                .setView(content).setPositiveButton(R.string.update_download, null)
                .setNegativeButton(R.string.update_skip, (d, w) -> repository.preferences.skip(release))
                .setNeutralButton(R.string.update_later, (d, w) -> repository.preferences.remindLater(release, repository.now()))
                .create();
        dialog = prompt;
        restoringPrompt = false;
        prompt.setOnCancelListener(d -> repository.preferences.remindLater(release, repository.now()));
        prompt.setOnDismissListener(d -> {
            dialog = null;
            if (!disposed) { manualStarted = 0; pendingManual = null; }
        });
        prompt.setOnShowListener(d -> prompt.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (BrowserLauncher.open(activity, release.downloadUrl)) {
                repository.preferences.remindLater(release, repository.now()); prompt.dismiss();
            }
        }));
        prompt.show();
    }
    private void checking(boolean checking) {
        MaterialButton button = activity.findViewById(R.id.check_updates_button);
        if (button != null) {
            button.setEnabled(!checking);
            button.setText(checking ? R.string.update_checking : R.string.check_updates);
        }
        if (checking) status(activity.getString(R.string.update_checking));
    }
    private void status(String message) {
        TextView status = activity.findViewById(R.id.update_status);
        if (status != null) status.setText(message);
    }
    private void networkHelp(boolean visible) {
        View help = activity.findViewById(R.id.app_network_settings_button);
        if (help != null) help.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    void saveState(Bundle state) {
        state.putLong("update_manual_started", manualStarted);
        state.putBoolean("update_prompt_visible", dialog != null || restoringPrompt);
        TextView status = activity.findViewById(R.id.update_status);
        if (status != null) state.putCharSequence("update_status", status.getText());
        View help = activity.findViewById(R.id.app_network_settings_button);
        state.putBoolean("update_network_help", help != null && help.getVisibility() == View.VISIBLE);
    }
    void restoreState(Bundle state) {
        if (state == null) return;
        restoringPrompt = state.getBoolean("update_prompt_visible");
        CharSequence message = state.getCharSequence("update_status");
        if (message != null) status(message.toString());
        networkHelp(state.getBoolean("update_network_help"));
        manualStarted = state.getLong("update_manual_started");
        if (manualStarted > 0) {
            Result completed = repository.isChecking() ? null : repository.resultSince(manualStarted);
            if (completed != null) received(completed, true, manualStarted);
            else { checking(true); request(true, true); }
        }
    }
    void dispose() {
        disposed = true;
        hidePrompt();
    }
    void hidePrompt() {
        if (dialog != null) {
            // Remove the window before activity recreation, without treating it as a user choice.
            restoringPrompt = true;
            dialog.setOnDismissListener(null);
            dialog.dismiss(); dialog = null;
        }
    }
}
