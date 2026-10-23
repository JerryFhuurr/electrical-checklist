package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AircraftNavigationTest {
    private Instrumentation instrumentation;
    private Context context;
    private NavigationPreferences navigation;
    private ChecklistCatalog catalog;
    @Before public void launch() throws Exception {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        context.getSharedPreferences("navigation_preferences", Context.MODE_PRIVATE).edit().clear().commit();
        navigation = new NavigationPreferences(context); catalog = ChecklistCatalog.load(context);
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        instrumentation.runOnMainSync(() -> {
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
        restart();
    }
    private void restart() {
        instrumentation.startActivitySync(new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        instrumentation.waitForIdleSync();
    }
    private Activity resumed() {
        AtomicReference<Activity> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).iterator().next()));
        return result.get();
    }
    private void rotate(int orientation) {
        Activity current = resumed();
        int desired = orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT;
        if (current.getResources().getConfiguration().orientation == desired) return;
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(current.getClass().getName(), null, false);
        instrumentation.runOnMainSync(() -> current.setRequestedOrientation(orientation));
        assertNotNull(monitor.waitForActivityWithTimeout(5000));
        instrumentation.removeMonitor(monitor); instrumentation.waitForIdleSync();
    }
    @After public void close() {
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
            DisplaySettings settings = new DisplaySettings(context);
            settings.setFontChoice(1); settings.setNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        });
        context.getSharedPreferences("navigation_preferences", Context.MODE_PRIVATE).edit().clear().commit();
    }
    private void home() { onView(withId(R.id.home_button)).perform(click()); }
    private void assertHome() {
        onView(withId(R.id.manufacturer_rows)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        onView(withId(R.id.home_button)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        onView(withId(R.id.favorite_aircraft_rows)).check(doesNotExist());
        onView(withId(R.id.recent_aircraft_rows)).check(doesNotExist());
    }
    private void collection(int section) {
        onView(withId(section == R.id.favorite_aircraft_rows ? R.id.open_favorites_button : R.id.open_recent_button))
                .perform(scrollTo(), click());
    }
    private void open(String manufacturer, String aircraft) {
        onView(withContentDescription("Choose aircraft from " + manufacturer)).perform(scrollTo(), click());
        onView(withContentDescription("Open checklists for " + aircraft)).perform(scrollTo(), click());
    }
    private void phase(String id) { onView(allOf(withTagValue(is((Object) id)), isDescendantOfA(withId(R.id.phase_rows)))).perform(scrollTo(), click()); }
    private void shortcut(String aircraft, int section) {
        onView(allOf(withContentDescription("Open " + aircraft + " checklists"), isDescendantOfA(withId(section)))).perform(scrollTo(), click());
    }
    private void unstar(String aircraft) {
        collection(R.id.favorite_aircraft_rows);
        onView(allOf(withContentDescription("Remove " + aircraft + " from favorites"),
                isDescendantOfA(withId(R.id.favorite_aircraft_rows)))).perform(scrollTo(), click());
    }
    private void startup(int choice) {
        onView(withId(R.id.settings_button)).perform(click());
        onView(withId(choice)).perform(scrollTo(), click());
    }
    private List<String> recentIds() {
        List<String> ids = new ArrayList<>();
        for (ChecklistCatalog.Aircraft model : navigation.recentAircraft(catalog)) ids.add(model.id);
        return ids;
    }

    @Test public void favoritesPersistAndQuickLinksKeepProgressWhileResetPreservesPreferences() {
        onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
        onView(withContentDescription("Add Boeing 737-NG to favorites")).perform(scrollTo(), click());
        onView(withContentDescription("Remove Boeing 737-NG from favorites")).check(matches(isDisplayed()));
        assertTrue(recentIds().isEmpty());
        onView(withContentDescription("Open checklists for Boeing 737-NG")).perform(scrollTo(), click());
        phase("safety-inspection"); onView(withContentDescription("BATTERY, ON")).perform(click());
        home(); assertHome();
        collection(R.id.favorite_aircraft_rows);
        onView(withId(R.id.favorites_empty)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        shortcut("Boeing 737-NG", R.id.favorite_aircraft_rows); phase("safety-inspection");
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        home(); onView(withId(R.id.reset_button)).perform(click());
        onView(withText("Reset all")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(click());
        assertTrue(navigation.isFavorite("ryanair-737-ng")); assertEquals("ryanair-737-ng", recentIds().get(0));
        assertEquals("safety-inspection", navigation.lastChecklist().checklistId);
        assertTrue(new ProgressStore(context).load().checkedSnapshot().isEmpty());
        restart(); collection(R.id.recent_aircraft_rows); shortcut("Boeing 737-NG", R.id.recent_aircraft_rows); phase("safety-inspection");
        onView(withContentDescription("BATTERY, ON")).check(matches(isNotChecked()));
        home(); unstar("Boeing 737-NG");
        onView(withId(R.id.favorites_empty)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)));
        assertFalse(navigation.isFavorite("ryanair-737-ng"));
        pressBack(); collection(R.id.recent_aircraft_rows);
        shortcut("Boeing 737-NG", R.id.recent_aircraft_rows);
        onView(withId(R.id.model_title)).check(matches(withText("Boeing 737-NG")));
    }

    @Test public void recentsAreUniqueMostRecentFirstAndLimitedToFive() {
        List<ChecklistCatalog.Aircraft> models = catalog.aircraft;
        for (int i = 0; i < 6; i++) navigation.recordAircraft(models.get(i).id);
        navigation.recordAircraft(models.get(1).id);
        assertEquals(Arrays.asList(models.get(1).id, models.get(5).id, models.get(4).id, models.get(3).id, models.get(2).id), recentIds());
        restart();
        collection(R.id.recent_aircraft_rows);
        onView(withId(R.id.recent_aircraft_rows)).check((view,error) -> {
            if (error != null) throw error; assertEquals(5, ((ViewGroup) view).getChildCount());
        });
        shortcut(models.get(2).name, R.id.recent_aircraft_rows);
        assertEquals(models.get(2).id, recentIds().get(0)); assertEquals(5, recentIds().size());
        home(); collection(R.id.recent_aircraft_rows); shortcut(models.get(2).name, R.id.recent_aircraft_rows);
        assertEquals(5, recentIds().size());
    }

    @Test public void collectionPagesHaveEmptyStatesAndRestoreTheirBackDestinationAfterRotation() {
        assertHome();
        assertEquals("E-Checklist", context.getApplicationInfo().loadLabel(context.getPackageManager()).toString());
        onView(withId(R.id.brand_label)).check(matches(withText("E-Checklist")));
        onView(withText("Every check.\nA little clearer.")).check(doesNotExist());
        onView(withText("YOUR FLIGHT COMPANION")).check(doesNotExist());
        collection(R.id.favorite_aircraft_rows);
        onView(withId(R.id.favorites_empty)).check(matches(isDisplayed()));
        onView(withId(R.id.recent_aircraft_rows)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        pressBack(); assertHome(); collection(R.id.recent_aircraft_rows);
        onView(withId(R.id.recent_aircraft_empty)).check(matches(isDisplayed()));
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withId(R.id.shortcuts_title)).check(matches(withText("Recent aircraft")));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withId(R.id.recent_aircraft_empty)).check(matches(isDisplayed()));
        home(); assertHome();
        navigation.setFavorite("ryanair-737-ng", true);
        collection(R.id.favorite_aircraft_rows); shortcut("Boeing 737-NG", R.id.favorite_aircraft_rows);
        phase("safety-inspection"); onView(withContentDescription("BATTERY, ON")).perform(click());
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withId(R.id.checklist_title)).check(matches(withText("Safety Inspection")));
        pressBack(); onView(withContentDescription("Back to favorites")).perform(click());
        onView(withId(R.id.shortcuts_title)).check(matches(withText("Favorites")));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        shortcut("Boeing 737-NG", R.id.favorite_aircraft_rows); phase("safety-inspection");
        onView(withContentDescription("BATTERY, ON")).check(matches(isChecked()));
        home(); collection(R.id.recent_aircraft_rows); shortcut("Boeing 737-NG", R.id.recent_aircraft_rows);
        onView(withContentDescription("Back to recent aircraft")).perform(click());
        onView(withId(R.id.shortcuts_title)).check(matches(withText("Recent aircraft")));
        onView(withContentDescription("Back to manufacturers")).perform(click()); assertHome();
    }

    @Test public void favoriteStartupChoosesConfiguredPlaneAndRotationKeepsCurrentNavigation() {
        navigation.setFavorite("ryanair-737-ng", true); navigation.setFavorite("airbus-a320-family", true);
        startup(R.id.startup_favorite);
        onView(withId(R.id.startup_favorite_selector)).perform(scrollTo(), click());
        onData(is("Boeing 737-NG")).inRoot(androidx.test.espresso.matcher.RootMatchers.isPlatformPopup()).perform(click());
        pressBack(); assertHome(); restart();
        onView(withId(R.id.model_title)).check(matches(withText("Boeing 737-NG")));
        home(); open("Airbus", "Airbus A320 Family"); phase("cockpit-preparation");
        onView(withContentDescription("ADIRS, NAV")).perform(scrollTo(), click());
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(withId(R.id.checklist_workspace)).check(matches(isDisplayed()));
        onView(withId(R.id.checklist_title)).check(matches(withText("Cockpit Preparation")));
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onView(withContentDescription("ADIRS, NAV")).check(matches(isChecked()));
        restart(); onView(withId(R.id.model_title)).check(matches(withText("Boeing 737-NG")));
        assertEquals(2, navigation.favorites(catalog).size());
    }

    @Test public void lastStartupResumesNextChecklistAndManufacturerSettingOpensHome() {
        startup(R.id.startup_last_checklist); pressBack(); restart(); assertHome();
        open("Boeing", "Boeing 737-NG"); phase("safety-inspection");
        for (int i = 0; i < 6; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        assertEquals("before-start", navigation.lastChecklist().checklistId);
        home(); open("Airbus", "Airbus A320 Family"); restart();
        onView(withId(R.id.checklist_title)).check(matches(withText("Before Start")));
        assertTrue(new ProgressStore(context).load().isComplete("ryanair-737-ng", "safety-inspection"));
        startup(R.id.startup_manufacturers); pressBack();
        onView(withId(R.id.checklist_title)).check(matches(withText("Before Start")));
        restart(); assertHome();
    }

    @Test public void lastStartupRestoresExtendedAndMemoryCategoryIncludingCompletion() {
        navigation.setStartupMode(NavigationPreferences.LAST_CHECKLIST);
        open("Airbus", "Airbus A320 Family");
        onView(withId(R.id.checklist_mode_button)).perform(scrollTo(), click()); phase("extended-cockpit-preparation");
        onView(withId(R.id.checklist_action)).perform(click());
        assertTrue(navigation.lastChecklist().extended);
        restart(); onView(withId(R.id.checklist_title)).check(matches(withText("Cockpit Preparation")));
        onView(withId(R.id.checklist_mode_button)).check(matches(withText("Show normal checklist")));
        assertTrue(navigation.lastChecklist().extended);
        home(); open("Airbus", "Airbus A340-300");
        onView(withTagValue(is((Object) "memory-items"))).perform(scrollTo(), click()); phase("memory-stall-warning-at-liftoff");
        for (int i = 0; i < 3; i++) onView(withId(R.id.checklist_action)).perform(click());
        onView(withId(R.id.checklist_action)).perform(click());
        restart(); onView(withId(R.id.checklist_title)).check(matches(withText("STALL WARNING AT LIFTOFF")));
        onView(withId(R.id.checklist_action)).check(matches(withText("Checklist complete"))).check(matches(not(isEnabled())));
        assertEquals("memory-items", navigation.lastChecklist().categoryId);
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onView(allOf(withTagValue(is((Object) "memory-stall-warning-at-liftoff")),
                isDescendantOfA(withId(R.id.sidebar_phase_rows)))).check(matches(isSelected()));
        home(); assertHome();
    }

    @Test public void missingOrStaleStartupTargetsFallBackToHome() {
        startup(R.id.startup_favorite);
        onView(withId(R.id.startup_favorites_empty)).perform(scrollTo()).check(matches(isDisplayed()));
        pressBack(); restart(); assertHome();
        navigation.setFavorite("removed-aircraft", true); navigation.setStartupFavorite("removed-aircraft");
        restart(); assertHome(); navigation.setStartupMode(NavigationPreferences.LAST_CHECKLIST);
        navigation.saveLastChecklist("removed-aircraft", false, "", "before-start"); restart(); assertHome();
        navigation.saveLastChecklist("ryanair-737-ng", false, "", "removed-checklist"); restart(); assertHome();
        navigation.saveLastChecklist("ryanair-737-ng", false, "removed-category", "before-start"); restart(); assertHome();
    }

    @Test public void removingStartupFavoriteUsesRemainingFavoriteThenFallsBackToHome() {
        navigation.setFavorite("ryanair-737-ng", true); navigation.setFavorite("airbus-a320-family", true);
        navigation.setStartupFavorite("airbus-a320-family"); navigation.setStartupMode(NavigationPreferences.FAVORITE);
        restart(); onView(withId(R.id.model_title)).check(matches(withText("Airbus A320 Family")));
        home(); unstar("Airbus A320 Family"); restart();
        onView(withId(R.id.model_title)).check(matches(withText("Boeing 737-NG")));
        home(); unstar("Boeing 737-NG"); restart(); assertHome();
        assertEquals(NavigationPreferences.FAVORITE, navigation.startupMode());
    }
}
