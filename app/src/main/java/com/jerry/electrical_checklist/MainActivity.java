package com.jerry.electrical_checklist;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.jerry.electrical_checklist.ChecklistCatalog.Aircraft;
import com.jerry.electrical_checklist.ChecklistCatalog.Checklist;
import com.jerry.electrical_checklist.ChecklistCatalog.Item;
import com.jerry.electrical_checklist.ChecklistCatalog.ChecklistRow;
import com.jerry.electrical_checklist.ChecklistCatalog.ChecklistOption;

/** XML layouts own presentation; this activity binds the catalog and manual checklist state. */
public class MainActivity extends AppCompatActivity {
    private ChecklistCatalog catalog;
    private ChecklistProgress progress;
    private ProgressStore store;
    private String manufacturer;
    private Aircraft aircraft;
    private Checklist checklist;
    private boolean showExtended;
    private Map<String, String> selectedOptions = new LinkedHashMap<>();
    private View root, screen;
    private FrameLayout container;
    private ScrollView scroll;
    private MaterialButton action;
    private final Map<String, View> itemViews = new LinkedHashMap<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.main);
        container = findViewById(R.id.screen_container);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.getWindowInsetsController(root).setAppearanceLightStatusBars(false);
        ViewCompat.getWindowInsetsController(root).setAppearanceLightNavigationBars(false);
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.reset_button).setOnClickListener(v -> confirmReset());
        store = new ProgressStore(this);
        progress = store.load();
        try { catalog = ChecklistCatalog.load(this); }
        catch (Exception error) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.load_error_title)
                    .setMessage(R.string.load_error_message)
                    .setPositiveButton(R.string.close, (d, w) -> finish()).setCancelable(false).show();
            return;
        }
        if (state != null) {
            String savedManufacturer = state.getString("manufacturer");
            if (catalog.manufacturers.contains(savedManufacturer)) manufacturer = savedManufacturer;
            showExtended = state.getBoolean("extended");
            for (Aircraft model : catalog.aircraft)
                if (model.id.equals(state.getString("aircraft"))) aircraft = model;
            if (aircraft != null) { manufacturer = aircraft.manufacturer; selectedOptions = store.loadOptions(aircraft); }
            if (aircraft != null) for (Checklist list : activeChecklists())
                if (list.id.equals(state.getString("checklist"))) checklist = list;
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (checklist != null) { checklist = null; render(); }
                else if (aircraft != null) { aircraft = null; showExtended = false; render(); }
                else if (manufacturer != null) { manufacturer = null; render(); }
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
        render();
        if (state != null) scroll.post(() -> scroll.scrollTo(0, state.getInt("scroll")));
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (manufacturer != null) state.putString("manufacturer", manufacturer);
        if (aircraft != null) state.putString("aircraft", aircraft.id);
        if (checklist != null) state.putString("checklist", checklist.id);
        state.putBoolean("extended", showExtended);
        if (scroll != null) state.putInt("scroll", scroll.getScrollY());
    }

    private void render() {
        container.removeAllViews();
        itemViews.clear();
        root.findViewById(R.id.back_button).setVisibility(manufacturer == null ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.back_button).setContentDescription(getString(checklist != null ? R.string.back_checklists
                : aircraft != null ? R.string.back_aircraft : R.string.back_manufacturers));
        int background = getColor(checklist == null ? R.color.app_background : R.color.checklist_background);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));
        if (manufacturer == null) renderManufacturers();
        else if (aircraft == null) renderAircraft();
        else if (checklist == null) renderChecklists();
        else renderItems();
        ViewCompat.requestApplyInsets(root);
    }

    private void inflateScreen(int layout) {
        screen = getLayoutInflater().inflate(layout, container, false);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) screen.getLayoutParams();
        if (getResources().getConfiguration().screenWidthDp > 800) params.width = dp(800);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        container.addView(screen, params);
        scroll = screen.findViewById(R.id.page_scroll);
        scroll.setSaveEnabled(false); // Navigation and scroll restoration are scoped to the selected screen.
        View source = screen.findViewById(R.id.source_button);
        if (source != null) source.setOnClickListener(v -> showSource(aircraft == null ? catalog.aircraft.get(0) : aircraft));
        View reference = screen.findViewById(R.id.view_checklist_button);
        if (reference != null) {
            reference.setVisibility(aircraft == null || aircraft.references.isEmpty() ? View.GONE : View.VISIBLE);
            reference.setOnClickListener(v -> openReference((showExtended ? aircraft.extendedInitialPage : aircraft.normalInitialPage) - 1));
        }
        View optionsButton = screen.findViewById(R.id.checklist_options_button);
        if (optionsButton != null) {
            optionsButton.setVisibility(aircraft == null || aircraft.options.isEmpty() ? View.GONE : View.VISIBLE);
            optionsButton.setOnClickListener(v -> showChecklistOptions());
        }
        MaterialButton mode = screen.findViewById(R.id.checklist_mode_button);
        if (mode != null) {
            mode.setVisibility(aircraft == null || aircraft.extendedChecklists.isEmpty() ? View.GONE : View.VISIBLE);
            String customMode = aircraft == null ? "" : showExtended ? aircraft.showNormalLabel : aircraft.showExtendedLabel;
            mode.setText(customMode.isEmpty() ? getString(showExtended ? R.string.show_normal : R.string.show_extended) : customMode);
            mode.setOnClickListener(v -> { showExtended = !showExtended; checklist = null; render(); });
        }
    }

    private void renderAircraft() {
        inflateScreen(R.layout.screen_aircraft);
        label(screen, R.id.aircraft_title, manufacturer);
        screen.setAccessibilityPaneTitle(manufacturer);
        LinearLayout rows = screen.findViewById(R.id.aircraft_rows);
        for (Aircraft model : catalog.aircraftFor(manufacturer)) {
            View row = getLayoutInflater().inflate(R.layout.row_aircraft, rows, false);
            row.setTag(model.id);
            label(row, R.id.aircraft_name, model.name);
            label(row, R.id.aircraft_description, getString(R.string.aircraft_description, model.operator,
                    getResources().getQuantityString(R.plurals.checklist_count, model.checklists.size(), model.checklists.size()),
                    getResources().getQuantityString(R.plurals.item_count, model.itemCount(), model.itemCount())));
            row.findViewById(R.id.open_aircraft).setContentDescription(getString(R.string.open_aircraft_description, model.name));
            row.findViewById(R.id.open_aircraft).setOnClickListener(v -> { aircraft = model; selectedOptions = store.loadOptions(model); showExtended = false; render(); });
            rows.addView(row);
        }
    }

    private void renderManufacturers() {
        inflateScreen(R.layout.screen_manufacturers);
        screen.setAccessibilityPaneTitle(getString(R.string.manufacturers_title));
        LinearLayout rows = screen.findViewById(R.id.manufacturer_rows);
        for (String name : catalog.manufacturers) {
            View row = getLayoutInflater().inflate(R.layout.row_manufacturer, rows, false);
            row.setTag(name);
            label(row, R.id.manufacturer_name, name);
            List<String> modelNames = new ArrayList<>();
            for (Aircraft model : catalog.aircraftFor(name)) modelNames.add(model.name);
            label(row, R.id.manufacturer_models, android.text.TextUtils.join(" · ", modelNames));
            View button = row.findViewById(R.id.open_manufacturer);
            button.setContentDescription(getString(R.string.open_manufacturer_description, name));
            button.setOnClickListener(v -> { manufacturer = name; render(); });
            rows.addView(row);
        }
    }

    private void renderChecklists() {
        inflateScreen(R.layout.screen_checklists);
        label(screen, R.id.model_eyebrow, getString(showExtended ? R.string.model_eyebrow_extended : R.string.model_eyebrow, aircraft.operator.toUpperCase(Locale.ROOT)));
        label(screen, R.id.model_title, aircraft.name);
        List<Checklist> lists = activeChecklists();
        TextView modeLabel = screen.findViewById(R.id.checklist_mode_label);
        modeLabel.setVisibility(aircraft.extendedChecklists.isEmpty() ? View.GONE : View.VISIBLE);
        String customLabel = showExtended ? aircraft.extendedModeLabel : aircraft.normalModeLabel;
        modeLabel.setText(customLabel.isEmpty() ? getString(showExtended ? R.string.extended_mode_label : R.string.normal_mode_label) : customLabel);
        View referenceInfo = screen.findViewById(R.id.extended_reference_info);
        boolean hasReferenceInfo = showExtended && !aircraft.extendedReferenceNote.isEmpty();
        referenceInfo.setVisibility(hasReferenceInfo ? View.VISIBLE : View.GONE);
        if (hasReferenceInfo) {
            label(screen, R.id.extended_reference_note, aircraft.extendedReferenceNote);
            screen.findViewById(R.id.extended_reference_button).setOnClickListener(v -> openReference(aircraft.extendedReferencePage - 1));
        }
        LinearLayout rows = screen.findViewById(R.id.phase_rows);
        for (int i = 0; i < lists.size(); i++) {
            Checklist list = lists.get(i);
            View row = getLayoutInflater().inflate(R.layout.row_phase, rows, false);
            row.setTag(list.id);
            boolean complete = isComplete(list);
            row.setSelected(complete);
            label(row, R.id.phase_number, complete ? "✓" : String.format(Locale.ROOT, "%02d", i + 1));
            label(row, R.id.phase_name, list.name);
            label(row, R.id.phase_description, list.alternate ? getString(R.string.alternate_phase_description, list.description) : list.description);
            label(row, R.id.phase_status, visibleItems(list).isEmpty() ? getString(R.string.reference_notes)
                    : progressStatus(list, R.string.phase_status));
            row.setOnClickListener(v -> { checklist = list; render(); });
            rows.addView(row);
        }
    }

    private void renderItems() {
        inflateScreen(R.layout.screen_checklist);
        label(screen, R.id.checklist_aircraft, getString(R.string.checklist_aircraft,
                aircraft.name.toUpperCase(Locale.ROOT), activeChecklists().indexOf(checklist) + 1, activeChecklists().size()));
        TextView legend = screen.findViewById(R.id.checklist_legend);
        legend.setText(aircraft.legend);
        legend.setVisibility(aircraft.legend.isEmpty() ? View.GONE : View.VISIBLE);
        TextView note = screen.findViewById(R.id.checklist_note);
        note.setText(checklist.note);
        note.setVisibility(checklist.note.isEmpty() ? View.GONE : View.VISIBLE);
        label(screen, R.id.checklist_title, checklist.name);
        screen.setAccessibilityPaneTitle(checklist.name);
        LinearLayout rows = screen.findViewById(R.id.item_rows);
        for (ChecklistRow definition : checklist.rows) {
            if (!ChecklistCatalog.matches(definition.when, selectedOptions)) continue;
            if (definition.item == null) {
                TextView text = (TextView) getLayoutInflater().inflate(R.layout.row_checklist_text, rows, false);
                text.setTag(definition.id);
                text.setText(definition.text);
                int style = definition.bold ? (definition.italic ? Typeface.BOLD_ITALIC : Typeface.BOLD)
                        : (definition.italic ? Typeface.ITALIC : Typeface.NORMAL);
                text.setTypeface(Typeface.create("sans-serif", style));
                int textStart = visibleItems(checklist).isEmpty() ? 8 : 48;
                text.setPaddingRelative(dp(textStart + definition.indent), dp(6), dp(8), dp(6));
                text.setGravity(definition.alignment.equals("end") ? Gravity.END : Gravity.START);
                text.setAccessibilityHeading(definition.type.equals("heading"));
                rows.addView(text);
                continue;
            }
            Item item = definition.item;
            if (!item.group.isEmpty()) {
                TextView group = (TextView) getLayoutInflater().inflate(R.layout.row_checklist_group, rows, false);
                group.setText(item.group.toUpperCase(Locale.ROOT));
                rows.addView(group);
            }
            View row = getLayoutInflater().inflate(R.layout.row_checklist_item, rows, false);
            row.setTag(item.id);
            if (item.optional) row.setBackgroundResource(R.drawable.manual_item_background);
            TextView challenge = row.findViewById(R.id.item_challenge);
            TextView response = row.findViewById(R.id.item_response);
            TextView itemNote = row.findViewById(R.id.item_note);
            String marker = item.note.equals("Through flights (*)") ? " *" : item.note.equals("If applicable (**)") ? " **" : "";
            challenge.setText(getString(R.string.item_challenge_with_marker, item.challenge, marker));
            response.setText(item.response);
            String rowNote = marker.isEmpty() ? item.note : "";
            if (item.optional) {
                String kind = getString(item.manualOnly ? R.string.manual_only : R.string.optional_item);
                rowNote = rowNote.isEmpty() ? kind : getString(R.string.optional_item_note, kind, rowNote);
            }
            if (!rowNote.isEmpty()) { itemNote.setText(rowNote); itemNote.setVisibility(View.VISIBLE); }
            row.setTooltipText(item.challenge + ": " + item.response + (item.note.isEmpty() ? "" : "\n" + item.note));
            CheckBox box = row.findViewById(R.id.item_checkbox);
            box.setSaveEnabled(false); // Repeated row IDs must not overwrite the model on rotation.
            box.setContentDescription(item.optional ? getString(item.manualOnly ? R.string.manual_only_description : R.string.optional_item_description, item.challenge, item.response)
                    : item.challenge + ", " + item.response);
            row.setOnClickListener(v -> box.setChecked(!box.isChecked()));
            // Keep both text columns readable on narrow phones, large fonts and landscape.
            // The XML leader takes all remaining space between the natural text widths.
            row.findViewById(R.id.item_line_content).addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (r - l != or - ol) {
                    int max = Math.max(1, (r - l - dp(16)) / 2);
                    challenge.setMaxWidth(max); response.setMaxWidth(max); itemNote.setMaxWidth(max);
                }
            });
            itemViews.put(item.id, row);
            rows.addView(row);
            bindItem(item);
        }
        action = screen.findViewById(R.id.checklist_action);
        action.setOnClickListener(v -> {
            if (visibleItems(checklist).isEmpty()) openNextChecklist();
            else if (checkedCount(checklist) == requiredItems(checklist).size()) completeChecklist();
            else markNext();
        });
        updateChecklistStatus();
    }

    private void bindItem(Item item) {
        View row = itemViews.get(item.id);
        boolean checked = progress.isChecked(aircraft.id, checklist.id, item.id);
        CheckBox box = row.findViewById(R.id.item_checkbox);
        box.setOnCheckedChangeListener(null);
        box.setChecked(checked);
        box.setOnCheckedChangeListener((button, value) -> toggle(item, value, false));
        int color = getColor(checked ? R.color.checklist_green : R.color.checklist_ink);
        ((TextView) row.findViewById(R.id.item_challenge)).setTextColor(color);
        ((TextView) row.findViewById(R.id.item_response)).setTextColor(color);
        row.findViewById(R.id.item_leader).setBackgroundColor(getColor(checked ? R.color.checklist_green : R.color.checklist_line));
    }

    private void updateChecklistStatus() {
        if (visibleItems(checklist).isEmpty()) {
            label(screen, R.id.checklist_status, getString(R.string.reference_notes));
            action.setText(R.string.continue_checklist);
            action.setEnabled(activeChecklists().indexOf(checklist) + 1 < activeChecklists().size());
            return;
        }
        int count = checkedCount(checklist);
        label(screen, R.id.checklist_status, progressStatus(checklist, R.string.checklist_status));
        boolean complete = isComplete(checklist);
        action.setEnabled(!complete);
        action.setAlpha(complete ? 0.55f : 1f);
        action.setText(complete ? R.string.checklist_done : count == requiredItems(checklist).size() ? R.string.mark_checklist : R.string.mark_item);
    }

    private void toggle(Item item, boolean checked, boolean fromAction) {
        View row = itemViews.get(item.id);
        boolean reveal = checked && (fromAction || isBottomVisibleItem(row));
        progress.setChecked(aircraft.id, checklist.id, item.id, checked, !item.optional);
        store.save(progress);
        bindItem(item);
        updateChecklistStatus();
        if (reveal) revealNextUnchecked(item);
    }

    private boolean isBottomVisibleItem(View clicked) {
        int viewportBottom = scroll.getScrollY() + scroll.getHeight() - scroll.getPaddingBottom();
        boolean afterClicked = false;
        for (View row : itemViews.values()) {
            if (row == clicked) { afterClicked = true; continue; }
            if (afterClicked && row.getBottom() <= viewportBottom) return false;
        }
        return true;
    }

    private void revealNextUnchecked(Item checkedItem) {
        int index = visibleItems(checklist).indexOf(checkedItem);
        for (int i = index + 1; i < visibleItems(checklist).size(); i++) {
            Item next = visibleItems(checklist).get(i);
            if (!next.optional && !progress.isChecked(aircraft.id, checklist.id, next.id)) {
                View target = itemViews.get(next.id);
                Checklist current = checklist;
                scroll.post(() -> {
                    if (checklist != current || itemViews.get(next.id) != target) return;
                    int top = target.getTop(), bottom = target.getBottom();
                    int visibleBottom = scroll.getScrollY() + scroll.getHeight() - scroll.getPaddingBottom();
                    if (top < scroll.getScrollY()) scroll.smoothScrollTo(0, Math.max(0, top - dp(8)));
                    else if (bottom + dp(12) > visibleBottom)
                        scroll.smoothScrollTo(0, Math.max(0, bottom + dp(12) - scroll.getHeight() + scroll.getPaddingBottom()));
                });
                return;
            }
        }
    }

    private void markNext() {
        for (Item item : requiredItems(checklist)) {
            if (!progress.isChecked(aircraft.id, checklist.id, item.id)) { toggle(item, true, true); return; }
        }
    }

    private void completeChecklist() {
        List<String> ids = new ArrayList<>();
        for (Item item : requiredItems(checklist)) ids.add(item.id);
        if (!progress.complete(aircraft.id, checklist.id, ids, !visibleItems(checklist).isEmpty())) return;
        store.save(progress);
        if (!openNextChecklist()) updateChecklistStatus();
    }
    private boolean openNextChecklist() {
        List<Checklist> lists = activeChecklists();
        int index = lists.indexOf(checklist);
        int next = index + 1;
        while (next < lists.size() && lists.get(next).alternate) next++;
        if (next < lists.size()) {
            checklist = lists.get(next);
            render();
            return true;
        }
        return false; // The final checklist remains open; never wrap to the first.
    }

    private void confirmReset() {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.reset_title).setMessage(R.string.reset_message)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.reset_all, (dialog, which) -> {
                    progress.resetAll(); store.save(progress); render();
                    Snackbar.make(root, R.string.reset_success, Snackbar.LENGTH_SHORT).show();
                }).show();
    }

    private void showSource(Aircraft model) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.source_notes)
                .setMessage(getString(R.string.source_message, model.source, model.notice, model.legend))
                .setPositiveButton(R.string.got_it, null).show();
    }
    private String status(Checklist list) {
        int count = checkedCount(list);
        return getString(isComplete(list) ? R.string.complete : count == requiredItems(list).size() ? R.string.ready : count > 0 ? R.string.in_progress : R.string.not_started);
    }
    private int checkedCount(Checklist list) {
        int count = 0;
        for (Item item : requiredItems(list)) if (progress.isChecked(aircraft.id, list.id, item.id)) count++;
        return count;
    }
    private boolean isComplete(Checklist list) {
        return progress.isComplete(aircraft.id, list.id) && checkedCount(list) == requiredItems(list).size();
    }
    private String progressStatus(Checklist list, int standardFormat) {
        int optionalCount = visibleItems(list).size() - requiredItems(list).size();
        if (optionalCount == 0) return getString(standardFormat, status(list), checkedCount(list), requiredItems(list).size());
        int optionalChecked = 0;
        for (Item item : visibleItems(list))
            if (item.optional && progress.isChecked(aircraft.id, list.id, item.id)) optionalChecked++;
        boolean manualOnly = true;
        for (Item item : visibleItems(list)) if (item.optional && !item.manualOnly) manualOnly = false;
        return getString(manualOnly ? R.string.required_manual_status : R.string.required_optional_status, status(list), checkedCount(list), requiredItems(list).size(), optionalChecked, optionalCount);
    }
    private List<Item> visibleItems(Checklist list) { return list.itemsFor(selectedOptions, false); }
    private List<Item> requiredItems(Checklist list) { return list.itemsFor(selectedOptions, true); }
    private List<Checklist> activeChecklists() {
        List<Checklist> lists = new ArrayList<>();
        for (Checklist list : aircraft.checklistsFor(showExtended))
            if (ChecklistCatalog.matches(list.when, selectedOptions)) lists.add(list);
        return lists;
    }
    private void showChecklistOptions() {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_checklist_options, null);
        LinearLayout rows = dialog.findViewById(R.id.option_rows);
        Map<ChecklistOption, Spinner> controls = new LinkedHashMap<>();
        for (ChecklistOption option : aircraft.options) {
            View row = getLayoutInflater().inflate(R.layout.row_checklist_option, rows, false);
            label(row, R.id.option_label, option.label);
            Spinner selector = row.findViewById(R.id.option_selector);
            selector.setContentDescription(option.label);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, option.labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            selector.setAdapter(adapter);
            selector.setSelection(option.values.indexOf(selectedOptions.get(option.id)));
            controls.put(option, selector); rows.addView(row);
        }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.checklist_options).setView(dialog)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.apply_options, (d, w) -> {
                    Map<String, String> next = new LinkedHashMap<>();
                    for (Map.Entry<ChecklistOption, Spinner> entry : controls.entrySet())
                        next.put(entry.getKey().id, entry.getKey().values.get(entry.getValue().getSelectedItemPosition()));
                    if (next.equals(selectedOptions)) return;
                    List<Checklist> lists = new ArrayList<>(aircraft.checklists); lists.addAll(aircraft.extendedChecklists);
                    for (Checklist list : lists) {
                        List<Item> before = list.itemsFor(selectedOptions, true), after = list.itemsFor(next, true);
                        if (!before.equals(after) || ChecklistCatalog.matches(list.when, selectedOptions) != ChecklistCatalog.matches(list.when, next))
                            progress.reopen(aircraft.id, list.id);
                    }
                    selectedOptions = next; store.saveOptions(aircraft.id, next); store.save(progress);
                    if (checklist != null && !activeChecklists().contains(checklist)) checklist = null;
                    render();
                }).show();
    }
    private void openReference(int page) {
        startActivity(new Intent(this, DocumentReaderActivity.class)
                .putExtra(DocumentReaderActivity.EXTRA_AIRCRAFT_ID, aircraft.id)
                .putExtra(DocumentReaderActivity.EXTRA_INITIAL_PAGE, Math.max(0, page)));
    }
    private void label(View parent, int id, String value) { ((TextView) parent.findViewById(id)).setText(value); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
