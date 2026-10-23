package com.jerry.electrical_checklist;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class A380ChecklistTest {
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
        onView(withContentDescription("Open checklists for Airbus A380")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private void open(String title) { onView(withText(title)).perform(scrollTo(), click()); }

    @Test public void catalogIncludesAllThreePagesFourEngineChecksAndFormattedNotes() throws Exception {
        ChecklistCatalog.Aircraft model = ChecklistCatalog.load(context).aircraft.stream()
                .filter(a -> a.id.equals("fbw-a380x")).findFirst().get();
        assertEquals("Airbus", model.manufacturer);
        assertEquals(31, model.checklists.size());
        assertEquals(269, model.itemCount());
        int[] counts = {0,9,4,12,6,14,4,5,7,0,13,9,7,12,8,19,12,7,11,8,3,10,8,13,4,13,10,13,4,13,11};
        int textRows = 0;
        for (int i = 0; i < counts.length; i++) {
            ChecklistCatalog.Checklist list = model.checklists.get(i);
            assertEquals(counts[i], list.items.size());
            assertEquals(list.items.size(), list.requiredItems.size());
            for (ChecklistCatalog.ChecklistRow row : list.rows) if (row.item == null) textRows++;
        }
        assertEquals(30, textRows);
        assertEquals("ENGINE 1/2/3/4 FIRE", model.checklists.get(3).items.get(6).challenge);
        assertEquals("START ENGINES 3 and 4", model.checklists.get(12).items.get(4).challenge);
        assertEquals("ENGINE MASTER 3 then 4", model.checklists.get(12).items.get(5).challenge);
        assertEquals("before-takeoff-checklist-taxi-down-to-the-line", model.checklists.get(15).items.get(18).id);
        assertEquals("COMPLETE", model.checklists.get(16).items.get(11).response);
        ChecklistCatalog.ChecklistRow hotWeather = model.checklists.get(3).rows.get(12);
        assertTrue(hotWeather.italic);
        assertEquals("end", hotWeather.alignment);
        ChecklistCatalog.ChecklistRow flare = model.checklists.get(26).rows.get(11);
        assertEquals("The A380 should be flared at ~40ft agl", flare.text);
        assertTrue(flare.italic);
        assertEquals("end", flare.alignment);
        assertTrue(model.extendedChecklists.isEmpty());
        try (ReferenceSource source = ReferenceSource.openAsset(context, model.references.get(0))) {
            assertEquals(3, source.pageCount());
        }
    }

    @Test public void textOnlySectionsContinueAndInFmsHeadingNeverCountsAsAnItem() {
        open("PRE FLIGHT");
        onView(withId(R.id.item_checkbox)).check(doesNotExist());
        onView(withId(R.id.checklist_action)).check(matches(withText("Continue to next checklist"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Preliminary Cockpit Preparation")));
        pressBack(); open("Initialise FMS");
        onView(withId(R.id.item_checkbox)).check(doesNotExist());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Glareshield & Lateral Consoles")));
        pressBack(); open("Before TOD");
        onView(withText("IN FMS")).perform(scrollTo(), click());
        instrumentation.runOnMainSync(() -> {
            LinearLayout rows = activity.findViewById(R.id.item_rows);
            View heading = rows.getChildAt(4);
            assertTrue(heading instanceof TextView);
            assertTrue(((TextView) heading).getTypeface().isBold());
            assertFalse(heading.isClickable());
            assertTrue(heading.isAccessibilityHeading());
        });
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        for (int i = 0; i < 10; i++) onView(withId(R.id.checklist_action)).perform(click());
        assertEquals(10, new ProgressStore(context).load().checkedSnapshot().size());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("OANS and BTV if AVAIL")));
    }

    @Test public void engineChecksPersistAfterRotationAdvanceAndStaySeparateFromOtherModels() {
        open("Engine Start");
        onView(withContentDescription("ENGINE MASTER 3 then 4, ON")).perform(scrollTo(), click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withContentDescription("ENGINE MASTER 3 then 4, ON")).perform(scrollTo()).check(matches(isChecked()));
        for (int i = 0; i < 6; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("After Engine Start")));
        ChecklistProgress saved = new ProgressStore(context).load();
        assertTrue(saved.isComplete("fbw-a380x", "engine-start"));
        assertFalse(saved.isComplete("ini-a350", "engine-start"));
        assertEquals(7, saved.checkedSnapshot().size());
        for (String key : saved.checkedSnapshot()) assertTrue(key.startsWith("fbw-a380x/"));
    }

    @Test public void originalThreePagePdfReturnsWithoutLosingA380ChecksAndResetClearsThem() {
        open("Fire Test and APU Start");
        onView(withContentDescription("FIRE TEST, PRESS")).perform(scrollTo(), click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_aircraft)).check(matches(withText("Airbus A380")));
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 3")));
            for (int p = 2; p <= 3; p++) {
                onView(withId(R.id.reader_next)).perform(click());
                onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
                onView(withId(R.id.reader_page_count)).check(matches(withText("Page " + p + " / 3")));
            }
            onView(withId(R.id.reader_back)).perform(click());
            onView(withContentDescription("FIRE TEST, PRESS")).perform(scrollTo()).check(matches(isChecked()));
            onView(withId(R.id.checklist_status)).check(matches(withText(containsString("1 / 12 checked"))));
            onView(withId(R.id.reset_button)).perform(click());
            onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
            assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
}
