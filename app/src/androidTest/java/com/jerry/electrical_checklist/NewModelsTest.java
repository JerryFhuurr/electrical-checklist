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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NewModelsTest {
    private MainActivity activity;
    private Context context;
    private Instrumentation instrumentation;
    private ChecklistCatalog catalog;
    @Before public void launch() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext(); catalog = ChecklistCatalog.load(context);
        ProgressStore store = new ProgressStore(context);
        store.save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        for (ChecklistCatalog.Aircraft a : catalog.aircraft) {
            Map<String,String> defaults = new LinkedHashMap<>();
            for (ChecklistCatalog.ChecklistOption o : a.options) defaults.put(o.id, o.defaultValue);
            store.saveOptions(a.id, defaults);
        }
        start();
    }
    private void start() {
        activity = (MainActivity) instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }
    private ChecklistCatalog.Aircraft aircraft(String id) { return catalog.aircraft.stream().filter(a -> a.id.equals(id)).findFirst().get(); }
    private void openModel(String maker, String model) {
        onView(withContentDescription("Choose aircraft from " + maker)).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for " + model)).perform(scrollTo(), click());
    }
    private void open(String title) { onView(withText(title)).perform(scrollTo(), click()); }
    private void checkRequired(int count) { for (int i=0; i<count; i++) onView(withId(R.id.checklist_action)).perform(click()); }
    private void options() { onView(withId(R.id.checklist_options_button)).perform(click()); }
    private void select(String option, String value) {
        onView(withContentDescription(option)).perform(click());
        onData(is(value)).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
    }
    private void apply() { onView(withText("Apply")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click()); }

    @Test public void catalogsAndOriginalReferencesMatchAllThreeSources() throws Exception {
        assertEquals(9, catalog.aircraft.size());
        ChecklistCatalog.Aircraft a = aircraft("inibuilds-a330-300"), b = aircraft("pmdg-777"), atr = aircraft("atr-42-72");
        assertEquals(26, a.checklists.size()); assertEquals(280, a.itemCount()); assertEquals(3, a.options.size());
        Map<String,String> defaults = new ProgressStore(context).loadOptions(a);
        int visible=0; for (ChecklistCatalog.Checklist c : a.checklists) visible+=c.itemsFor(defaults,true).size();
        assertEquals(264, visible);
        defaults.put("autothrust","off"); defaults.put("power","gpu"); defaults.put("approach","npa");
        visible=0; for (ChecklistCatalog.Checklist c : a.checklists) visible+=c.itemsFor(defaults,true).size();
        assertEquals(262, visible);
        assertEquals(10,b.checklists.size()); assertEquals(38,b.itemCount()); assertEquals(23,b.extendedChecklists.size());
        int extended=0; for (ChecklistCatalog.Checklist c : b.extendedChecklists) extended+=c.items.size();
        assertEquals(428,extended);
        assertEquals(4,b.normalInitialPage); assertEquals(2,b.extendedInitialPage);
        assertEquals(12,atr.checklists.size()); assertEquals(126,atr.itemCount());
        for (ChecklistCatalog.Checklist c : atr.checklists) assertEquals(c.items.size(),c.requiredItems.size());
        for (ChecklistCatalog.Aircraft model : new ChecklistCatalog.Aircraft[]{a,b,atr})
            try (ReferenceSource source=ReferenceSource.openAsset(context,model.references.get(0))) {
                assertEquals(model==b?7:2,source.pageCount());
            }
    }

    @Test public void a330PowerSelectorRequiresOnlyChosenPathAndReopensAffectedCompletion() {
        openModel("Airbus","Airbus A330-300"); open("AIR & POWER · CHOOSE ONE");
        onView(withContentDescription("GPU (EFB), KEEP CONNECTED")).check(doesNotExist());
        checkRequired(4); onView(withId(R.id.checklist_action)).perform(click());
        assertTrue(new ProgressStore(context).load().isComplete("inibuilds-a330-300","air-power-choose-one"));
        pressBack(); open("AIR & POWER · CHOOSE ONE"); options(); select("Air and power","GPU, APU later"); apply();
        onView(withContentDescription("APU MASTER SW, ON")).check(doesNotExist());
        onView(withContentDescription("GPU (EFB), KEEP CONNECTED")).check(matches(isNotChecked()));
        assertFalse(new ProgressStore(context).load().isComplete("inibuilds-a330-300","air-power-choose-one"));
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 1 checked"))));
        checkRequired(1); onView(withId(R.id.checklist_action)).perform(click()); pressBack();
        open("AIR & POWER · CHOOSE ONE"); options(); select("Air and power","APU now"); apply();
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).check(matches(isEnabled()));
        assertFalse(new ProgressStore(context).load().isComplete("inibuilds-a330-300","air-power-choose-one"));
    }

    @Test public void a330AutothrustAndApproachSelectorsHideConflictingAlternatives() {
        openModel("Airbus","Airbus A330-300"); open("FCU · INSTRUMENTS");
        onView(withContentDescription("A/THR, MANUAL THRUST")).check(doesNotExist());
        options(); select("Autothrust","Off (manual thrust)"); apply();
        onView(withContentDescription("A/THR, ARMS AT FLX/TOGA")).check(doesNotExist());
        onView(withContentDescription("A/THR, MANUAL THRUST")).perform(scrollTo()).check(matches(isDisplayed()));
        pressBack(); open("APPROACH TYPE · CHOOSE ONE");
        onView(withContentDescription("PROG · RWY BRG/DIST, INSERT")).check(doesNotExist());
        options(); select("Approach type","RNAV (GNSS)"); apply();
        onView(withContentDescription("APPR PB · BOTH AP, PRESS · ENGAGE")).check(doesNotExist());
        onView(withContentDescription("PROG · RWY BRG/DIST, INSERT")).check(matches(isDisplayed()));
        checkRequired(5); onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("LANDING")));
    }

    @Test public void a330OptionsAndChecksSurviveRotationRestartAndCancelWhileResetOnlyClearsTicks() {
        openModel("Airbus","Airbus A330-300"); open("FCU · INSTRUMENTS");
        options(); select("Autothrust","Off (manual thrust)"); apply();
        onView(withContentDescription("A/THR, MANUAL THRUST")).perform(scrollTo(),click());
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
        instrumentation.runOnMainSync(activity::recreate); activity=(MainActivity)monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor); assertNotNull(activity);
        onView(withContentDescription("A/THR, MANUAL THRUST")).perform(scrollTo()).check(matches(isChecked()));
        options(); select("Autothrust","On (AT)"); onView(withText("Cancel")).perform(click());
        assertEquals("off",new ProgressStore(context).loadOptions(aircraft("inibuilds-a330-300")).get("autothrust"));
        instrumentation.runOnMainSync(activity::finish); start(); openModel("Airbus","Airbus A330-300"); open("FCU · INSTRUMENTS");
        onView(withContentDescription("A/THR, MANUAL THRUST")).perform(scrollTo()).check(matches(isChecked()));
        onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        assertEquals("off",new ProgressStore(context).loadOptions(aircraft("inibuilds-a330-300")).get("autothrust"));
    }

    @Test public void normalLandingSkipsGoAroundButAlternateRemainsSelectable() {
        openModel("Airbus","Airbus A330-300"); assertReaderPage(1,2); open("LANDING");
        checkRequired(3); onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("AFTER LANDING")));
        pressBack(); open("GO-AROUND");
        onView(withContentDescription("THRUST LEVERS, TOGA")).check(matches(isDisplayed()));
    }

    @Test public void boeingNormalAndFullProceduresUseChosenRouteAndSkipReferenceSchedules() {
        openModel("Boeing","Boeing 777");
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Normal checklists - pages 4-7")));
        open("PREFLIGHT CHECKLIST"); checkRequired(4); onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("BEFORE START CHECKLIST")));
        onView(withId(R.id.checklist_mode_button)).perform(click());
        onView(withId(R.id.checklist_mode_label)).check(matches(withText("Full procedures - pages 2-7")));
        onView(withText("FMC SETUP - Part I - without SimBrief")).check(doesNotExist());
        options(); select("FMC route setup","Without SimBrief"); apply();
        onView(withText("FMC SETUP - Part I - with SimBrief")).check(doesNotExist());
        open("FMC SETUP - Part I - without SimBrief");
        assertTrue(new ProgressStore(context).load().isComplete("pmdg-777","preflight-checklist"));
        pressBack(); open("TAKEOFF"); checkRequired(14); // One conditional manual-flying row is optional.
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("AFTER TAKEOFF | CLIMB")));
    }

    @Test public void boeingOriginalReaderStartsOnRelevantPageAndOptionalFoPanelDoesNotBlock() {
        openModel("Boeing","Boeing 777");
        assertReaderPage(4,7);
        onView(withId(R.id.checklist_mode_button)).perform(click()); assertReaderPage(2,7);
        open("PREFLIGHT - Part III");
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 7 optional"))));
        onView(withId(R.id.checklist_action)).check(matches(withText("Mark checklist as complete"))).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("BEFORE PUSHBACK / BEFORE START")));
    }
    private void assertReaderPage(int page,int total) {
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(DocumentReaderActivity.class.getName(),null,false);
        onView(withId(R.id.view_checklist_button)).perform(click());
        DocumentReaderActivity reader=(DocumentReaderActivity)monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor); assertNotNull(reader);
        try {
            onView(withId(R.id.document_page)).perform(DocumentReaderTest.awaitPage());
            onView(withId(R.id.reader_page_count)).check(matches(withText("Page "+page+" / "+total)));
            onView(withId(R.id.reader_back)).perform(click());
        } finally {instrumentation.runOnMainSync(reader::finish);}
    }

    @Test public void atrUsesRequiredChecksDespiteGreySourceRowsAndKeepsTimingNotes() {
        openModel("ATR","ATR 42 / 72"); assertReaderPage(1,2); open("Engine Start");
        onView(withId(R.id.checklist_status)).check(matches(withText(containsString("0 / 13 checked"))));
        checkRequired(13); onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_title)).check(matches(withText("Taxi")));
        pressBack(); open("After Landing");
        onView(withText("AFTER MIN. 1 MINUTE")).perform(scrollTo()).check(matches(isDisplayed()));
        onView(withContentDescription("After 30 Seconds, ENG1 Condition, SHUT OFF")).perform(scrollTo(),click());
        assertEquals(14,new ProgressStore(context).load().checkedSnapshot().size());
        assertFalse(new ProgressStore(context).load().isComplete("inibuilds-a330-300","engine-start"));
    }
}
