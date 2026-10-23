package com.jerry.electrical_checklist;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Rect;
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
import androidx.appcompat.widget.AppCompatImageButton;
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
import com.jerry.electrical_checklist.ChecklistCatalog.ChecklistSet;

/** XML layouts own presentation; this activity binds the catalog and manual checklist state. */
public class MainActivity extends AppCompatActivity {
    private static final String FAVORITES = "favorites", RECENT = "recent";
    private ChecklistCatalog catalog;
    private ChecklistProgress progress;
    private ProgressStore store;
    private NavigationPreferences navigation;
    private String manufacturer;
    private String aircraftCollection;
    private Aircraft aircraft;
    private Checklist checklist;
    private boolean showExtended;
    private ChecklistSet checklistSet;
    private Map<String, String> selectedOptions = new LinkedHashMap<>();
    private View root, screen;
    private View workspace;
    private FrameLayout container;
    private ScrollView scroll;
    private ScrollView sidebarScroll;
    private String sidebarKey = "";
    private int sidebarPosition;
    private final Map<String, MaterialButton> sidebarButtons = new LinkedHashMap<>();
    private MaterialButton action;
    private float checklistFontScale;
    private final Map<String, View> itemViews = new LinkedHashMap<>();
    private final PageTransitions pageTransitions = new PageTransitions();
    private String renderedPage;
    private String renderedAircraft;
    private String renderedSidebarContext;
    private int renderedChecklistIndex;

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
        DisplaySettings.styleSystemBars(this, root);
        findViewById(R.id.back_button).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.reset_button).setOnClickListener(v -> confirmReset());
        findViewById(R.id.settings_button).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.home_button).setOnClickListener(v -> {
            manufacturer = null; aircraftCollection = null; aircraft = null; checklist = null; checklistSet = null; showExtended = false; render(true);
        });
        checklistFontScale = new DisplaySettings(this).fontScale();
        store = new ProgressStore(this);
        navigation = new NavigationPreferences(this);
        progress = store.load();
        try { catalog = ChecklistCatalog.load(this); }
        catch (Exception error) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.load_error_title)
                    .setMessage(R.string.load_error_message)
                    .setPositiveButton(R.string.close, (d, w) -> finish()).setCancelable(false).show();
            return;
        }
        if (state != null) {
            String savedCollection = state.getString("aircraft_collection");
            if (FAVORITES.equals(savedCollection) || RECENT.equals(savedCollection)) aircraftCollection = savedCollection;
            String savedManufacturer = state.getString("manufacturer");
            if (catalog.manufacturers.contains(savedManufacturer)) manufacturer = savedManufacturer;
            showExtended = state.getBoolean("extended");
            for (Aircraft model : catalog.aircraft)
                if (model.id.equals(state.getString("aircraft"))) aircraft = model;
            if (aircraft != null) { manufacturer = aircraft.manufacturer; selectedOptions = store.loadOptions(aircraft); checklistSet = aircraft.extraSet(state.getString("checklist_set")); }
            if (aircraft != null) for (Checklist list : activeChecklists())
                if (list.id.equals(state.getString("checklist"))) checklist = list;
            sidebarPosition = state.getInt("sidebar_scroll");
            sidebarKey = sidebarContext();
        } else restoreStartup();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (checklist != null) { checklist = null; if (checklistSet != null && checklistSet.directOpen) checklistSet = null; render(true); }
                else if (checklistSet != null) { checklistSet = null; render(true); }
                else if (aircraft != null) { aircraft = null; showExtended = false; if (aircraftCollection != null) manufacturer = null; render(true); }
                else if (manufacturer != null) { manufacturer = null; render(true); }
                else if (aircraftCollection != null) { aircraftCollection = null; render(true); }
                else { setEnabled(false); getOnBackPressedDispatcher().onBackPressed(); }
            }
        });
        render();
        if (state != null) scroll.post(() -> scroll.scrollTo(0, state.getInt("scroll")));
    }

    @Override protected void onResume() {
        super.onResume();
        float selected = new DisplaySettings(this).fontScale();
        if (Float.compare(selected, checklistFontScale) != 0) {
            checklistFontScale = selected;
            if (catalog != null && checklist != null) {
                int position = scroll.getScrollY();
                render();
                scroll.post(() -> scroll.scrollTo(0, position));
            }
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (manufacturer != null) state.putString("manufacturer", manufacturer);
        if (aircraftCollection != null) state.putString("aircraft_collection", aircraftCollection);
        if (aircraft != null) state.putString("aircraft", aircraft.id);
        if (checklist != null) state.putString("checklist", checklist.id);
        state.putBoolean("extended", showExtended);
        if (checklistSet != null) state.putString("checklist_set", checklistSet.id);
        if (scroll != null) state.putInt("scroll", scroll.getScrollY());
        if (sidebarScroll != null) state.putInt("sidebar_scroll", sidebarScroll.getScrollY());
    }

    @Override protected void onStop() {
        pageTransitions.clear();
        super.onStop();
    }

    private void render() { render(false); }

    private void render(boolean backward) {
        pageTransitions.clear();
        String aircraftId = aircraft == null ? "" : aircraft.id;
        String context = sidebarContext();
        String page = aircraftCollection + "/" + manufacturer + "/" + aircraftId + "/" + context + "/" + (checklist == null ? "" : checklist.id);
        int checklistIndex = checklist == null ? -1 : activeChecklists().indexOf(checklist);
        boolean changedPage = renderedPage != null && !renderedPage.equals(page);
        boolean sameAircraft = !aircraftId.isEmpty() && aircraftId.equals(renderedAircraft);
        if (context.equals(renderedSidebarContext) && checklistIndex >= 0 && renderedChecklistIndex >= 0)
            backward = checklistIndex < renderedChecklistIndex;
        if (checklist != null) navigation.saveLastChecklist(aircraft.id, showExtended,
                checklistSet == null ? "" : checklistSet.id, checklist.id);
        String nextSidebarKey = sidebarContext();
        if (!sidebarKey.equals(nextSidebarKey)) sidebarPosition = 0;
        else if (sidebarScroll != null) sidebarPosition = sidebarScroll.getScrollY();
        sidebarKey = nextSidebarKey;
        sidebarScroll = null;
        workspace = null;
        sidebarButtons.clear();
        container.removeAllViews();
        itemViews.clear();
        boolean atHome = manufacturer == null && aircraftCollection == null;
        root.findViewById(R.id.back_button).setVisibility(atHome ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.home_button).setVisibility(atHome ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.back_button).setContentDescription(getString(checklist != null ? R.string.back_checklists
                : checklistSet != null ? R.string.back_normal_checklists : aircraft != null
                ? aircraftCollection == null ? R.string.back_aircraft : FAVORITES.equals(aircraftCollection) ? R.string.back_favorites : R.string.back_recent_aircraft
                : R.string.back_manufacturers));
        int background = getColor(checklist == null ? R.color.app_background : R.color.checklist_background);
        getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(background));
        if (manufacturer == null && aircraftCollection != null) renderAircraftCollection();
        else if (manufacturer == null) renderManufacturers();
        else if (aircraft == null) renderAircraft();
        else if (getResources().getBoolean(R.bool.checklist_two_pane)) renderWorkspace();
        else if (checklist == null) renderChecklists();
        else renderItems();
        ViewCompat.requestApplyInsets(root);
        if (changedPage) {
            View target = workspace != null && sameAircraft
                    ? workspace.findViewById(R.id.checklist_detail_container) : screen;
            pageTransitions.enter(target, backward);
        }
        renderedPage = page;
        renderedAircraft = aircraftId;
        renderedSidebarContext = context;
        renderedChecklistIndex = checklistIndex;
    }

    private void restoreStartup() {
        if (navigation.startupMode().equals(NavigationPreferences.FAVORITE)) {
            Aircraft favorite = navigation.startupFavorite(catalog);
            if (favorite != null) selectAircraft(favorite);
        } else if (navigation.startupMode().equals(NavigationPreferences.LAST_CHECKLIST)) {
            NavigationPreferences.LastChecklist saved = navigation.lastChecklist();
            if (saved == null) return;
            for (Aircraft model : catalog.aircraft) {
                if (!model.id.equals(saved.aircraftId)) continue;
                ChecklistSet category = model.extraSet(saved.categoryId);
                if (!saved.categoryId.isEmpty() && category == null) return;
                Map<String, String> options = store.loadOptions(model);
                boolean extended = category == null && saved.extended && !model.extendedChecklists.isEmpty();
                List<Checklist> lists = category == null ? model.checklistsFor(extended) : category.checklists;
                for (Checklist list : lists) if (list.id.equals(saved.checklistId) && ChecklistCatalog.matches(list.when, options)) {
                    selectAircraft(model); checklistSet = category; showExtended = extended; checklist = list;
                    return;
                }
            }
        }
    }

    private void selectAircraft(Aircraft model) {
        manufacturer = model.manufacturer; aircraft = model; selectedOptions = store.loadOptions(model);
        showExtended = false; checklistSet = null; checklist = null;
        navigation.recordAircraft(model.id);
    }

    private void bindFavorite(AppCompatImageButton button, Aircraft model) {
        boolean favorite = navigation.isFavorite(model.id);
        button.setImageResource(favorite ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        button.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(favorite ? R.color.accent : R.color.muted)));
        button.setContentDescription(getString(favorite ? R.string.remove_favorite : R.string.add_favorite, model.name));
        button.setOnClickListener(v -> {
            navigation.setFavorite(model.id, !favorite);
            int position = scroll.getScrollY();
            render(); scroll.post(() -> scroll.scrollTo(0, position));
        });
    }

    private void bindHomeAircraft(LinearLayout rows, List<Aircraft> models) {
        for (Aircraft model : models) {
            View row = getLayoutInflater().inflate(R.layout.row_home_aircraft, rows, false);
            row.setTag(model.id);
            label(row, R.id.shortcut_aircraft_name, model.name);
            label(row, R.id.shortcut_aircraft_detail, getString(R.string.shortcut_aircraft_detail, model.manufacturer, model.operator));
            View open = row.findViewById(R.id.open_shortcut);
            open.setContentDescription(getString(R.string.open_aircraft_shortcut, model.name));
            open.setOnClickListener(v -> { selectAircraft(model); render(); });
            bindFavorite(row.findViewById(R.id.shortcut_favorite), model);
            rows.addView(row);
        }
    }

    private void inflateScreen(int layout) {
        FrameLayout host = workspace == null ? container : workspace.findViewById(R.id.checklist_detail_container);
        screen = getLayoutInflater().inflate(layout, host, false);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) screen.getLayoutParams();
        if (workspace == null && layout != R.layout.screen_checklist_workspace && getResources().getConfiguration().screenWidthDp > 800) params.width = dp(800);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        host.addView(screen, params);
        scroll = screen.findViewById(R.id.page_scroll);
        if (scroll != null) scroll.setSaveEnabled(false); // Navigation and scroll restoration are scoped to the selected screen.
        View source = screen.findViewById(R.id.source_button);
        if (source != null) source.setOnClickListener(v -> showSource(aircraft == null ? catalog.aircraft.get(0) : aircraft));
        View reference = screen.findViewById(R.id.view_checklist_button);
        if (reference != null) {
            reference.setVisibility(aircraft == null || aircraft.references.isEmpty() ? View.GONE : View.VISIBLE);
            reference.setOnClickListener(v -> openReference(referencePage() - 1));
        }
        View optionsButton = screen.findViewById(R.id.checklist_options_button);
        if (optionsButton != null) {
            optionsButton.setVisibility(aircraft == null || visibleOptions().isEmpty() ? View.GONE : View.VISIBLE);
            optionsButton.setOnClickListener(v -> showChecklistOptions());
        }
        MaterialButton mode = screen.findViewById(R.id.checklist_mode_button);
        if (mode != null) {
            mode.setVisibility(aircraft == null || aircraft.extendedChecklists.isEmpty() ? View.GONE : View.VISIBLE);
            String customMode = aircraft == null ? "" : showExtended ? aircraft.showNormalLabel : aircraft.showExtendedLabel;
            mode.setText(customMode.isEmpty() ? getString(showExtended ? R.string.show_normal : R.string.show_extended) : customMode);
            mode.setOnClickListener(v -> { showExtended = !showExtended; checklistSet = null; checklist = null; render(); });
        }
    }

    private String sidebarContext() {
        return aircraft == null ? "" : aircraft.id + "/" + showExtended + "/" + (checklistSet == null ? "normal" : checklistSet.id);
    }

    private void renderWorkspace() {
        inflateScreen(R.layout.screen_checklist_workspace);
        workspace = screen;
        label(workspace, R.id.sidebar_aircraft, aircraft.name);
        String mode = checklistSet != null ? checklistSet.name : showExtended ? aircraft.extendedModeLabel : aircraft.normalModeLabel;
        label(workspace, R.id.sidebar_mode_label, mode.isEmpty() ? getString(aircraft.extendedChecklists.isEmpty()
                ? R.string.normal_checklists : showExtended ? R.string.extended_mode_label : R.string.normal_mode_label) : mode);
        TextView description = workspace.findViewById(R.id.sidebar_set_description);
        description.setVisibility(checklistSet != null && !checklistSet.description.isEmpty() ? View.VISIBLE : View.GONE);
        if (checklistSet != null) description.setText(checklistSet.description);
        bindCategoryButtons(workspace.findViewById(R.id.checklist_categories));
        bindExtendedReferenceInfo(workspace);
        LinearLayout rows = workspace.findViewById(R.id.sidebar_phase_rows);
        for (Checklist list : activeChecklists()) {
            MaterialButton button = (MaterialButton) getLayoutInflater().inflate(R.layout.row_sidebar_phase, rows, false);
            button.setTag(list.id);
            button.setTooltipText(list.description);
            button.setOnClickListener(v -> { checklist = list; render(); });
            sidebarButtons.put(list.id, button);
            rows.addView(button);
        }
        sidebarScroll = workspace.findViewById(R.id.sidebar_scroll);
        sidebarScroll.setSaveEnabled(false);
        if (checklist == null) inflateScreen(R.layout.screen_checklist_empty);
        else renderItems();
        updateSidebarStatus();
        ScrollView navigation = sidebarScroll;
        MaterialButton selected = checklist == null ? null : sidebarButtons.get(checklist.id);
        int position = sidebarPosition;
        navigation.post(() -> {
            navigation.scrollTo(0, position);
            if (selected == null) return;
            Rect bounds = new Rect();
            selected.getDrawingRect(bounds);
            navigation.offsetDescendantRectToMyCoords(selected, bounds);
            if (bounds.top < navigation.getScrollY()) navigation.scrollTo(0, bounds.top);
            else if (bounds.bottom > navigation.getScrollY() + navigation.getHeight())
                navigation.scrollTo(0, bounds.bottom - navigation.getHeight());
        });
    }

    private void updateSidebarStatus() {
        if (workspace == null) return;
        for (Checklist list : activeChecklists()) {
            MaterialButton button = sidebarButtons.get(list.id);
            if (button == null) continue;
            button.setSelected(list == checklist);
            button.setText(isComplete(list) ? getString(R.string.sidebar_completed_phase, list.name) : list.name);
            String summary = visibleItems(list).isEmpty() ? getString(list.items.isEmpty() ? R.string.reference_notes : R.string.no_applicable_items)
                    : progressStatus(list, R.string.phase_status);
            button.setContentDescription(getString(R.string.sidebar_phase_description, list.name, summary));
        }
    }

    private void bindExtendedReferenceInfo(View parent) {
        View info = parent.findViewById(R.id.extended_reference_info);
        if (info == null) return;
        boolean visible = showExtended && !aircraft.extendedReferenceNote.isEmpty();
        info.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            label(parent, R.id.extended_reference_note, aircraft.extendedReferenceNote);
            parent.findViewById(R.id.extended_reference_button).setOnClickListener(v -> openReference(aircraft.extendedReferencePage - 1));
        }
    }

    private void bindCategoryButtons(LinearLayout categories) {
        if (checklistSet != null) {
            MaterialButton normal = (MaterialButton) getLayoutInflater().inflate(R.layout.row_checklist_category, categories, false);
            normal.setText(R.string.show_normal); normal.setTag("normal-checklists");
            normal.setOnClickListener(v -> { checklistSet = null; checklist = null; showExtended = false; render(); });
            categories.addView(normal);
        }
        for (ChecklistSet set : aircraft.extraSets) {
            if (set == checklistSet) continue;
            MaterialButton button = (MaterialButton) getLayoutInflater().inflate(R.layout.row_checklist_category, categories, false);
            button.setText(set.buttonLabel); button.setTag(set.id);
            button.setOnClickListener(v -> { checklistSet = set; showExtended = false; checklist = set.directOpen ? set.checklists.get(0) : null; render(); });
            categories.addView(button);
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
            row.findViewById(R.id.open_aircraft).setOnClickListener(v -> { selectAircraft(model); render(); });
            bindFavorite(row.findViewById(R.id.aircraft_favorite), model);
            rows.addView(row);
        }
    }

    private void renderManufacturers() {
        inflateScreen(R.layout.screen_manufacturers);
        screen.setAccessibilityPaneTitle(getString(R.string.manufacturers_title));
        screen.findViewById(R.id.open_favorites_button).setOnClickListener(v -> { aircraftCollection = FAVORITES; render(); });
        screen.findViewById(R.id.open_recent_button).setOnClickListener(v -> { aircraftCollection = RECENT; render(); });
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

    private void renderAircraftCollection() {
        inflateScreen(R.layout.screen_aircraft_shortcuts);
        boolean favorite = FAVORITES.equals(aircraftCollection);
        String title = getString(favorite ? R.string.favorites_title : R.string.recent_aircraft_title);
        label(screen, R.id.shortcuts_title, title);
        screen.setAccessibilityPaneTitle(title);
        List<Aircraft> models = favorite ? navigation.favorites(catalog) : navigation.recentAircraft(catalog);
        screen.findViewById(R.id.favorites_empty).setVisibility(favorite && models.isEmpty() ? View.VISIBLE : View.GONE);
        screen.findViewById(R.id.recent_aircraft_empty).setVisibility(!favorite && models.isEmpty() ? View.VISIBLE : View.GONE);
        LinearLayout favorites = screen.findViewById(R.id.favorite_aircraft_rows), recent = screen.findViewById(R.id.recent_aircraft_rows);
        favorites.setVisibility(favorite ? View.VISIBLE : View.GONE);
        recent.setVisibility(favorite ? View.GONE : View.VISIBLE);
        bindHomeAircraft(favorite ? favorites : recent, models);
    }

    private void renderChecklists() {
        inflateScreen(R.layout.screen_checklists);
        label(screen, R.id.model_eyebrow, checklistSet == null ? getString(showExtended ? R.string.model_eyebrow_extended : R.string.model_eyebrow, aircraft.operator.toUpperCase(Locale.ROOT))
                : getString(R.string.model_eyebrow_set, aircraft.operator.toUpperCase(Locale.ROOT), checklistSet.name.toUpperCase(Locale.ROOT)));
        label(screen, R.id.model_title, aircraft.name);
        label(screen, R.id.checklist_picker_hint, getString(checklistSet == null ? R.string.choose_phase : R.string.choose_procedure));
        label(screen, R.id.checklist_picker_heading, getString(checklistSet == null ? R.string.flight_phases : R.string.procedures_heading));
        List<Checklist> lists = activeChecklists();
        TextView modeLabel = screen.findViewById(R.id.checklist_mode_label);
        modeLabel.setVisibility(checklistSet != null || !aircraft.extendedChecklists.isEmpty() ? View.VISIBLE : View.GONE);
        String customLabel = checklistSet != null ? checklistSet.name : showExtended ? aircraft.extendedModeLabel : aircraft.normalModeLabel;
        modeLabel.setText(customLabel.isEmpty() ? getString(showExtended ? R.string.extended_mode_label : R.string.normal_mode_label) : customLabel);
        bindExtendedReferenceInfo(screen);
        TextView setDescription = screen.findViewById(R.id.checklist_set_description);
        setDescription.setVisibility(checklistSet != null && !checklistSet.description.isEmpty() ? View.VISIBLE : View.GONE);
        if (checklistSet != null) setDescription.setText(checklistSet.description);
        bindCategoryButtons(screen.findViewById(R.id.checklist_categories));
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
            label(row, R.id.phase_status, visibleItems(list).isEmpty() ? getString(list.items.isEmpty() ? R.string.reference_notes : R.string.no_applicable_items)
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
        legend.setTextSize(11 * checklistFontScale);
        legend.setVisibility(aircraft.legend.isEmpty() ? View.GONE : View.VISIBLE);
        TextView note = screen.findViewById(R.id.checklist_note);
        note.setText(checklist.note);
        note.setTextSize(12 * checklistFontScale);
        note.setVisibility(checklist.note.isEmpty() ? View.GONE : View.VISIBLE);
        View categoriesButton = screen.findViewById(R.id.checklist_categories_button);
        if (categoriesButton != null) {
            categoriesButton.setVisibility(aircraft.extraSets.isEmpty() ? View.GONE : View.VISIBLE);
            categoriesButton.setOnClickListener(v -> { checklistSet = null; checklist = null; showExtended = false; render(); });
        }
        label(screen, R.id.checklist_title, checklist.name);
        screen.setAccessibilityPaneTitle(checklist.name);
        LinearLayout rows = screen.findViewById(R.id.item_rows);
        for (ChecklistRow definition : checklist.rows) {
            if (!ChecklistCatalog.matches(definition.when, selectedOptions)) continue;
            if (definition.item == null) {
                TextView text = (TextView) getLayoutInflater().inflate(R.layout.row_checklist_text, rows, false);
                text.setTag(definition.id);
                text.setText(definition.text);
                text.setTextSize(12 * checklistFontScale);
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
                group.setTextSize(11 * checklistFontScale);
                rows.addView(group);
            }
            View row = getLayoutInflater().inflate(R.layout.row_checklist_item, rows, false);
            row.setTag(item.id);
            if (item.optional) row.setBackgroundResource(R.drawable.manual_item_background);
            TextView challenge = row.findViewById(R.id.item_challenge);
            TextView response = row.findViewById(R.id.item_response);
            TextView itemNote = row.findViewById(R.id.item_note);
            challenge.setTextSize(12 * checklistFontScale);
            response.setTextSize(12 * checklistFontScale);
            itemNote.setTextSize(11 * checklistFontScale);
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
        updateSidebarStatus();
        if (visibleItems(checklist).isEmpty()) {
            if (!checklist.items.isEmpty()) {
                label(screen, R.id.checklist_status, getString(R.string.no_applicable_items));
                action.setText(R.string.no_applicable_items); action.setEnabled(false); return;
            }
            label(screen, R.id.checklist_status, getString(R.string.reference_notes));
            action.setText(R.string.continue_checklist);
            action.setEnabled((checklistSet == null || checklistSet.autoAdvance) && activeChecklists().indexOf(checklist) + 1 < activeChecklists().size());
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
            if (afterClicked && itemBounds(row).bottom <= viewportBottom) return false;
        }
        return true;
    }

    private Rect itemBounds(View row) {
        Rect bounds = new Rect();
        row.getDrawingRect(bounds);
        scroll.offsetDescendantRectToMyCoords(row, bounds);
        return bounds;
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
                    Rect bounds = itemBounds(target);
                    int top = bounds.top, bottom = bounds.bottom;
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
        if (checklistSet != null && !checklistSet.autoAdvance) return false;
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
        for (Checklist list : checklistSet == null ? aircraft.checklistsFor(showExtended) : checklistSet.checklists)
            if (ChecklistCatalog.matches(list.when, selectedOptions)) lists.add(list);
        return lists;
    }
    private int referencePage() {
        if (checklist != null && checklist.referencePage > 0) return checklist.referencePage;
        return checklistSet != null ? checklistSet.initialPage : showExtended ? aircraft.extendedInitialPage : aircraft.normalInitialPage;
    }
    private List<ChecklistOption> visibleOptions() {
        List<ChecklistOption> options = new ArrayList<>();
        for (ChecklistOption option : aircraft.options)
            if (option.checklistIds.isEmpty() || (checklist != null && option.checklistIds.contains(checklist.id))) options.add(option);
        return options;
    }
    private void showChecklistOptions() {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_checklist_options, null);
        LinearLayout rows = dialog.findViewById(R.id.option_rows);
        Map<ChecklistOption, Spinner> controls = new LinkedHashMap<>();
        for (ChecklistOption option : visibleOptions()) {
            View row = getLayoutInflater().inflate(R.layout.row_checklist_option, rows, false);
            label(row, R.id.option_label, option.label);
            Spinner selector = row.findViewById(R.id.option_selector);
            selector.setContentDescription(option.label);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.row_checklist_option_value, option.labels);
            adapter.setDropDownViewResource(R.layout.row_checklist_option_value);
            selector.setAdapter(adapter);
            selector.setSelection(option.values.indexOf(selectedOptions.get(option.id)));
            controls.put(option, selector); rows.addView(row);
        }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.checklist_options).setView(dialog)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.apply_options, (d, w) -> {
                    Map<String, String> next = new LinkedHashMap<>(selectedOptions);
                    for (Map.Entry<ChecklistOption, Spinner> entry : controls.entrySet())
                        next.put(entry.getKey().id, entry.getKey().values.get(entry.getValue().getSelectedItemPosition()));
                    if (next.equals(selectedOptions)) return;
                    List<Checklist> lists = aircraft.allChecklists();
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
