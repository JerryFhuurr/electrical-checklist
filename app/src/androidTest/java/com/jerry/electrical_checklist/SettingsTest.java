package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.View;
import android.widget.TextView;
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
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SettingsTest {
    private Instrumentation instrumentation;
    private Context context;
    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        defaults(); start();
    }
    private void defaults() {
        instrumentation.runOnMainSync(() -> {
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
    }
    private void start() {
        instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @After public void close() {
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
        });
        defaults();
    }
    private void openSafety() {
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Safety Inspection")).perform(click());
    }
    private void settings() { onView(withId(R.id.settings_button)).perform(click()); }
    private void font(String value) {
        onView(withId(R.id.font_size_selector)).perform(click());
        onData(is(value)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
    }
    private Activity resumed() {
        AtomicReference<Activity> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).iterator().next()));
        return result.get();
    }
    private float textSize(String text) {
        AtomicReference<Float> size = new AtomicReference<>();
        onView(withText(text)).check((view,error) -> { if (error != null) throw error; size.set(((TextView)view).getTextSize()); });
        return size.get();
    }
    private void recreate() {
        Activity current = resumed();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(current.getClass().getName(), null, false);
        instrumentation.runOnMainSync(current::recreate);
        assertNotNull(monitor.waitForActivityWithTimeout(5000));
        instrumentation.removeMonitor(monitor);
    }
    private void assertMode(int expected) {
        assertEquals(expected, resumed().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }
    @Test public void fontPreviewAndSavedSizePreserveTicksAndResetKeepsSettings() {
        openSafety(); onView(withContentDescription("BATTERY, ON")).perform(click());
        float original = textSize("BATTERY");
        settings(); font("Largest (175%)");
        assertEquals(original * 1.75f, textSize("Landing gear — Down"), 0.1f);
        recreate(); onView(withId(R.id.font_size_selector)).check(matches(withSpinnerText("Largest (175%)")));
        pressBack(); onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        assertEquals(original * 1.75f, textSize("BATTERY"), 0.1f);
        recreate(); onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        assertEquals(original * 1.75f, textSize("BATTERY"), 0.1f);
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        onView(withContentDescription("BATTERY, ON")).check(matches(isNotChecked()));
        assertEquals(1.75f, new DisplaySettings(context).fontScale(), 0.001f);
        start(); openSafety(); assertEquals(original * 1.75f, textSize("BATTERY"), 0.1f);
    }
    @Test public void dayAndNightRetainChecklistAndApplyToReaderAndRestart() {
        openSafety(); onView(withContentDescription("BATTERY, ON")).perform(click());
        settings(); onView(withId(R.id.theme_day)).perform(scrollTo(), click());
        onView(withId(R.id.theme_day)).check(matches(isChecked())); assertMode(Configuration.UI_MODE_NIGHT_NO);
        pressBack(); onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked())); assertMode(Configuration.UI_MODE_NIGHT_NO);
        onView(withId(R.id.view_checklist_button)).perform(click());
        onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage()); assertMode(Configuration.UI_MODE_NIGHT_NO);
        pressBack(); settings(); onView(withId(R.id.theme_night)).perform(scrollTo(), click());
        onView(withId(R.id.theme_night)).check(matches(isChecked())); assertMode(Configuration.UI_MODE_NIGHT_YES);
        pressBack(); onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        assertMode(Configuration.UI_MODE_NIGHT_YES);
        settings(); onView(withId(R.id.theme_day)).perform(scrollTo(), click());
        onView(withId(R.id.theme_day)).check(matches(isChecked()));
        start(); assertMode(Configuration.UI_MODE_NIGHT_NO);
        settings(); onView(withId(R.id.theme_day)).check(matches(isChecked()));
        onView(withId(R.id.theme_system)).perform(scrollTo(), click());
        onView(withId(R.id.theme_system)).check(matches(isChecked()));
        assertEquals(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, new DisplaySettings(context).nightMode());
        recreate(); onView(withId(R.id.theme_system)).check(matches(isChecked()));
    }
    @Test public void largestFontWrapsLongResponsesAndKeepsSourceNotesInformational() {
        instrumentation.runOnMainSync(() -> new DisplaySettings(context).setFontChoice(4));
        start(); onView(withContentDescription("Choose aircraft from Airbus")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Airbus A350")).perform(scrollTo(), click());
        onView(withText("Before TOD")).perform(scrollTo(), click());
        onView(withText("IN FMS")).perform(scrollTo()).check(matches(not(isClickable())));
        onView(withContentDescription("DES PANEL OF THE FMS ACTIVE/PERF PAGE, INSERT/CHECK")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withContentDescription("DES PANEL OF THE FMS ACTIVE/PERF PAGE, INSERT/CHECK")).check((view,error) -> {
            if (error != null) throw error;
            TextView challenge = ((View)view.getParent()).findViewById(R.id.item_challenge);
            TextView response = ((View)view.getParent()).findViewById(R.id.item_response);
            assertTrue(challenge.getLineCount() > 1);
            for (TextView text : new TextView[]{challenge,response})
                for (int line = 0; line < text.getLineCount(); line++) assertEquals(0,text.getLayout().getEllipsisCount(line));
        });
    }
    @Test public void aboutShowsAuthorVersionAndOpensRepositoryLink() {
        settings(); onView(withId(R.id.about_button)).perform(scrollTo(), click());
        onView(withId(R.id.about_author)).check(matches(withText("Jerry Cao (JerryFhuurr)")));
        onView(withId(R.id.about_version)).check(matches(withText(startsWith("Version 1.18"))));
        AtomicReference<Intent> launched = new AtomicReference<>();
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor() {
            @Override public Instrumentation.ActivityResult onStartActivity(Intent intent) {
                if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                    launched.set(intent); return new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
                }
                return null;
            }
        };
        instrumentation.addMonitor(monitor);
        try { onView(withId(R.id.github_button)).perform(scrollTo(), click()); }
        finally { instrumentation.removeMonitor(monitor); }
        assertNotNull(launched.get());
        assertEquals("https://github.com/JerryFhuurr/electrical-checklist", launched.get().getDataString());
        recreate(); onView(withId(R.id.about_author)).check(matches(isDisplayed()));
        pressBack(); onView(withId(R.id.font_size_selector)).perform(scrollTo()).check(matches(isDisplayed()));
        pressBack(); onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo()).check(matches(isDisplayed()));
    }
}
