package com.jerry.electrical_checklist;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class A350ChecklistTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    private ChecklistCatalog.Aircraft model;
    @Before public void launch() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation(); context = instrumentation.getTargetContext();
        new NavigationPreferences(context).setStartupMode(NavigationPreferences.MANUFACTURERS);
        model = ChecklistCatalog.load(context).aircraft.stream().filter(a -> a.id.equals("ini-a350")).findFirst().get();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        new ProgressStore(context).saveOptions(model.id, Collections.singletonMap("evac-man-cab", "used"));
        start();
    }
    private void start() {
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from Airbus")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Airbus A350")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private void open(String title) { onView(withText(title)).perform(scrollTo(), click()); }
    private void check(int count) { for (int i=0; i<count; i++) onView(withId(R.id.checklist_action)).perform(click()); }
    private void categories() { onView(withId(R.id.checklist_categories_button)).perform(click()); }
    @Test public void normalCatalogMatchesReplacementAndKeepsExtrasSeparate() throws Exception {
        assertEquals("Iberia Virtual", model.operator);
        assertEquals(8, model.checklists.size()); assertEquals(58, model.itemCount());
        assertEquals(Arrays.asList("Before Start", "After Start", "Before Takeoff", "After Takeoff", "Approach", "Landing", "Parking", "Securing the Aircraft"),
                model.checklists.stream().map(c -> c.name).collect(java.util.stream.Collectors.toList()));
        assertEquals(2, model.extraSets.size());
        assertEquals(8, model.extraSet("emergency-evacuation").checklists.get(0).items.size());
        assertTrue(model.extraSet("rejected-takeoff").checklists.get(0).items.isEmpty());
        try (ReferenceSource source = ReferenceSource.openAsset(context, model.references.get(0))) { assertEquals(2, source.pageCount()); }
        onView(withText("Before TOD")).check(doesNotExist());
        onView(withText("PRE FLIGHT")).check(doesNotExist());
    }
    @Test public void normalCompletionAdvancesAndFinalChecklistStaysOpen() {
        open("Before Start"); check(12);
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("After Start")));
        categories(); open("Securing the Aircraft"); check(6);
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Securing the Aircraft")));
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")));
    }
    @Test public void conditionalWeatherItemDoesNotBlockParkingAndNotesStayInformational() {
        open("Parking"); check(10);
        onView(withText("Consider HEAVY RAIN")).perform(scrollTo()).check(matches(not(isClickable())));
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        assertTrue(new ProgressStore(context).load().isComplete(model.id, "normal-parking"));
        categories(); open("Landing");
        onView(withText("L/G DOWN\nSIGNS ON\nSPLRS ARM\nFLAPS SET")).perform(scrollTo()).check(matches(not(isClickable())));
        assertEquals(4, model.checklists.get(5).items.size());
    }
    @Test public void evacuationOptionControlsPressureCheckAndStaysOnProcedure() {
        open("Emergency evacuation");
        onView(withContentDescription("ΔP (only if MAN CAB PR has been used), CHECK ZERO")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.checklist_options_button)).perform(click());
        onView(withContentDescription("MAN CAB PR used")).perform(click());
        onData(is("Not used")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click()); onView(withText("Apply")).perform(click());
        onView(withContentDescription("ΔP (only if MAN CAB PR has been used), CHECK ZERO")).check(doesNotExist());
        check(7); onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("On Ground Emergency Evacuation")));
        categories(); open("Rejected takeoff guidance");
        onView(withTagValue(is((Object) "rto-2-capt"))).perform(scrollTo()).check(matches(not(isClickable())));
        onView(withId(R.id.item_checkbox)).check(doesNotExist());
        onView(withId(R.id.checklist_action)).check(matches(not(isEnabled())));
    }
    @Test public void replacementDoesNotReuseOldSopCompletionOrChangeOtherAircraftProgress() {
        ChecklistProgress saved = new ProgressStore(context).load();
        saved.setChecked(model.id, "before-start", "parking-brake", true);
        saved.complete(model.id, "before-start", Collections.singleton("parking-brake"));
        saved.setChecked("ryanair-737-ng", "safety-inspection", "battery", true);
        new ProgressStore(context).save(saved); start(); open("Before Start");
        onView(withContentDescription("PARKING BRAKE, SET")).check(matches(isNotChecked()));
        assertFalse(new ProgressStore(context).load().isComplete(model.id, "normal-before-start"));
        assertTrue(new ProgressStore(context).load().isChecked("ryanair-737-ng", "safety-inspection", "battery"));
    }
    @Test public void sourceReaderShowsNormalAndEmergencyPagesAndReturnsToChecklist() {
        open("Before Start"); reader(1); categories(); open("Emergency evacuation"); reader(2);
    }
    private void reader(int page) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor); assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page " + page + " / 2")));
            onView(withId(R.id.reader_back)).perform(click());
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
}
