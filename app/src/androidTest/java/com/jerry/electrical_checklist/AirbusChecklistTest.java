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
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AirbusChecklistTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from Airbus")).perform(scrollTo(), click());
        openAircraft();
    }
    private void openAircraft() {
        onView(withContentDescription("Open checklists for Airbus A320 Family")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }

    @Test public void catalogContainsNormalAndExtendedItemsWithDistinctIdsAndFourPageReference() throws Exception {
        ChecklistCatalog.Aircraft model = ChecklistCatalog.load(context).aircraft.get(1);
        assertEquals("airbus-a320-family", model.id);
        assertEquals(11, model.checklists.size());
        assertEquals(69, model.itemCount());
        assertEquals(22, model.extendedChecklists.size());
        int count = 0;
        for (ChecklistCatalog.Checklist list : model.extendedChecklists) {
            assertTrue(list.id.startsWith("extended-")); count += list.items.size();
        }
        assertEquals(252, count);
        assertEquals(16, model.extendedChecklists.get(7).items.size()); // Taxi continues onto page 3.
        assertEquals("Continued (page 3)", model.extendedChecklists.get(7).items.get(5).group);
        assertTrue(model.legend.contains("Pilot Monitoring"));
        assertFalse(model.legend.contains("Through flights"));
        try (ReferenceSource source = ReferenceSource.openAsset(context, model.references.get(0))) {
            assertEquals(4, source.pageCount());
        }
    }

    @Test public void normalIsDefaultAndReenteringAircraftReturnsToNormal() {
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Normal checklist - page 1")));
        onView(withText("Flight Deck Preparation")).check(doesNotExist());
        onView(withText("Cockpit Preparation")).perform(click());
        onView(withContentDescription("Gear PINS and COVERS, REMOVED")).check(matches(isDisplayed()));
        onView(withContentDescription("Battery Master 1+2, CHECK & ON 1-2")).check(doesNotExist());
        onView(withId(R.id.checklist_mode_button)).perform(click());
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Extended items - pages 2-4")));
        onView(withText("Flight Deck Preparation")).check(matches(isDisplayed()));
        androidx.test.espresso.Espresso.pressBack();
        openAircraft();
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Normal checklist - page 1")));
    }

    @Test public void normalAndExtendedProgressStayIndependentAndGlobalResetClearsBoth() {
        onView(withText("Cockpit Preparation")).perform(click());
        onView(withContentDescription("ADIRS, NAV")).perform(click());
        onView(withId(R.id.checklist_mode_button)).perform(click());
        onView(withText("Cockpit Preparation")).perform(scrollTo(), click());
        onView(withContentDescription("Battery Master 1+2, CHECK & ON 1-2")).check(matches(isNotChecked())).perform(click());
        onView(withContentDescription("ADIRS, ALIGN 1-2-3")).check(matches(isNotChecked()));
        onView(withId(R.id.checklist_mode_button)).perform(click());
        onView(withText("Cockpit Preparation")).perform(click());
        onView(withContentDescription("ADIRS, NAV")).check(matches(isChecked()));
        ChecklistProgress saved = new ProgressStore(context).load();
        assertTrue(saved.isChecked("airbus-a320-family", "cockpit-preparation", "adirs"));
        assertTrue(saved.isChecked("airbus-a320-family", "extended-cockpit-preparation", "battery-master-1-2"));
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
    }

    @Test public void extendedCompletionAdvancesWithinExtendedAndRotationRestoresMode() {
        onView(withId(R.id.checklist_mode_button)).perform(click());
        onView(withText("Flight Deck Preparation")).perform(click());
        for (int i = 0; i < 9; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Cockpit Preparation")));
        onView(withContentDescription("Battery Master 1+2, CHECK & ON 1-2")).check(matches(isDisplayed()));
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withId(R.id.checklist_mode_button)).check(matches(withText("Show normal checklist")));
        onView(withContentDescription("Battery Master 1+2, CHECK & ON 1-2")).check(matches(isDisplayed()));
        assertTrue(new ProgressStore(context).load().isComplete("airbus-a320-family", "extended-flight-deck-preparation"));
        assertFalse(new ProgressStore(context).load().isComplete("airbus-a320-family", "cockpit-preparation"));
    }

    private void assertReaderPage(int expected, boolean navigateToEnd) {
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(scrollTo(), click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page " + expected + " / 4")));
            if (navigateToEnd) {
                for (int p = expected + 1; p <= 4; p++) {
                    onView(withId(R.id.reader_next)).perform(click());
                    onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
                    onView(withId(R.id.reader_page_count)).check(matches(withText("Page " + p + " / 4")));
                }
                onView(withId(R.id.reader_next)).check(matches(not(isEnabled())));
            }
            onView(withId(R.id.reader_back)).perform(click());
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }

    @Test public void readerOpensPageOneForNormalAndPageTwoForExtendedAndNavigatesAllPages() {
        assertReaderPage(1, false);
        onView(withId(R.id.checklist_mode_button)).perform(click());
        assertReaderPage(2, true);
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Extended items - pages 2-4")));
    }
}
