package com.jerry.electrical_checklist;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DocumentReaderTest {
    private DocumentReaderActivity activity;
    private Instrumentation instrumentation;

    @Before public void launch() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        start("ryanair-737-ng");
    }
    private void start(String aircraftId) {
        Context context = instrumentation.getTargetContext();
        activity = (DocumentReaderActivity) instrumentation.startActivitySync(new Intent(context, DocumentReaderActivity.class)
                .putExtra(DocumentReaderActivity.EXTRA_AIRCRAFT_ID, aircraftId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }
    @After public void close() { if (activity != null) instrumentation.runOnMainSync(activity::finish); }

    public static ViewAction awaitPage() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return isAssignableFrom(DocumentPageView.class); }
            @Override public String getDescription() { return "wait for native document render"; }
            @Override public void perform(UiController controller, View view) {
                for (int i = 0; i < 200; i++) {
                    if (((DocumentPageView) view).hasPage()) return;
                    controller.loopMainThreadForAtLeast(50);
                }
                fail("Native reader did not render a page");
            }
        };
    }

    @Test public void pdfZoomPanAndFitWorkWithSinglePageNavigationDisabled() {
        onView(withId(R.id.document_page)).perform(awaitPage());
        onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 1")));
        onView(withId(R.id.reader_previous)).check(matches(not(isEnabled())));
        onView(withId(R.id.reader_next)).check(matches(not(isEnabled())));
        onView(withId(R.id.reader_zoom_in)).perform(click());
        instrumentation.runOnMainSync(() -> assertEquals(1.5f, ((DocumentPageView) activity.findViewById(R.id.document_page)).getZoom(), 0.01f));
        onView(withId(R.id.reader_zoom_out)).perform(click());
        instrumentation.runOnMainSync(() -> assertEquals(1f, ((DocumentPageView) activity.findViewById(R.id.document_page)).getZoom(), 0.01f));
        onView(withId(R.id.document_page)).perform(doubleClick(), swipeLeft());
        instrumentation.runOnMainSync(() -> {
            DocumentPageView page = activity.findViewById(R.id.document_page);
            assertEquals(2.5f, page.getZoom(), 0.01f);
            assertTrue(page.snapshot()[1] < 0);
        });
        onView(withId(R.id.reader_fit)).perform(click());
        instrumentation.runOnMainSync(() -> assertEquals(1f, ((DocumentPageView) activity.findViewById(R.id.document_page)).getZoom(), 0.01f));
    }

    @Test public void rotationRestoresTheDocumentAndZoom() {
        onView(withId(R.id.document_page)).perform(awaitPage());
        onView(withId(R.id.reader_zoom_in)).perform(click());
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(DocumentReaderActivity.class.getName(), null, false);
        instrumentation.runOnMainSync(activity::recreate);
        activity = (DocumentReaderActivity) monitor.waitForActivityWithTimeout(5000);
        instrumentation.removeMonitor(monitor);
        assertNotNull(activity);
        onView(withId(R.id.document_page)).perform(awaitPage());
        onView(withId(R.id.reader_page_count)).check(matches(withText("Page 1 / 1")));
        instrumentation.runOnMainSync(() -> assertEquals(1.5f, ((DocumentPageView) activity.findViewById(R.id.document_page)).getZoom(), 0.01f));
    }

    @Test public void invalidAircraftHasAnErrorAndWorkingBackButton() {
        instrumentation.runOnMainSync(activity::finish);
        start("missing-aircraft");
        onView(withId(R.id.reader_error)).check(matches(isDisplayed()));
        onView(withId(R.id.reader_page_count)).check(matches(withText("Unavailable")));
        onView(withId(R.id.reader_fit)).check(matches(not(isEnabled())));
        onView(withId(R.id.reader_back)).check(matches(isDisplayed()));
    }
}
