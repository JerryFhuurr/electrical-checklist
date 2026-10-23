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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class Boeing757767Test {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation(); context = instrumentation.getTargetContext();
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(),Collections.emptySet()));
        activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(),click());
    }
    @After public void close() { if(activity!=null)instrumentation.runOnMainSync(activity::finish); }
    private void model(String number) { onView(withContentDescription("Open checklists for Boeing "+number)).perform(scrollTo(),click()); }
    private void open(String title) { onView(withText(title)).perform(scrollTo(),click()); }
    private void check(int count) { for(int i=0;i<count;i++)onView(withId(R.id.checklist_action)).perform(click()); }
    private void finishChecklist() { onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click()); }
    @Test public void bothEngineGroupsRequiredAndSavedSeparatelyAcrossModelsAndRotation() throws Exception {
        ChecklistCatalog catalog=ChecklistCatalog.load(context);
        for(String id:new String[]{"boeing-757","boeing-767"}) {
            ChecklistCatalog.Aircraft a=catalog.aircraft.stream().filter(x->x.id.equals(id)).findFirst().get();
            assertEquals(17,a.checklists.size()); assertEquals(202,a.itemCount());
            assertEquals(67,a.checklists.get(1).requiredItems.size());
            assertEquals(14,a.checklists.get(5).requiredItems.size());
        }
        model("757"); open("Engine Start"); check(7);
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark item as complete")));
        onView(allOf(withContentDescription("FUEL CONTROL Switches, RUN"),isDescendantOfA(withTagValue(is("first-fuel-control-switches"))))).check(matches(isChecked()));
        onView(allOf(withContentDescription("FUEL CONTROL Switches, RUN"),isDescendantOfA(withTagValue(is("second-fuel-control-switches"))))).check(matches(isNotChecked()));
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(activity::recreate); activity=(MainActivity)monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor); assertNotNull(activity);
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("7 / 14 checked"))));
        check(7);finishChecklist();onView(withId(R.id.checklist_title)).check(matches(withText("Before Taxi Preparation")));
        assertTrue(new ProgressStore(context).load().isComplete("boeing-757","engine-start"));
        pressBack();pressBack();model("767");open("Engine Start");
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 14 checked"))));
        assertFalse(new ProgressStore(context).load().isComplete("boeing-767","engine-start"));
    }
    @Test public void nativeReferenceShowsFourPagesForEachModelAndPreservesOwnTicks() {
        for(String number:new String[]{"757","767"}) {
            model(number);open("Power Up & Preliminary Preflight");
            onView(withContentDescription("BATTERY Switch, ON")).check(matches(isNotChecked())).perform(click());
            Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(DocumentReaderActivity.class.getName(),null,false);
            onView(withId(R.id.view_checklist_button)).perform(click());
            DocumentReaderActivity reader=(DocumentReaderActivity)monitor.waitForActivityWithTimeout(5000);
            instrumentation.removeMonitor(monitor);assertNotNull(reader);
            try {
                onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
                onView(withId(R.id.reader_aircraft)).check(matches(withText("Boeing "+number)));
                onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 4")));
                for(int page=2;page<=4;page++) {
                    onView(withId(R.id.reader_next)).perform(click());
                    onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
                    onView(withId(R.id.reader_page_count)).check(matches(withText("Page "+page+" / 4")));
                }
                onView(withId(R.id.reader_back)).perform(click());
                onView(withContentDescription("BATTERY Switch, ON")).check(matches(isChecked()));
            } finally {instrumentation.runOnMainSync(reader::finish);}
            pressBack();pressBack();
        }
        assertEquals(2,new ProgressStore(context).load().checkedSnapshot().size());
    }
    @Test public void normalFlowSkipsFuelAlternateAndBothFinalChecklistsStayOpenThenReset() {
        for(String number:new String[]{"757","767"}) {
            model(number);open("After Takeoff Checklist");check(6);finishChecklist();
            onView(withId(R.id.checklist_title)).check(matches(withText("Descend Checklist")));
            pressBack();open("Fuel Balancing (Fuel leak)");check(2);finishChecklist();
            onView(withId(R.id.checklist_title)).check(matches(withText("Descend Checklist")));
            pressBack();open("Preflight Preparation");
            onView(withText("FLIGHT DIRECTOR SOURCE Selector:")).perform(scrollTo()).check(matches(not(isClickable())));
            onView(withContentDescription("APU SELECTOR (if no GPU), START -> ON, optional")).perform(scrollTo(),click());
            pressBack();open("Secure");check(10);finishChecklist();
            onView(withId(R.id.checklist_title)).check(matches(withText("Secure")));
            onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete"))).check(matches(not(isEnabled())));
            pressBack();pressBack();
        }
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        assertTrue(new ProgressStore(context).load().completedSnapshot().isEmpty());
    }
}
