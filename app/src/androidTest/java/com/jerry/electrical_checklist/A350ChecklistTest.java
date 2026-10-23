package com.jerry.electrical_checklist;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
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
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class A350ChecklistTest {
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
        onView(withContentDescription("Open checklists for Airbus A350")).perform(scrollTo(), click());
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private void open(String title) { onView(withText(title)).perform(scrollTo(), click()); }

    @Test public void catalogSeparatesCheckableItemsAndOrderedTextRows() throws Exception {
        ChecklistCatalog.Aircraft model = ChecklistCatalog.load(context).aircraft.get(2);
        assertEquals("ini-a350", model.id);
        assertEquals(31, model.checklists.size());
        assertEquals(258, model.itemCount());
        int texts = 0;
        for (ChecklistCatalog.Checklist list : model.checklists)
            for (ChecklistCatalog.ChecklistRow row : list.rows) if (row.item == null) texts++;
        assertEquals(26, texts);
        ChecklistCatalog.Checklist tod = model.checklists.stream().filter(c -> c.id.equals("before-tod")).findFirst().get();
        assertEquals(10, tod.items.size());
        assertEquals(11, tod.rows.size());
        ChecklistCatalog.ChecklistRow heading = tod.rows.get(4);
        assertNull(heading.item);
        assertEquals("heading", heading.type);
        assertEquals("IN FMS", heading.text);
        assertTrue(heading.bold);
        assertEquals(28, heading.indent);
        assertEquals("FMS ACTIVE/F-PLN/ARRIVAL PAGE", tod.rows.get(5).item.challenge);
        try (ReferenceSource source = ReferenceSource.openAsset(context, model.references.get(0))) {
            assertEquals(3, source.pageCount());
        }
    }

    @Test public void inFmsIsBoldNonClickableAndSkippedDuringCompletion() {
        open("Before TOD");
        onView(withText("IN FMS")).perform(scrollTo());
        instrumentation.runOnMainSync(() -> {
            LinearLayout rows = activity.findViewById(R.id.item_rows);
            View heading = rows.getChildAt(4);
            assertTrue(heading instanceof TextView);
            assertEquals("IN FMS", ((TextView) heading).getText().toString());
            assertTrue(((TextView) heading).getTypeface().isBold());
            assertTrue(heading.isAccessibilityHeading());
            assertFalse(heading.isClickable());
            assertFalse(heading.isLongClickable());
            assertTrue(heading.getPaddingStart() > 48);
        });
        onView(withText("IN FMS")).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 10 checked"))));
        for (int i = 0; i < 10; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete")));
        assertEquals(10, new ProgressStore(context).load().checkedSnapshot().size());
        for (String key : new ProgressStore(context).load().checkedSnapshot()) assertFalse(key.endsWith("/in-fms"));
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("OANS and BTV if AVAIL")));
    }

    @Test public void textOnlySectionsContinueWithoutCreatingCheckedItemsOrCompletion() {
        open("PRE FLIGHT");
        onView(withId(R.id.item_checkbox)).check(doesNotExist());
        onView(withId(R.id.checklist_action)).check(matches(withText("Continue to next checklist"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Preliminary Cockpit Preparation")));
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        assertTrue(new ProgressStore(context).load().completedSnapshot().isEmpty());
        androidx.test.espresso.Espresso.pressBack();
        open("Initialise FMS");
        onView(withId(R.id.item_checkbox)).check(doesNotExist());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Glareshield & Lateral Consoles")));
    }

    @Test public void notesKeepOriginalCaseItalicsAlignmentAndFormattingAfterRotation() {
        open("Turning Into Gate & Engine Shut Down");
        onView(withText("After engine N1 <5%")).perform(scrollTo());
        instrumentation.runOnMainSync(() -> {
            LinearLayout rows = activity.findViewById(R.id.item_rows);
            TextView heading = rows.findViewWithTag("after-engine-n1-5");
            TextView note = rows.findViewWithTag("call-gsx");
            assertEquals("After engine N1 <5%", heading.getText().toString());
            assertTrue(heading.getTypeface().isBold());
            assertEquals("call GSX", note.getText().toString());
            assertEquals(Typeface.ITALIC, note.getTypeface().getStyle());
            assertEquals(Gravity.END, note.getGravity() & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK);
            assertFalse(note.isClickable());
        });
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withId(R.id.checklist_title)).check(matches(withText("Turning Into Gate & Engine Shut Down")));
        onView(withText("After engine N1 <5%")).perform(scrollTo()).check(matches(isDisplayed()));
    }

    @Test public void originalThreePagePdfOpensAndReturnsToSameChecklist() {
        open("Before TOD");
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 3")));
            onView(withId(R.id.reader_next)).perform(click());
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 2 / 3")));
            onView(withId(R.id.reader_back)).perform(click());
            onView(withId(R.id.checklist_title)).check(matches(withText("Before TOD")));
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
}
