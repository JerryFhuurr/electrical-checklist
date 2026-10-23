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
import java.util.Collections;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BoeingMaxChecklistTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737 MAX Family")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }

    @Test public void catalogMatches8200SourceIncludingEltAndBelowLineDivider() throws Exception {
        ChecklistCatalog catalog = ChecklistCatalog.load(context);
        ChecklistCatalog.Aircraft max = catalog.aircraft.stream()
                .filter(a -> a.id.equals("ryanair-737-max")).findFirst().get();
        assertEquals("Boeing", max.manufacturer);
        assertEquals("Ryanair / 737-8200", max.operator);
        assertEquals(5, max.checklists.size());
        assertEquals(88, max.itemCount());
        int[] counts = {6, 39, 12, 23, 8};
        for (int i = 0; i < counts.length; i++) assertEquals(counts[i], max.checklists.get(i).items.size());
        ChecklistCatalog.Checklist start = max.checklists.get(1);
        assertEquals("PROBE COVERS", start.items.get(2).challenge);
        assertEquals("If applicable (**)", start.items.get(2).note);
        assertEquals("ELT", start.items.get(3).challenge);
        assertEquals("ARM", start.items.get(3).response);
        assertEquals("Through flights (*)", start.items.get(3).note);
        assertEquals("Below the line", start.items.get(35).group);
        assertEquals("IDLE DETENT", max.checklists.get(2).items.get(9).response);
        assertEquals("ELECTRIC HYDRAULIC PUMPS", max.checklists.get(3).items.get(6).challenge);
        assertEquals("*ON/AUTO", max.checklists.get(3).items.get(7).response);
        assertEquals("ELT", max.checklists.get(3).items.get(22).challenge);
        assertEquals("ELT", max.checklists.get(4).items.get(2).challenge);
        assertEquals("EMERGENCY EXIT LIGHTS", max.checklists.get(4).items.get(3).challenge);
        assertTrue(max.checklists.get(4).items.get(5).note.isEmpty());
        assertTrue(max.extendedChecklists.isEmpty());
        assertEquals(85, catalog.aircraft.get(0).itemCount());
        try (ReferenceSource source = ReferenceSource.openAsset(context, max.references.get(0))) {
            assertEquals(1, source.pageCount());
        }
    }

    @Test public void maxAdvancesSeparatelyFromNgAndFinalChecklistStaysOpen() {
        onView(withText("Safety Inspection")).perform(click());
        for (int i = 0; i < 6; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Before Start")));
        onView(withContentDescription("ELT, ARM")).perform(scrollTo()).check(matches(isNotChecked()));
        assertTrue(new ProgressStore(context).load().isComplete("ryanair-737-max", "safety-inspection"));
        assertFalse(new ProgressStore(context).load().isComplete("ryanair-737-ng", "safety-inspection"));
        pressBack();
        onView(withText("Secure")).perform(scrollTo(), click());
        for (int i = 0; i < 8; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Secure")));
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")));
        pressBack(); pressBack();
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Safety Inspection")).perform(click());
        onView(withContentDescription("BATTERY, ON")).check(matches(isNotChecked()));
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        assertTrue(new ProgressStore(context).load().completedSnapshot().isEmpty());
    }

    @Test public void original8200PdfReturnsToSameMaxChecklistAndChecks() {
        onView(withText("Before Start")).perform(click());
        onView(withContentDescription("ELT, ARM")).perform(scrollTo(), click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 1")));
            onView(withId(R.id.reader_aircraft)).check(matches(withText("Boeing 737 MAX Family")));
            onView(withId(R.id.reader_back)).perform(click());
            onView(withId(R.id.checklist_title)).check(matches(withText("Before Start")));
            onView(withContentDescription("ELT, ARM")).perform(scrollTo()).check(matches(isChecked()));
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
}
