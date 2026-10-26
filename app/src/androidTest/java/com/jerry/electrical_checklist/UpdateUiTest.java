package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import com.jerry.electrical_checklist.GitHubReleaseClient.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UpdateUiTest {
    private Instrumentation instrumentation;
    private Context context;
    private ChecklistApplication application;
    private UpdateRepository original, repository;
    private String preferenceName;
    private final AtomicLong clock = new AtomicLong(1800000000000L);
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<Result> answer = new AtomicReference<>(Result.of(Status.NO_RELEASE));
    private volatile CountDownLatch gate;
    private Release release() { return new Release("9.0", GitHubReleaseClientTest.APK); }
    @Before public void setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation(); context = instrumentation.getTargetContext();
        application = (ChecklistApplication) context.getApplicationContext(); original = application.updates();
        preferenceName = "update_ui_test_" + System.nanoTime();
        repository = new UpdateRepository(new UpdatePreferences(context, preferenceName), () -> {
            calls.incrementAndGet();
            CountDownLatch pending = gate;
            if (pending != null && !pending.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Test gate timed out");
            return answer.get();
        }, clock::get);
        application.setUpdates(repository);
        new NavigationPreferences(context).setStartupMode(NavigationPreferences.MANUFACTURERS);
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        instrumentation.runOnMainSync(() -> {
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
    }
    private void startMain() {
        closeActivities();
        instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        instrumentation.waitForIdleSync();
    }
    private void closeActivities() {
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
        });
        awaitForeground(false);
    }
    private void awaitForeground(boolean expected) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (SystemClock.uptimeMillis() < deadline) {
            java.util.concurrent.atomic.AtomicBoolean foreground = new java.util.concurrent.atomic.AtomicBoolean();
            instrumentation.runOnMainSync(() -> {
                for (Stage stage : new Stage[]{Stage.STARTED, Stage.RESUMED, Stage.PAUSED})
                    if (!ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage).isEmpty()) foreground.set(true);
            });
            if (foreground.get() == expected) { instrumentation.waitForIdleSync(); return; }
            SystemClock.sleep(20);
        }
        fail("App did not enter expected foreground state: " + expected);
    }
    private void completed() {
        long deadline = SystemClock.uptimeMillis() + 6000;
        while ((calls.get() == 0 || repository.isChecking()) && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20);
        assertFalse("Update worker must finish", repository.isChecking());
        instrumentation.waitForIdleSync();
    }
    private Activity resumed() {
        AtomicReference<Activity> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).iterator().next())); return result.get();
    }
    private void recreate() {
        Activity current = resumed();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(current.getClass().getName(), null, false);
        instrumentation.runOnMainSync(current::recreate);
        assertNotNull(monitor.waitForActivityWithTimeout(5000)); instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
    }
    private void manual() {
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed();
    }
    private void prompt() {
        onView(withText(R.string.update_available_title)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .check(matches(isDisplayed()));
    }
    private void noPrompt() { onView(withText(R.string.update_available_title)).check(doesNotExist()); }
    @After public void cleanup() {
        if (gate != null) gate.countDown();
        closeActivities();
        instrumentation.waitForIdleSync(); repository.close(); application.setUpdates(original);
        context.deleteSharedPreferences(preferenceName);
    }
    @Test public void automaticPromptSkipsOnlyChosenVersionAndResetKeepsChoice() {
        answer.set(Result.available(release())); startMain(); completed(); prompt();
        onView(withText(R.string.update_skip)).perform(click()); noPrompt();
        startMain(); completed(); noPrompt(); assertEquals(2, calls.get());
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new UpdatePreferences(context, preferenceName).suppressed(release(), clock.get()));
        repository.preferences.record(Result.available(new Release("10.0", GitHubReleaseClientTest.APK)), clock.get());
        recreate(); prompt(); onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void remindLaterWaits24HoursAndSurvivesRestart() {
        answer.set(Result.available(release())); startMain(); completed(); prompt();
        onView(withText(R.string.update_later)).perform(click());
        startMain(); completed(); noPrompt(); assertEquals(2, calls.get());
        clock.addAndGet(UpdateRules.DAY - 1); startMain(); completed(); noPrompt(); assertEquals(3, calls.get());
        clock.incrementAndGet(); startMain(); completed(); prompt(); assertEquals(4, calls.get());
        onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void responseDuringChecklistWaitsForHomeAndKeepsTicks() {
        gate = new CountDownLatch(1); answer.set(Result.available(release())); startMain();
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Safety Inspection")).perform(click());
        onView(withContentDescription("BATTERY, ON")).perform(click());
        gate.countDown(); completed(); noPrompt();
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        onView(withId(R.id.home_button)).perform(click()); prompt();
        onView(withText(R.string.update_skip)).perform(click());
        assertEquals(1, new ProgressStore(context).load().checkedSnapshot().size());
    }
    @Test public void manualCheckOverridesSkippedAndDeferredVersions() {
        repository.preferences.skip(release()); answer.set(Result.available(release()));
        startMain(); completed(); noPrompt(); manual(); prompt(); assertEquals(2, calls.get());
        onView(withText(R.string.update_later)).perform(click());
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed(); prompt();
        assertEquals(3, calls.get()); onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void manualResultsExplainOlderReleaseOfflineRateLimitAndMissingApk() {
        startMain(); completed(); answer.set(Result.available(new Release("1.0", GitHubReleaseClientTest.APK)));
        manual(); noPrompt(); onView(withId(R.id.update_status)).check(matches(withText(R.string.update_no_newer)));
        for (Status status : new Status[]{Status.ERROR, Status.RATE_LIMITED, Status.NO_RELEASE, Status.NO_APK}) {
            answer.set(Result.of(status)); onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed();
            int message = status == Status.ERROR ? R.string.update_check_failed : status == Status.RATE_LIMITED
                    ? R.string.update_rate_limited : status == Status.NO_RELEASE ? R.string.update_no_release : R.string.update_no_apk;
            onView(withId(R.id.update_status)).check(matches(withText(message)));
            onView(withId(R.id.check_updates_button)).check(matches(isEnabled())); noPrompt();
        }
    }
    @Test public void manualRequestAndDialogSurviveRecreationWithoutDuplicateNetworkRequest() {
        startMain(); completed(); answer.set(Result.available(release())); gate = new CountDownLatch(1);
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click());
        onView(withId(R.id.check_updates_button)).check(matches(not(isEnabled())));
        recreate(); gate.countDown(); completed(); prompt(); assertEquals(2, calls.get());
        recreate(); prompt(); assertEquals(2, calls.get()); onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void dismissingPromptDefersRatherThanShowingRepeatedly() {
        answer.set(Result.available(release())); startMain(); completed(); prompt(); pressBack(); noPrompt();
        assertTrue(repository.preferences.suppressed(release(), clock.get())); startMain(); noPrompt();
    }
    @Test public void automaticPromptSurvivesRecreationWithoutSavingAUserChoice() {
        answer.set(Result.available(release())); startMain(); completed(); prompt();
        recreate(); prompt(); assertEquals(1, calls.get());
        assertFalse(repository.preferences.suppressed(release(), clock.get()));
        onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void downloadUsesExternalBrowsableIntentAndDefersPrompt() {
        answer.set(Result.available(release())); startMain(); completed(); prompt();
        IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW); filter.addDataScheme("https");
        filter.addCategory(Intent.CATEGORY_BROWSABLE);
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(filter, new Instrumentation.ActivityResult(0, null), true);
        try {
            onView(withText(R.string.update_download)).perform(click());
            assertEquals(1, monitor.getHits()); noPrompt(); assertTrue(repository.preferences.suppressed(release(), clock.get()));
        } finally { instrumentation.removeMonitor(monitor); }
    }
    @Test public void automaticOfflineFailureIsSilentAndRetriesOnNextOpening() {
        answer.set(Result.of(Status.ERROR)); startMain(); completed(); noPrompt();
        startMain(); completed(); noPrompt(); assertEquals(2, calls.get());
        answer.set(Result.available(release())); startMain(); completed(); prompt();
        assertEquals(3, calls.get()); onView(withText(R.string.update_skip)).perform(click());
    }
    private void about() {
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.about_button)).perform(scrollTo(), click());
    }
    @Test public void aboutManualCheckSurvivesRecreationAndOverridesSkip() {
        startMain(); completed(); repository.preferences.skip(release());
        about(); assertEquals(1, calls.get());
        answer.set(Result.available(release())); gate = new CountDownLatch(1);
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click());
        onView(withId(R.id.check_updates_button)).check(matches(not(isEnabled())));
        recreate(); gate.countDown(); completed(); prompt(); assertEquals(2, calls.get());
        assertTrue(resumed() instanceof AboutActivity);
        recreate(); prompt(); assertEquals(2, calls.get());
        onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void returningToAboutChecksAgainButInternalNavigationDoesNot() {
        startMain(); completed(); about(); recreate(); assertEquals(1, calls.get());
        Activity current = resumed();
        instrumentation.runOnMainSync(() -> assertTrue(current.moveTaskToBack(true)));
        awaitForeground(false); answer.set(Result.available(release()));
        instrumentation.runOnMainSync(() -> context.startActivity(new Intent(context, AboutActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));
        awaitForeground(true); completed(); assertEquals(2, calls.get()); noPrompt();
        assertTrue(resumed() instanceof AboutActivity);
        pressBack(); pressBack(); prompt(); assertEquals(2, calls.get());
        onView(withText(R.string.update_skip)).perform(click());
    }
    @Test public void aboutManualCheckShowsOfflineResultAndCanRetry() {
        startMain(); completed(); about(); answer.set(Result.of(Status.ERROR));
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed();
        onView(withId(R.id.update_status)).check(matches(withText(R.string.update_check_failed)));
        onView(withId(R.id.check_updates_button)).check(matches(isEnabled()));
        answer.set(Result.available(new Release("1.0", GitHubReleaseClientTest.APK)));
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed();
        onView(withId(R.id.update_status)).check(matches(withText(R.string.update_no_newer)));
        assertEquals(3, calls.get());
    }
    @Test public void promptActionsRemainVisibleInDayLandscapeAndNightPortrait() throws Exception {
        startMain(); completed(); answer.set(Result.available(release()));
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.theme_day)).perform(scrollTo(), click());
        Activity settings = resumed();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(SettingsActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(() -> settings.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        assertNotNull(monitor.waitForActivityWithTimeout(5000)); instrumentation.removeMonitor(monitor);
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed(); prompt();
        screenshot("update-dialog-day-landscape.png");
        onView(withText(R.string.update_skip)).check(matches(isDisplayed()));
        onView(withText(R.string.update_download)).check(matches(isDisplayed()));
        onView(withText(R.string.update_later)).check(matches(isDisplayed())).perform(click());
        Activity current = resumed();
        monitor = instrumentation.addMonitor(SettingsActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(() -> current.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        assertNotNull(monitor.waitForActivityWithTimeout(5000)); instrumentation.removeMonitor(monitor);
        onView(withId(R.id.theme_night)).perform(scrollTo(), click());
        onView(withId(R.id.check_updates_button)).perform(scrollTo(), click()); completed(); prompt();
        screenshot("update-dialog-night-portrait.png");
        onView(withText(R.string.update_skip)).perform(click());
    }
    private void screenshot(String name) throws Exception {
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null), name))) {
            android.graphics.Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
            assertNotNull(bitmap); bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle();
        }
    }
}
