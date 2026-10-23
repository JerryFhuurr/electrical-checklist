package com.jerry.electrical_checklist;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class ChecklistProgressTest {
    private ChecklistProgress empty() {
        return new ChecklistProgress(Collections.emptySet(), Collections.emptySet());
    }
    @Test public void completionRequiresEveryItemAndExplicitAcknowledgement() {
        ChecklistProgress progress = empty();
        assertFalse(progress.complete("737", "start", Arrays.asList("fuel", "doors")));
        progress.setChecked("737", "start", "fuel", true);
        assertFalse(progress.complete("737", "start", Arrays.asList("fuel", "doors")));
        progress.setChecked("737", "start", "doors", true);
        assertFalse(progress.isComplete("737", "start"));
        assertTrue(progress.complete("737", "start", Arrays.asList("fuel", "doors")));
        assertTrue(progress.isComplete("737", "start"));
        progress.setChecked("737", "start", "doors", false);
        assertFalse(progress.isComplete("737", "start"));
        assertTrue(progress.isChecked("737", "start", "fuel"));
    }
    @Test public void aircraftAndPhasesHaveIndependentProgress() {
        ChecklistProgress progress = empty();
        progress.setChecked("737", "start", "fuel", true);
        assertFalse(progress.isChecked("a320", "start", "fuel"));
        assertFalse(progress.isChecked("737", "shutdown", "fuel"));
    }
    @Test public void resetClearsAllAircraftAndCompletions() {
        ChecklistProgress progress = empty();
        for (String aircraft : Arrays.asList("737", "a320")) {
            progress.setChecked(aircraft, "start", "fuel", true);
            assertTrue(progress.complete(aircraft, "start", Collections.singletonList("fuel")));
        }
        progress.resetAll();
        assertTrue(progress.checkedSnapshot().isEmpty());
        assertTrue(progress.completedSnapshot().isEmpty());
    }
    @Test public void snapshotsCanBeSavedAndRestoredWithoutSharingMutableSets() {
        Set<String> saved = new HashSet<>(Collections.singletonList("737/start/fuel"));
        ChecklistProgress progress = new ChecklistProgress(saved, Collections.emptySet());
        saved.clear();
        assertTrue(progress.isChecked("737", "start", "fuel"));
        progress.checkedSnapshot().clear();
        ChecklistProgress restored = new ChecklistProgress(progress.checkedSnapshot(), progress.completedSnapshot());
        assertTrue(restored.isChecked("737", "start", "fuel"));
    }
    @Test public void emptyChecklistCannotBeCompleted() {
        assertFalse(empty().complete("737", "start", Collections.emptyList()));
    }
}
