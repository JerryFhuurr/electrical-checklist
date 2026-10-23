package com.jerry.electrical_checklist;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.Collections;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ManufacturerNavigationTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;

    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private void manufacturer(String name) {
        onView(withContentDescription("Choose aircraft from " + name)).perform(scrollTo(), click());
    }
    private void model(String name) {
        onView(withContentDescription("Open checklists for " + name)).perform(scrollTo(), click());
    }
    private void recreate() {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
    }

    @Test public void manufacturerSelectionFiltersModelsAndBackReturnsThroughEachLevel() throws Exception {
        ChecklistCatalog catalog = ChecklistCatalog.load(context);
        assertEquals(Arrays.asList("Boeing", "Airbus", "McDonnell Douglas", "ATR"), catalog.manufacturers);
        assertEquals(5, catalog.aircraftFor("Boeing").size());
        assertEquals(4, catalog.aircraftFor("Airbus").size());
        onView(withId(R.id.aircraft_rows)).check(doesNotExist());
        onView(withId(R.id.back_button)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        manufacturer("Airbus");
        onView(withId(R.id.aircraft_title)).check(matches(withText("Airbus")));
        onView(withContentDescription("Open checklists for Boeing 737-NG")).check(doesNotExist());
        onView(withContentDescription("Open checklists for Airbus A320 Family")).check(matches(isDisplayed()));
        onView(withContentDescription("Open checklists for Airbus A350")).perform(scrollTo()).check(matches(isDisplayed()));
        model("Airbus A350");
        onView(withText("Before TOD")).perform(scrollTo(), click());
        onView(withContentDescription("Back to checklists")).perform(click());
        onView(withId(R.id.model_title)).check(matches(withText("Airbus A350")));
        onView(withContentDescription("Back to aircraft")).perform(click());
        onView(withId(R.id.aircraft_title)).check(matches(withText("Airbus")));
        onView(withContentDescription("Back to manufacturers")).perform(click());
        manufacturer("Boeing");
        onView(withId(R.id.aircraft_title)).check(matches(withText("Boeing")));
        onView(withContentDescription("Open checklists for Airbus A350")).check(doesNotExist());
        model("Boeing 737-NG");
        onView(withId(R.id.model_title)).check(matches(withText("Boeing 737-NG")));
    }

    @Test public void rotationRestoresManufacturerAndBackFromRestoredChecklistKeepsItsGroup() {
        recreate();
        onView(withId(R.id.manufacturer_rows)).check(matches(isDisplayed()));
        manufacturer("Airbus");
        recreate();
        onView(withId(R.id.aircraft_title)).check(matches(withText("Airbus")));
        onView(withContentDescription("Open checklists for Boeing 737-NG")).check(doesNotExist());
        model("Airbus A320 Family");
        onView(withText("Cockpit Preparation")).perform(click());
        recreate();
        onView(withId(R.id.checklist_title)).check(matches(withText("Cockpit Preparation")));
        pressBack(); pressBack();
        onView(withId(R.id.aircraft_title)).check(matches(withText("Airbus")));
        pressBack();
        onView(withId(R.id.manufacturer_rows)).check(matches(isDisplayed()));
    }

    @Test public void switchingManufacturersPreservesChecksAndHomeResetClearsEveryGroup() {
        manufacturer("Boeing"); model("Boeing 737-NG");
        onView(withText("Safety Inspection")).perform(click());
        onView(withContentDescription("BATTERY, ON")).perform(click());
        pressBack(); pressBack(); pressBack();
        manufacturer("Airbus"); model("Airbus A320 Family");
        onView(withText("Cockpit Preparation")).perform(click());
        onView(withContentDescription("ADIRS, NAV")).perform(click());
        pressBack(); pressBack(); pressBack();
        manufacturer("Boeing"); model("Boeing 737-NG");
        onView(withText("Safety Inspection")).perform(click());
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        assertEquals(2, new ProgressStore(context).load().checkedSnapshot().size());
        pressBack(); pressBack(); pressBack();
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        onView(withId(R.id.manufacturer_rows)).check(matches(isDisplayed()));
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
    }
}
