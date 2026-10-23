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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Md11ChecklistTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    private static final String MODEL = "mcdonnell-douglas-md11";
    private static final String HYD = "1-3, 2-3 RMP's, On, manual only, optional";
    private static final String PACKS = "Packs (all), Off, manual only, optional";
    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from McDonnell Douglas")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for MD-11")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private void open(String title) { onView(withText(title)).perform(scrollTo(), click()); }

    @Test public void catalogMatchesSourceAndOnlyGreyRowsAreOptional() throws Exception {
        ChecklistCatalog catalog = ChecklistCatalog.load(context);
        assertEquals(1, catalog.aircraftFor("McDonnell Douglas").size());
        ChecklistCatalog.Aircraft md = catalog.aircraftFor("McDonnell Douglas").get(0);
        assertEquals(MODEL, md.id);
        assertEquals(9, md.checklists.size());
        assertEquals(97, md.itemCount());
        int required = 0;
        int[] requiredCounts = {16, 3, 11, 6, 6, 4, 5, 8, 12};
        int[] optionalCounts = {6, 7, 2, 6, 3, 0, 0, 1, 1};
        for (int i = 0; i < 9; i++) {
            ChecklistCatalog.Checklist list = md.checklists.get(i);
            assertEquals(requiredCounts[i], list.requiredItems.size());
            assertEquals(optionalCounts[i], list.items.size() - list.requiredItems.size());
            required += list.requiredItems.size();
        }
        assertEquals(71, required);
        ChecklistCatalog.Checklist takeoff = md.checklists.get(2);
        assertTrue(takeoff.items.get(6).manualOnly);
        assertEquals("Below the line / Hyd Manual", takeoff.items.get(6).group);
        assertTrue(takeoff.items.get(7).manualOnly);
        assertFalse(takeoff.items.get(5).manualOnly); // '+' alone means complete silently.
        assertTrue(takeoff.items.get(5).note.contains("Complete silently"));
        assertEquals("F: ___\nC: Checked", takeoff.items.get(0).response);
        assertTrue(md.checklists.get(0).items.get(2).note.contains("Amplified procedure"));
        for (ChecklistCatalog.Aircraft a : catalog.aircraft) if (!a.id.equals(MODEL))
            for (ChecklistCatalog.Checklist c : a.checklists)
                assertEquals(c.items.size(), c.requiredItems.size());
        try (ReferenceSource source = ReferenceSource.openAsset(context, md.references.get(0))) {
            assertEquals(2, source.pageCount());
        }
    }

    @Test public void manualRowsAreClickableButButtonSkipsThemAndCompletionIgnoresThem() {
        open("Before Takeoff");
        onView(withContentDescription(HYD)).perform(scrollTo(), click()).check(matches(isChecked()));
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 11 required checked"))));
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("1 / 2 manual only"))));
        onView(withContentDescription(HYD)).perform(click()).check(matches(isNotChecked()));
        for (int i = 0; i < 11; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete")));
        onView(withContentDescription(HYD)).perform(scrollTo()).check(matches(isNotChecked()));
        onView(withContentDescription(PACKS)).perform(scrollTo()).check(matches(isNotChecked()));
        assertEquals(11, new ProgressStore(context).load().checkedSnapshot().size());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("After Takeoff")));
        assertTrue(new ProgressStore(context).load().isComplete(MODEL, "before-takeoff"));
    }

    @Test public void optionalTicksSurviveRotationAndDoNotReopenCompletedChecklist() {
        open("Before Takeoff");
        onView(withContentDescription(HYD)).perform(scrollTo(), click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withContentDescription(HYD)).perform(scrollTo()).check(matches(isChecked()));
        for (int i = 0; i < 11; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        pressBack(); open("Before Takeoff");
        onView(withContentDescription(HYD)).perform(scrollTo(), click());
        assertTrue(new ProgressStore(context).load().isComplete(MODEL, "before-takeoff"));
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")))
                .check(matches(not(isEnabled())));
        onView(withContentDescription("Flaps, F: ___\nC: Checked")).perform(scrollTo(), click());
        assertFalse(new ProgressStore(context).load().isComplete(MODEL, "before-takeoff"));
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark item as complete")));
    }

    @Test public void allPhasesCompleteWithEveryManualRowUncheckedAndLastStaysOpen() throws Exception {
        ChecklistCatalog.Aircraft md = ChecklistCatalog.load(context).aircraftFor("McDonnell Douglas").get(0);
        open("Before Start");
        for (ChecklistCatalog.Checklist c : md.checklists) {
            onView(withId(R.id.checklist_title)).check(matches(withText(c.name)));
            for (int i = 0; i < c.requiredItems.size(); i++) onView(withId(R.id.checklist_action)).perform(click());
            onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        }
        onView(withId(R.id.checklist_title)).check(matches(withText("Shut Down")));
        onView(withId(R.id.checklist_action)).check(matches(not(isEnabled())));
        ChecklistProgress saved = new ProgressStore(context).load();
        assertEquals(71, saved.checkedSnapshot().size());
        assertEquals(9, saved.completedSnapshot().size());
        for (ChecklistCatalog.Checklist c : md.checklists)
            for (ChecklistCatalog.Item r : c.items) if (r.manualOnly) assertFalse(saved.isChecked(MODEL, c.id, r.id));
    }

    @Test public void originalPdfAndResetPreserveThenClearBothKindsOfTicks() {
        open("Before Takeoff");
        onView(withContentDescription(PACKS)).perform(scrollTo(), click());
        onView(withContentDescription("Spoilers, Armed")).perform(scrollTo(), click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 2")));
            onView(withId(R.id.reader_next)).perform(click());
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 2 / 2")));
            onView(withId(R.id.reader_back)).perform(click());
            onView(withId(R.id.checklist_title)).check(matches(withText("Before Takeoff")));
            onView(withContentDescription(PACKS)).perform(scrollTo()).check(matches(isChecked()));
            onView(withContentDescription("Spoilers, Armed")).perform(scrollTo()).check(matches(isChecked()));
            onView(withId(R.id.reset_button)).perform(click());
            onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
            assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
            onView(withContentDescription(PACKS)).perform(scrollTo()).check(matches(isNotChecked()));
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
}
