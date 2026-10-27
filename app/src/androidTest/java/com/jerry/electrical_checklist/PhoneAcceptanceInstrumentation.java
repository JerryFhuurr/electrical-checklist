package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Explicit physical-phone acceptance runner: no resets or installation/data clearing. */
public final class PhoneAcceptanceInstrumentation extends Instrumentation {
    private final List<Activity> opened = new ArrayList<>();
    private Context context;
    private boolean baseline;
    @Override public void onCreate(Bundle args) {
        baseline = args != null && "true".equals(args.getString("baseline"));
        super.onCreate(args); start();
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        StringBuilder log = new StringBuilder();
        try {
            context = getTargetContext();
            String before = progressDigest();
            log.append("Progress before: ").append(before).append('\n');
            Activity main = start(MainActivity.class, true);
            GitHubReleaseClient.Result release = new GitHubReleaseClient().call();
            require(release.status == GitHubReleaseClient.Status.RELEASE, "Live GitHub release must be readable");
            log.append("Live HTTPS: ").append(release.status).append(" version=").append(release.release.version).append('\n');
            if (baseline) {
                require(before.equals(progressDigest()), "Baseline must not change progress");
                log.append("Read-only pre-upgrade baseline PASS\n");
                result.putString("result", "PASS"); return;
            }
            Activity about = start(AboutActivity.class, false);
            click(about, "check_updates_button");
            awaitText(about, "update_status", context.getString(context.getResources()
                    .getIdentifier("update_no_newer", "string", context.getPackageName())));
            screenshot("phone-about-update.png");
            log.append("About manual update: latest available version; no connection error\n");
            runOnMainSync(about::finish); waitForIdleSync();

            ChecklistCatalog.Aircraft model = ChecklistCatalog.load(context).aircraft.stream()
                    .filter(a -> a.id.equals("ini-a350")).findFirst().get();
            require(model.checklists.size() == 8 && model.itemCount() == 58, "Replacement A350 catalog");
            require(model.extraSets.size() == 2, "Separate emergency and rejected-takeoff guidance");
            try (ReferenceSource source = ReferenceSource.openAsset(context, model.references.get(0))) {
                require(source.pageCount() == 2, "Replacement PDF has two pages");
            }
            clickDescription(main, "Choose aircraft from Airbus");
            clickDescription(main, "Open checklists for Airbus A350");
            screenshot("phone-a350-phases.png");
            clickText(main, "Before Start");
            require(readText(main, "checklist_title").equals("Before Start"), "New Before Start opens");
            View checkbox = findDescription(main.getWindow().getDecorView(), "COCKPIT PREP, COMPLETE (BOTH)");
            require(checkbox instanceof CheckBox, "New checklist contains interactive checkboxes");
            boolean initial = ((CheckBox) checkbox).isChecked();
            runOnMainSync(checkbox::performClick); waitForIdleSync();
            require(((CheckBox) checkbox).isChecked() != initial, "Checkbox toggles");
            runOnMainSync(checkbox::performClick); waitForIdleSync();
            require(((CheckBox) checkbox).isChecked() == initial, "Checkbox returns to original state");
            screenshot("phone-a350-before-start.png");
            log.append("A350: 8 normal phases / 58 checkboxes / 2 extra procedure buttons; tick and untick passed\n");

            ActivityMonitor monitor = addMonitor(DocumentReaderActivity.class.getName(), null, false);
            click(main, "view_checklist_button");
            Activity reader = monitor.waitForActivityWithTimeout(8000); removeMonitor(monitor);
            require(reader != null, "Native reader opens"); opened.add(reader);
            awaitText(reader, "reader_page_count", "Page 1 / 2"); screenshot("phone-a350-pdf-page1.png");
            click(reader, "reader_next"); awaitText(reader, "reader_page_count", "Page 2 / 2");
            screenshot("phone-a350-pdf-page2.png"); click(reader, "reader_back");
            require(readText(main, "checklist_title").equals("Before Start"), "Reader returns to checklist");
            log.append("Native source reader: pages 1 and 2 render; checklist restored\n");
            String after = progressDigest();
            require(before.equals(after), "Existing logical checklist progress must remain unchanged");
            log.append("Progress after: ").append(after).append(" (unchanged)\nPASS\n");
            result.putString("result", "PASS");
        } catch (Throwable error) {
            java.io.StringWriter stack = new java.io.StringWriter();
            error.printStackTrace(new java.io.PrintWriter(stack)); log.append(stack);
            result.putString("result", "FAIL");
        } finally {
            runOnMainSync(() -> { for (Activity activity : opened) if (!activity.isDestroyed()) activity.finish(); });
            result.putString("stream", log.toString()); finish(Activity.RESULT_OK, result);
        }
    }
    private Activity start(Class<? extends Activity> type, boolean fresh) {
        Intent intent = new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (fresh) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity activity = startActivitySync(intent); opened.add(activity); waitForIdleSync();
        return activity;
    }
    private int id(String name) { return context.getResources().getIdentifier(name, "id", context.getPackageName()); }
    private void click(Activity activity, String name) {
        runOnMainSync(() -> { View view = activity.findViewById(id(name)); require(view != null && view.isEnabled(), name); view.performClick(); });
        waitForIdleSync(); SystemClock.sleep(350);
    }
    private void clickDescription(Activity activity, String description) {
        runOnMainSync(() -> { View view = findDescription(activity.getWindow().getDecorView(), description); require(view != null, description); view.performClick(); });
        waitForIdleSync(); SystemClock.sleep(350);
    }
    private void clickText(Activity activity, String text) {
        runOnMainSync(() -> {
            View view = findText(activity.getWindow().getDecorView(), text); require(view != null, text);
            while (!view.isClickable() && view.getParent() instanceof View) view = (View) view.getParent();
            require(view.isClickable(), "Clickable parent for " + text); view.performClick();
        });
        waitForIdleSync(); SystemClock.sleep(350);
    }
    private View findDescription(View view, String text) {
        if (text.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View match = findDescription(((ViewGroup) view).getChildAt(i), text); if (match != null) return match;
        }
        return null;
    }
    private View findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View match = findText(((ViewGroup) view).getChildAt(i), text); if (match != null) return match;
        }
        return null;
    }
    private String readText(Activity activity, String name) {
        java.util.concurrent.atomic.AtomicReference<String> text = new java.util.concurrent.atomic.AtomicReference<>("");
        runOnMainSync(() -> { View view = activity.findViewById(id(name)); if (view instanceof TextView) text.set(((TextView) view).getText().toString()); });
        return text.get();
    }
    private void awaitText(Activity activity, String name, String expected) {
        long limit = SystemClock.uptimeMillis() + 15000;
        while (SystemClock.uptimeMillis() < limit) {
            waitForIdleSync(); if (expected.equals(readText(activity, name))) return;
            SystemClock.sleep(50);
        }
        throw new AssertionError(name + ": expected " + expected + ", got " + readText(activity, name));
    }
    private String progressDigest() throws Exception {
        android.content.SharedPreferences prefs = context.getSharedPreferences("checklist_progress_v1", Context.MODE_PRIVATE);
        TreeMap<String, Object> values = new TreeMap<>(prefs.getAll());
        values.put("checked", new TreeSet<>(prefs.getStringSet("checked", Collections.emptySet())));
        values.put("completed", new TreeSet<>(prefs.getStringSet("completed", Collections.emptySet())));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(values.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(); for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
        return hex.toString();
    }
    private void screenshot(String name) throws Exception {
        SystemClock.sleep(200);
        File folder = new File(context.getExternalFilesDir(null), "v121-test-evidence");
        require(folder.isDirectory() || folder.mkdirs(), "Evidence folder");
        android.graphics.Bitmap bitmap = getUiAutomation().takeScreenshot(); require(bitmap != null, "Screenshot");
        try (FileOutputStream stream = new FileOutputStream(new File(folder, name))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream);
        } finally { bitmap.recycle(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
