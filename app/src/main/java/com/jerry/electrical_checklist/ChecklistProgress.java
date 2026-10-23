package com.jerry.electrical_checklist;

import java.util.HashSet;
import java.util.Set;

/** Small, platform-independent state machine; completion always needs explicit acknowledgement. */
public final class ChecklistProgress {
    private final Set<String> checked;
    private final Set<String> completed;

    public ChecklistProgress(Set<String> checked, Set<String> completed) {
        this.checked = new HashSet<>(checked);
        this.completed = new HashSet<>(completed);
    }

    private String key(String aircraftId, String checklistId) { return aircraftId + "/" + checklistId; }
    private String key(String aircraftId, String checklistId, String itemId) {
        return key(aircraftId, checklistId) + "/" + itemId;
    }
    public boolean isChecked(String aircraftId, String checklistId, String itemId) {
        return checked.contains(key(aircraftId, checklistId, itemId));
    }
    public boolean isComplete(String aircraftId, String checklistId) {
        return completed.contains(key(aircraftId, checklistId));
    }
    public void setChecked(String aircraftId, String checklistId, String itemId, boolean value) {
        String key = key(aircraftId, checklistId, itemId);
        if (value) checked.add(key);
        else {
            checked.remove(key);
            completed.remove(key(aircraftId, checklistId));
        }
    }
    public boolean complete(String aircraftId, String checklistId, Iterable<String> itemIds) {
        boolean hasItems = false;
        for (String itemId : itemIds) {
            hasItems = true;
            if (!isChecked(aircraftId, checklistId, itemId)) return false;
        }
        if (!hasItems) return false;
        completed.add(key(aircraftId, checklistId));
        return true;
    }
    public void resetAll() { checked.clear(); completed.clear(); }
    public Set<String> checkedSnapshot() { return new HashSet<>(checked); }
    public Set<String> completedSnapshot() { return new HashSet<>(completed); }
}
