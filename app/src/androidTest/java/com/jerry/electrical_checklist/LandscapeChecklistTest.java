package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.View;
import android.widget.ScrollView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
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
public class LandscapeChecklistTest {
    private Instrumentation instrumentation;
    private Context context;

    @Before public void launch() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        ProgressStore store = new ProgressStore(context);
        store.save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        for (ChecklistCatalog.Aircraft model : ChecklistCatalog.load(context).aircraft) {
            java.util.Map<String, String> defaults = new java.util.LinkedHashMap<>();
            for (ChecklistCatalog.ChecklistOption option : model.options) defaults.put(option.id, option.defaultValue);
            store.saveOptions(model.id, defaults);
        }
        instrumentation.runOnMainSync(() -> {
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
        instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
        instrumentation.removeMonitor(monitor);
        instrumentation.waitForIdleSync();
        assertEquals(desired, resumed().getResources().getConfiguration().orientation);
    }

    @After public void close() {
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
    }

    private void open(String manufacturer, String model) {
        onView(withContentDescription("Choose aircraft from " + manufacturer)).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for " + model)).perform(scrollTo(), click());
    }
    private void phase(String id) { onView(allOf(withTagValue(is((Object) id)), isDescendantOfA(withId(R.id.checklist_sidebar)))).perform(scrollTo(), click()); }
    private void title(String text) { onView(withId(R.id.checklist_title)).check(matches(withText(text))); }
    private void selected(String id) { onView(allOf(withTagValue(is((Object) id)), isDescendantOfA(withId(R.id.sidebar_phase_rows)))).check(matches(isSelected())); }

    @Test public void switchingPhasesAndRotationPreserveChecksAndSelection() {
        open("Boeing", "Boeing 737-NG");
        onView(withText("Select a checklist")).check(matches(isDisplayed()));
        onView(withId(R.id.checklist_workspace)).check((view, error) -> {
            if (error != null) throw error;
            View left = view.findViewById(R.id.checklist_sidebar), right = view.findViewById(R.id.checklist_detail_container);
            assertTrue(left.getWidth() > 0); assertTrue(right.getLeft() >= left.getRight());
            assertTrue(right.getWidth() > left.getWidth());
            assertEquals(((View) view.getParent()).getWidth(), view.getWidth());
        });
        phase("safety-inspection"); title("Safety Inspection"); selected("safety-inspection");
        onView(withContentDescription("BATTERY, ON")).perform(click());
        phase("before-start"); title("Before Start"); selected("before-start");
        phase("safety-inspection");
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withId(R.id.checklist_workspace)).check(doesNotExist());
        title("Safety Inspection"); onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        selected("safety-inspection"); onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        pressBack(); onView(withText("Select a checklist")).check(matches(isDisplayed()));
        pressBack(); onView(withId(R.id.aircraft_title)).check(matches(withText("Boeing")));
    }

    @Test public void advancementAndIndependentScrollingKeepBothPanesUsable() {
        open("Boeing", "Boeing 737-NG"); phase("safety-inspection");
        for (int i = 0; i < 6; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        title("Before Start"); selected("before-start");
        onView(withTagValue(is((Object) "safety-inspection"))).check(matches(withText("✓ Safety Inspection")));
        for (int i = 0; i < 20; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.page_scroll)).perform(awaitScrollIdle());
        int[] positions = new int[2];
        instrumentation.runOnMainSync(() -> {
            ScrollView items = resumedMainView(R.id.page_scroll), navigation = resumedMainView(R.id.sidebar_scroll);
            positions[0] = items.getScrollY(); positions[1] = navigation.getScrollY();
            assertTrue(positions[0] > 0);
            navigation.scrollTo(0, navigation.getChildAt(0).getHeight());
        });
        instrumentation.waitForIdleSync();
        onView(withId(R.id.page_scroll)).check((view,error) -> assertEquals(positions[0], view.getScrollY()));
        onView(withId(R.id.sidebar_scroll)).check((view,error) -> assertTrue(view.getScrollY() > positions[1]));
        phase("secure"); title("Secure");
        for (int i = 0; i < 7; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        title("Secure"); selected("secure");
        assertTrue(new ProgressStore(context).load().isComplete("ryanair-737-ng", "secure"));
        onView(withTagValue(is((Object) "secure"))).check(matches(withText("✓ Secure")));
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        title("Secure"); selected("secure");
        onView(withTagValue(is((Object) "secure"))).check(matches(withText("Secure")));
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
    }

    private static ViewAction awaitScrollIdle() {
        return new ViewAction() {
            @Override public org.hamcrest.Matcher<View> getConstraints() { return isAssignableFrom(ScrollView.class); }
            @Override public String getDescription() { return "wait for checklist scrolling to settle"; }
            @Override public void perform(UiController controller, View view) {
                int previous = -1, stable = 0;
                for (int elapsed = 0; elapsed < 2000; elapsed += 32) {
                    controller.loopMainThreadForAtLeast(32);
                    int position = view.getScrollY();
                    stable = position == previous ? stable + 1 : 0;
                    if (stable >= 3) return;
                    previous = position;
                }
                throw new AssertionError("Checklist scrolling did not settle");
            }
        };
    }

    @SuppressWarnings("unchecked") private <T extends View> T resumedMainView(int id) {
        for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
            return (T) activity.findViewById(id);
        throw new AssertionError("No resumed activity");
    }

    @Test public void a340CategoriesOptionsAndSourceReaderWorkFromSidebar() {
        open("Airbus", "Airbus A340-300");
        phase("memory-items");
        phase("memory-tcas-warning-resolution-advisory"); title("TCAS WARNING – RESOLUTION ADVISORY");
        onView(withId(R.id.checklist_options_button)).perform(scrollTo(), click());
        onView(withText("AP/FD TCAS mode")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
        onView(withId(R.id.view_checklist_button)).perform(click());
        onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
        onView(withId(R.id.reader_page_count)).check(matches(withText("Page 8 / 10")));
        onView(withId(R.id.reader_back)).perform(click());
        selected("memory-tcas-warning-resolution-advisory");
        phase("normal-checklists"); phase("before-start"); title("BEFORE START");
        onView(withId(R.id.checklist_options_button)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        phase("emergency-evacuation"); title("EMERGENCY EVACUATION"); selected("evacuation");
        onView(withId(R.id.checklist_options_button)).perform(scrollTo(), click());
        onView(withText("MAN CAB PR used")).check(matches(isDisplayed()));
        onView(withText("Cancel")).perform(click());
        pressBack(); phase("other-procedures"); phase("procedure-takeoff-trim");
        onView(withId(R.id.view_checklist_button)).perform(click());
        onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
        onView(withId(R.id.reader_page_count)).check(matches(withText("Page 2 / 10")));
        onView(withId(R.id.reader_back)).perform(click());
    }

    @Test public void extendedModeAndLargeTextRemainAvailableAfterThemeChange() {
        open("Airbus", "Airbus A320 Family");
        onView(withId(R.id.checklist_mode_button)).perform(scrollTo(), click());
        onView(withId(R.id.extended_reference_button)).perform(scrollTo(), click());
        onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
        onView(withId(R.id.reader_page_count)).check(matches(withText("Page 4 / 4")));
        onView(withId(R.id.reader_back)).perform(click());
        onView(withId(R.id.checklist_mode_button)).perform(scrollTo(), click());
        onView(allOf(withText("Cockpit Preparation"), isDescendantOfA(withId(R.id.sidebar_phase_rows)))).perform(scrollTo(), click());
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(R.id.font_size_selector)).perform(click());
        onData(is("Largest (175%)")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
        onView(withId(R.id.theme_day)).perform(scrollTo(), click());
        pressBack(); title("Cockpit Preparation");
        onView(withId(R.id.checklist_action)).check(matches(isCompletelyDisplayed()));
        onView(withId(R.id.checklist_workspace)).check(matches(isDisplayed()));
        onView(withText("ADIRS")).check((view,error) -> {
            if (error != null) throw error;
            assertEquals(12 * 1.75f * view.getResources().getDisplayMetrics().scaledDensity,
                    ((android.widget.TextView) view).getTextSize(), 0.1f);
        });
        assertEquals(Configuration.UI_MODE_NIGHT_NO, resumed().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK);
        onView(withContentDescription("ADIRS, NAV")).perform(scrollTo(), click());
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withContentDescription("ADIRS, NAV")).check(matches(isChecked()));
    }
}
