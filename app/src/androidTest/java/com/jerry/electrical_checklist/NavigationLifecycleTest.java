package com.jerry.electrical_checklist;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.Espresso.pressBackUnconditionally;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class NavigationLifecycleTest {
    private Context context;
    private Instrumentation instrumentation;

    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        context = instrumentation.getTargetContext();
        new NavigationPreferences(context).setStartupMode(NavigationPreferences.MANUFACTURERS);
        new ProgressStore(context).save(new ChecklistProgress(Collections.emptySet(), Collections.emptySet()));
        instrumentation.startActivitySync(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setClass(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    @After public void close() {
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : Stage.values()) for (Activity activity : new ArrayList<>(
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
        });
    }

    @Test public void launcherResumeAfterHomeBackKeepsSystemAndToolbarBackWorking() throws Exception {
        for (int round = 0; round < 2; round++) {
            pressBackUnconditionally();
            instrumentation.waitForIdleSync();
            try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(
                    instrumentation.getUiAutomation().executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10000000 -n com.jerry.electrical_checklist/.MainActivity"))) {
                byte[] buffer = new byte[1024];
                while (input.read(buffer) != -1) { /* Wait until the existing launcher task is resumed. */ }
            }
            onView(withId(R.id.brand_label)).check(matches(withText("E-Checklist")));
            onView(withContentDescription("Choose aircraft from Boeing")).perform(scrollTo(), click());
            onView(withContentDescription("Open checklists for Boeing 757")).perform(scrollTo(), click());
            onView(withText("Engine Start")).perform(scrollTo(), click());
            onView(withId(R.id.brand_label)).check(matches(withText("Boeing 757")));
            if (round == 0) onView(withId(R.id.back_button)).perform(click()); else pressBack();
            onView(withId(R.id.phase_rows)).check(matches(isDisplayed()));
            pressBack(); onView(withId(R.id.aircraft_rows)).check(matches(isDisplayed()));
            pressBack(); onView(withId(R.id.manufacturer_rows)).check(matches(isDisplayed()));
        }
    }
}
