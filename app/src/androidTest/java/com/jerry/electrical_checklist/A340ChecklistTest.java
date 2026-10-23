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
import java.util.LinkedHashMap;
import java.util.Map;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class A340ChecklistTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    private ChecklistCatalog.Aircraft model;
    private static final String MEMORY="Non-normal checklists (memory items only)";
    @Before public void launch() throws Exception {
        instrumentation=InstrumentationRegistry.getInstrumentation();context=instrumentation.getTargetContext();
        model=ChecklistCatalog.load(context).aircraft.stream().filter(a->a.id.equals("airbus-a340-300")).findFirst().get();
        ProgressStore store=new ProgressStore(context);store.save(new ChecklistProgress(Collections.emptySet(),Collections.emptySet()));
        Map<String,String> defaults=new LinkedHashMap<>();for(ChecklistCatalog.ChecklistOption o:model.options)defaults.put(o.id,o.defaultValue);
        store.saveOptions(model.id,defaults);
        activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        onView(withContentDescription("Choose aircraft from Airbus")).perform(scrollTo(),click());
        onView(withContentDescription("Open checklists for Airbus A340-300")).perform(scrollTo(),click());
    }
    @After public void close(){if(activity!=null)instrumentation.runOnMainSync(activity::finish);}
    private void open(String title){onView(withText(title)).perform(scrollTo(),click());}
    private void categories(){onView(withId(R.id.checklist_categories_button)).perform(click());}
    private void check(int count){for(int i=0;i<count;i++)onView(withId(R.id.checklist_action)).perform(click());}
    private void finishChecklist(){onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());}
    private void options(){onView(withId(R.id.checklist_options_button)).perform(click());}
    private void select(String label,String value){onView(withContentDescription(label)).perform(click());onData(is(value)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());}
    private void apply(){onView(withText("Apply")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());}
    private void recreate(){
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(activity::recreate);activity=(MainActivity)monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);assertNotNull(activity);
    }
    private void reader(int firstPage){
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(DocumentReaderActivity.class.getName(),null,false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader=(DocumentReaderActivity)monitor.waitForActivityWithTimeout(5000);instrumentation.removeMonitor(monitor);assertNotNull(reader);
        try{onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());onView(withId(R.id.reader_page_count)).check(matches(withText("Page "+firstPage+" / 10")));onView(withId(R.id.reader_back)).perform(click());}
        finally{instrumentation.runOnMainSync(reader::finish);}
    }
    @Test public void normalIsDefaultAndCategoryButtonsOpenRelevantSourcePages() throws Exception {
        assertEquals(9,model.checklists.size());assertEquals(59,model.itemCount());assertEquals(3,model.extraSets.size());
        assertEquals(7,model.extraSet("memory-items").checklists.size());
        open("BEFORE START");reader(1);categories();
        open("Emergency evacuation");onView(withId(R.id.checklist_title)).check(matches(withText("EMERGENCY EVACUATION")));reader(2);
        pressBack();open("Other procedures");reader(3);
        open("TAKEOFF CG/TRIM");reader(2);onView(withId(R.id.checklist_action)).check(matches(not(isEnabled())));
        categories();open(MEMORY);reader(5);onView(withText("BEFORE START")).check(doesNotExist());
        open("STALL RECOVERY");reader(6);
        categories();open(MEMORY);open("GPWS/EGPWS Warnings");reader(7);
        categories();open(MEMORY);open("TCAS WARNING – RESOLUTION ADVISORY");reader(8);
        categories();open(MEMORY);open("WINDSHEAR WARNING – REACTIVE WINDSHEAR");reader(10);
        categories();pressBack();onView(withContentDescription("Open checklists for Airbus A340-300")).perform(scrollTo(),click());
        onView(withText("BEFORE START")).check(matches(isDisplayed()));
        onView(withText("LOSS OF BRAKING")).check(doesNotExist());
    }
    @Test public void normalAdvancesAndMemoryCompletionStaysScopedAfterRotationAndReset(){
        open("BEFORE START");check(11);finishChecklist();onView(withId(R.id.checklist_title)).check(matches(withText("AFTER START")));
        categories();open("SECURING THE AIRCRAFT");check(9);finishChecklist();onView(withId(R.id.checklist_title)).check(matches(withText("SECURING THE AIRCRAFT")));
        categories();open(MEMORY);open("STALL WARNING AT LIFTOFF");check(3);finishChecklist();
        onView(withId(R.id.checklist_title)).check(matches(withText("STALL WARNING AT LIFTOFF")));recreate();
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")));
        pressBack();onView(withId(R.id.checklist_mode_label)).check(matches(withText(MEMORY)));
        assertTrue(new ProgressStore(context).load().isComplete(model.id,"before-start"));
        assertFalse(new ProgressStore(context).load().isComplete("ini-a350","before-start"));
        onView(withId(R.id.reset_button)).perform(click());onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());assertTrue(new ProgressStore(context).load().completedSnapshot().isEmpty());
    }
    @Test public void evacuationBranchesAreExclusiveAndOnlyAffectedCompletionReopens(){
        open("Emergency evacuation");check(9);finishChecklist();onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete")));
        options();onView(withContentDescription("AP/FD TCAS mode")).check(doesNotExist());select("Evacuation required","Not required");apply();
        onView(withContentDescription("EVACUATION, INITIATE")).check(doesNotExist());
        onView(withContentDescription("CABIN CREW AND PASSENGERS (PA), NOTIFY")).check(matches(isNotChecked()));
        assertFalse(new ProgressStore(context).load().isComplete(model.id,"evacuation"));
        check(1);finishChecklist();options();select("Cabin differential pressure","Not zero");apply();
        onView(withContentDescription("CAB PR MODE SEL, MAN")).check(matches(isNotChecked()));
        options();select("MAN CAB PR used","Not used");apply();onView(withContentDescription("CAB PR MODE SEL, MAN")).check(doesNotExist());
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("7 / 7 checked"))));
        assertEquals(14,new ProgressStore(context).loadOptions(model).size());recreate();
        onView(withContentDescription("CABIN CREW AND PASSENGERS (PA), NOTIFY")).check(matches(isChecked()));
    }
    @Test public void tcasAndWindshearOptionsAreScopedAndPreserveOtherSelections(){
        open(MEMORY);open("TCAS WARNING – RESOLUTION ADVISORY");
        options();select("AP/FD TCAS mode","Not available");select("Resolution advisory situation","CLIMB RA in approach CONF 3 or FULL");select("Autopilot","Off");apply();
        onView(withContentDescription("BOTH FDs, OFF")).check(doesNotExist());
        onView(withContentDescription("GO-AROUND, PERFORM")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 6 checked"))));
        categories();open(MEMORY);open("WINDSHEAR WARNING – REACTIVE WINDSHEAR");
        onView(withContentDescription("AP (if engaged), KEEP ON")).check(doesNotExist());
        options();onView(withContentDescription("AP/FD TCAS mode")).check(doesNotExist());select("Windshear phase","Takeoff before V1");apply();
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 1 checked"))));
        onView(withContentDescription(containsString("V1, reject the takeoff."))).perform(scrollTo()).check(matches(isDisplayed()));
        options();select("Windshear phase","Takeoff after V1");apply();check(3);finishChecklist();
        onView(withId(R.id.checklist_title)).check(matches(withText("WINDSHEAR WARNING – REACTIVE WINDSHEAR")));
        Map<String,String> saved=new ProgressStore(context).loadOptions(model);assertEquals("unavailable",saved.get("tcas-mode"));assertEquals("climb",saved.get("tcas-ra"));assertEquals("off",saved.get("ap-state"));
    }
    @Test public void memoryConditionsKeepNotesAndZeroApplicableItemsAreNotCompleted(){
        open(MEMORY);open("EMER DESCENT");
        onView(withContentDescription("THR LEVERS, IDLE")).check(doesNotExist());
        options();select("A/THR active","Not active");select("Structural damage suspected","Suspected");select("CAB ALT above 14 000 ft","Yes");apply();
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 15 checked"))));check(15);finishChecklist();
        onView(withId(R.id.checklist_title)).check(matches(withText("EMER DESCENT")));
        categories();open(MEMORY);open("LOSS OF BRAKING");options();select("Autobrake selected","Not selected");select("No braking after pedal application","No");apply();
        onView(withId(R.id.checklist_status)).check(matches(withText("No items apply to the selected options")));onView(withId(R.id.checklist_action)).check(matches(not(isEnabled())));
        assertFalse(new ProgressStore(context).load().isComplete(model.id,"memory-loss-of-braking"));
        categories();open(MEMORY);open("GPWS/EGPWS Warnings");onView(withText(startsWith("DO NOT CHANGE CONFIGURATION"))).perform(scrollTo()).check(matches(not(isClickable())));
    }
    @Test public void manualEngineProgressIsIndependentAndAutomaticStartIncludesAllFourEngines(){
        open("Other procedures");open("MANUAL ENGINE START - ENGINE 1");reader(4);check(18);finishChecklist();
        onView(withId(R.id.checklist_title)).check(matches(withText("MANUAL ENGINE START - ENGINE 1")));
        pressBack();open("MANUAL ENGINE START - ENGINE 2");onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 18 checked"))));
        pressBack();open("AUTOMATIC ENGINE START");onView(withContentDescription("ANNOUNCE, “STARTING ENGINES 3 and 4”")).perform(scrollTo()).check(matches(isNotChecked()));
        onView(withText("ON ECAM UPPER DISPLAY | ON ECAM LOWER DISPLAY")).perform(scrollTo()).check(matches(not(isClickable())));
        check(7);finishChecklist();onView(withId(R.id.checklist_title)).check(matches(withText("AUTOMATIC ENGINE START")));
    }
}
