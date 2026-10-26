package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.ParcelFileDescriptor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ToolbarTitleTest {
    private Context context;
    private Instrumentation instrumentation;
    private ChecklistCatalog catalog;

    @Before public void launch() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        catalog = ChecklistCatalog.load(context);
        new NavigationPreferences(context).setStartupMode(NavigationPreferences.MANUFACTURERS);
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        instrumentation.runOnMainSync(() -> {
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
        instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        instrumentation.waitForIdleSync();
    }

    private Activity resumed() {
        AtomicReference<Activity> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).iterator().next()));
        return result.get();
    }

    private void rotate(int orientation) {
        Activity current = resumed();
        int desired = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        if (current.getResources().getConfiguration().orientation == desired) return;
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(current.getClass().getName(), null, false);
        instrumentation.runOnMainSync(() -> current.setRequestedOrientation(orientation));
        assertNotNull(monitor.waitForActivityWithTimeout(5000));
        instrumentation.removeMonitor(monitor); instrumentation.waitForIdleSync();
    }

    @After public void close() {
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
        new NavigationPreferences(context).setStartupMode(NavigationPreferences.MANUFACTURERS);
    }

    private void open(String manufacturer, String aircraft) {
        onView(withContentDescription("Choose aircraft from " + manufacturer)).perform(scrollTo(), click());
        title("E-Checklist");
        onView(withContentDescription("Open checklists for " + aircraft)).perform(scrollTo(), click());
        title(aircraft);
    }

    private void phase(String id) {
        onView(allOf(withTagValue(is((Object) id)), isDescendantOfA(withId(R.id.phase_rows))))
                .perform(scrollTo(), click());
    }

    private void title(String expected) {
        onView(withId(R.id.brand_label)).check(matches(withText(expected))).check((view, error) -> {
            if (error != null) throw error;
            TextView text = (TextView) view;
            assertNotNull(text.getLayout());
            assertTrue("Toolbar must have usable width", text.getWidth() > 0);
            for (int i = 0; i < text.getLayout().getLineCount(); i++)
                assertEquals("Aircraft name must not be truncated: " + expected, 0, text.getLayout().getEllipsisCount(i));
        });
    }

    @Test public void everyAircraftShowsItsModelInPickerAndChecklistAndHomeRestoresBrand() {
        title("E-Checklist");
        for (ChecklistCatalog.Aircraft model : catalog.aircraft) {
            open(model.manufacturer, model.name);
            phase(model.checklists.get(0).id); title(model.name);
            pressBack(); title(model.name);
            pressBack(); title("E-Checklist");
            onView(withId(R.id.home_button)).perform(click()); title("E-Checklist");
        }
    }

    @Test public void engineStartTitleSurvivesSettingsReaderRotationAndRecreation() {
        open("Boeing", "Boeing 757"); phase("engine-start"); title("Boeing 757");
        onView(withId(R.id.checklist_action)).perform(click());
        java.util.Set<String> checks = new ProgressStore(context).load().checkedSnapshot();
        assertEquals(1, checks.size());
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.font_size_selector)).perform(scrollTo(), click());
        onData(is("Largest (175%)")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
        pressBack(); title("Boeing 757");
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        title("Boeing 757"); onView(withId(R.id.checklist_workspace)).check(matches(isDisplayed()));
        onView(withId(R.id.view_checklist_button)).perform(click());
        onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
        onView(withId(R.id.reader_back)).perform(click()); title("Boeing 757");
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); title("Boeing 757");
        Activity current = resumed();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(current::recreate);
        assertNotNull(monitor.waitForActivityWithTimeout(5000)); instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync(); title("Boeing 757");
        assertEquals(checks, new ProgressStore(context).load().checkedSnapshot());
    }

    @Test public void shortcutPagesUseBrandAndSelectedAircraftUsesModel() {
        NavigationPreferences navigation = new NavigationPreferences(context);
        navigation.setFavorite("ryanair-737-max", true);
        onView(withId(R.id.open_favorites_button)).perform(scrollTo(), click()); title("E-Checklist");
        onView(withContentDescription("Open Boeing 737 MAX Family checklists")).perform(scrollTo(), click());
        title("Boeing 737 MAX Family"); phase("safety-inspection"); title("Boeing 737 MAX Family");
        pressBack(); pressBack(); title("E-Checklist");
        onView(withId(R.id.home_button)).perform(click());
        onView(withId(R.id.open_recent_button)).perform(scrollTo(), click()); title("E-Checklist");
        onView(withContentDescription("Open Boeing 737 MAX Family checklists")).perform(scrollTo(), click());
        title("Boeing 737 MAX Family");
        onView(withId(R.id.home_button)).perform(click()); title("E-Checklist");
        navigation.setFavorite("ryanair-737-max", false);
    }

    private String shell(String command) throws Exception {
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.getUiAutomation().executeShellCommand(command));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            return output.toString("UTF-8");
        }
    }

    private void awaitWidth(int expected) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            AtomicInteger width = new AtomicInteger();
            instrumentation.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                    if (activity instanceof MainActivity) width.set(activity.getResources().getConfiguration().screenWidthDp);
            });
            if (width.get() == expected) { instrumentation.waitForIdleSync(); return; }
            Thread.sleep(50);
        }
        fail("Display did not settle at " + expected + "dp");
    }

    @Test public void narrowPhoneKeepsLongAircraftNamesCompleteAndControlsAvailable() throws Exception {
        int originalWidth = resumed().getResources().getConfiguration().screenWidthDp;
        String size = shell("wm size"), density = shell("wm density");
        String oldSize = size.contains("Override size:") ? size.split("Override size:")[1].trim() : "reset";
        String oldDensity = density.contains("Override density:") ? density.split("Override density:")[1].trim() : "reset";
        try {
            shell("wm size 960x1920"); shell("wm density 480"); awaitWidth(320);
            open("Boeing", "Boeing 737 MAX Family"); phase("safety-inspection"); title("Boeing 737 MAX Family");
            onView(withId(R.id.home_button)).check(matches(isDisplayed())).perform(click());
            open("Airbus", "Airbus A320 Family"); phase("cockpit-preparation"); title("Airbus A320 Family");
            onView(withId(R.id.settings_button)).check(matches(isDisplayed()));
            onView(withId(R.id.reset_button)).check(matches(isDisplayed()));
        } finally {
            shell("wm size " + oldSize); shell("wm density " + oldDensity); awaitWidth(originalWidth);
        }
    }
}
