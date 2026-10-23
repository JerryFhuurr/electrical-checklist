package com.jerry.electrical_checklist;

import android.content.Context;
import android.content.Intent;
import android.app.Instrumentation;
import android.graphics.Rect;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matcher;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class ChecklistAppTest {
    private Context context;
    private MainActivity activity;

    @Before public void launch() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        activity = (MainActivity) InstrumentationRegistry.getInstrumentation().startActivitySync(
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @After public void close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.finish());
    }
    private void openSafety() {
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Safety Inspection")).perform(click());
    }
    @Test public void bundledCatalogHasAllSourceSectionsAndItems() throws Exception {
        ChecklistCatalog catalog = ChecklistCatalog.load(context);
        ChecklistCatalog.Aircraft model = catalog.aircraft.get(0);
        assertEquals(10, model.checklists.size());
        assertEquals(101, model.itemCount());
        assertEquals(38, model.checklists.get(1).items.size());
        assertEquals("Below the line", model.checklists.get(1).items.get(34).group);
        assertEquals("If applicable (**)", model.checklists.get(1).items.get(2).note);
        assertEquals("BATTERY", model.checklists.get(9).items.get(6).challenge);
        assertEquals(1, model.references.size());
        assertEquals("references/ryanair-737-ng/normal-checklist.pdf", model.references.get(0).path);
    }
    @Test public void originalChecklistOpensFromItemsAndReturnsWithoutLosingProgress() {
        openSafety();
        onView(withContentDescription("BATTERY, ON")).perform(click());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 1")));
            onView(withId(R.id.reader_back)).perform(click());
            onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
            onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        } finally { instrumentation.runOnMainSync(reader::finish); }
    }
    @Test public void completingChecklistAdvancesAndUncheckingReopensIt() {
        openSafety();
        for (int i = 0; i < 6; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Before Start")));
        assertTrue(new ProgressStore(context).load().isComplete("ryanair-737-ng", "safety-inspection"));
        androidx.test.espresso.Espresso.pressBack();
        onView(withText("Safety Inspection")).perform(click());
        onView(withContentDescription("SURFACES & CHOCKS, CHECKED")).perform(scrollTo(), click());
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark item as complete")));
        assertFalse(new ProgressStore(context).load().isComplete("ryanair-737-ng", "safety-inspection"));
    }

    @Test public void completingLastChecklistStaysOnLastAndCanBeReopened() {
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Secure")).perform(scrollTo(), click());
        for (int i = 0; i < 7; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Secure")));
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")))
                .check(matches(org.hamcrest.Matchers.not(isEnabled())));
        assertTrue(new ProgressStore(context).load().isComplete("ryanair-737-ng", "secure"));
        onView(withContentDescription("BATTERY, OFF")).perform(scrollTo(), click());
        onView(withId(R.id.checklist_action)).check(matches(isEnabled()));
        assertFalse(new ProgressStore(context).load().isComplete("ryanair-737-ng", "secure"));
    }

    @Test public void checkingBottomVisibleRowRevealsNextWithoutScrollingForTopRow() {
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        onView(withText("Before Start")).perform(click());
        onView(withContentDescription("IRS MODE SELECTORS, NAV")).perform(click());
        AtomicReference<String> bottomDescription = new AtomicReference<>();
        AtomicReference<View> nextRow = new AtomicReference<>();
        AtomicInteger originalY = new AtomicInteger();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ScrollView scroll = activity.findViewById(R.id.page_scroll);
            assertEquals(0, scroll.getScrollY());
            originalY.set(scroll.getScrollY());
            LinearLayout rows = activity.findViewById(R.id.item_rows);
            int lastVisible = -1;
            for (int i = 0; i < rows.getChildCount(); i++) {
                View row = rows.getChildAt(i);
                if (row.findViewById(R.id.item_checkbox) != null
                        && row.getBottom() <= scroll.getHeight() - scroll.getPaddingBottom()) lastVisible = i;
            }
            assertTrue(lastVisible >= 0 && lastVisible + 1 < rows.getChildCount());
            CheckBox box = rows.getChildAt(lastVisible).findViewById(R.id.item_checkbox);
            bottomDescription.set(box.getContentDescription().toString());
            nextRow.set(rows.getChildAt(lastVisible + 1));
        });
        onView(withContentDescription(bottomDescription.get())).perform(click());
        onView(withId(R.id.page_scroll)).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isDisplayed(); }
            @Override public String getDescription() { return "wait for automatic scroll to reveal next row"; }
            @Override public void perform(UiController controller, View view) {
                ScrollView scroll = (ScrollView) view;
                Rect visible = new Rect();
                for (int i = 0; i < 120; i++) {
                    if (scroll.getScrollY() > originalY.get() && nextRow.get().getGlobalVisibleRect(visible)
                            && visible.height() == nextRow.get().getHeight()) return;
                    controller.loopMainThreadForAtLeast(16);
                }
                fail("Checking the bottom visible row did not reveal the next row");
            }
        });
    }

    @Test public void rotationPreservesSelectedChecklistAndIndependentCheckboxValues() {
        openSafety();
        onView(withContentDescription("BATTERY, ON")).perform(scrollTo(), click());
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(MainActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(() -> activity.recreate());
        activity = (MainActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
        onView(withContentDescription("BATTERY, ON")).perform(scrollTo()).check(matches(isChecked()));
        onView(withContentDescription("SURFACES & CHOCKS, CHECKED")).perform(scrollTo()).check(matches(isNotChecked()));
    }
    @Test public void resetCanBeCancelledAndThenClearsSavedProgress() {
        openSafety();
        onView(withContentDescription("SURFACES & CHOCKS, CHECKED")).perform(scrollTo(), click());
        onView(withText("Reset all")).perform(click());
        onView(withText("Cancel")).perform(click());
        onView(withContentDescription("SURFACES & CHOCKS, CHECKED")).check(matches(isChecked()));
        onView(withText("Reset all")).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        onView(withContentDescription("SURFACES & CHOCKS, CHECKED")).check(matches(isNotChecked()));
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        assertTrue(new ProgressStore(context).load().completedSnapshot().isEmpty());
    }
    @Test public void progressSurvivesActivityRestartAndStaysScopedToPhase() {
        openSafety();
        onView(withContentDescription("BATTERY, ON")).perform(scrollTo(), click());
        androidx.test.espresso.Espresso.pressBack();
        onView(withText("Before Start")).perform(click());
        onView(withContentDescription("IRS MODE SELECTORS, NAV")).check(matches(isNotChecked()));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> activity.finish());
        activity = (MainActivity) InstrumentationRegistry.getInstrumentation().startActivitySync(
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        openSafety();
        onView(withContentDescription("BATTERY, ON")).perform(scrollTo()).check(matches(isChecked()));
    }
}
