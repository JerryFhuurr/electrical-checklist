package com.jerry.electrical_checklist;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;

/** Aircraft and checklist definitions are bundled data, independent of progress and UI. */
public final class ChecklistCatalog {
    public final List<Aircraft> aircraft;
    public final List<String> manufacturers;

    private ChecklistCatalog(List<Aircraft> aircraft) {
        this.aircraft = Collections.unmodifiableList(aircraft);
        Set<String> names = new LinkedHashSet<>();
        for (Aircraft model : aircraft) names.add(model.manufacturer);
        this.manufacturers = Collections.unmodifiableList(new ArrayList<>(names));
    }

    public List<Aircraft> aircraftFor(String manufacturer) {
        List<Aircraft> models = new ArrayList<>();
        for (Aircraft model : aircraft) if (model.manufacturer.equals(manufacturer)) models.add(model);
        return Collections.unmodifiableList(models);
    }

    public static ChecklistCatalog load(Context context) throws IOException, JSONException {
        JSONObject index = readJson(context, "checklists/index.json");
        if (index.getInt("schemaVersion") != 1) throw new JSONException("Unsupported catalog schema");
        JSONArray definitions = index.getJSONArray("aircraft");
        List<Aircraft> aircraft = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int a = 0; a < definitions.length(); a++) {
            String filename = definitions.getString(a);
            if (!filename.matches("[a-z0-9-]+\\.json")) throw new JSONException("Invalid aircraft filename");
            JSONObject model = readJson(context, "checklists/" + filename);
            String aircraftId = uniqueId(model, keys);
            String manufacturer = model.getString("manufacturer").trim();
            if (manufacturer.isEmpty()) throw new JSONException("Aircraft has no manufacturer: " + aircraftId);
            List<ReferenceDocument> references = new ArrayList<>();
            Set<String> referenceIds = new HashSet<>();
            JSONArray documents = model.optJSONArray("references");
            if (documents != null) for (int d = 0; d < documents.length(); d++) {
                JSONObject document = documents.getJSONObject(d);
                references.add(new ReferenceDocument(uniqueId(document, referenceIds),
                        document.getString("title"), document.getString("path")));
            }
            Set<String> checklistIds = new HashSet<>();
            List<Checklist> checklists = parseChecklists(model, "checklists", checklistIds);
            List<Checklist> extended = parseChecklists(model, "extendedChecklists", checklistIds);
            aircraft.add(new Aircraft(aircraftId, manufacturer, model.getString("name"), model.getString("operator"),
                    model.getString("source"), model.getString("notice"), model.optString("legend"),
                    checklists, extended, references, model.optString("extendedReferenceNote"),
                    model.optInt("extendedReferencePage", 0)));
        }
        if (aircraft.isEmpty()) throw new JSONException("No aircraft in catalog");
        return new ChecklistCatalog(aircraft);
    }

    private static JSONObject readJson(Context context, String path) throws IOException, JSONException {
        try (InputStream stream = context.getAssets().open(path)) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = stream.read(buffer)) != -1) bytes.write(buffer, 0, length);
            return new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
        }
    }

    private static List<Checklist> parseChecklists(JSONObject model, String field, Set<String> checklistIds) throws JSONException {
        List<Checklist> checklists = new ArrayList<>();
        JSONArray lists = model.optJSONArray(field);
        if (lists == null && field.equals("extendedChecklists")) return checklists;
        if (lists == null) throw new JSONException("Aircraft has no checklists");
        for (int c = 0; c < lists.length(); c++) {
            JSONObject list = lists.getJSONObject(c);
            String checklistId = uniqueId(list, checklistIds);
            List<Item> items = new ArrayList<>();
            List<ChecklistRow> displayRows = new ArrayList<>();
            Set<String> itemIds = new HashSet<>();
            JSONArray rows = list.getJSONArray("items");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String rowId = uniqueId(row, itemIds);
                String type = row.optString("type", "item");
                if (type.equals("item")) {
                    Item item = new Item(rowId, row.getString("challenge"), row.getString("response"),
                            row.optString("note"), row.optString("group"), row.optBoolean("manualOnly"));
                    items.add(item);
                    displayRows.add(new ChecklistRow(item));
                } else if (type.equals("heading") || type.equals("note")) {
                    String text = row.getString("text");
                    if (text.trim().isEmpty()) throw new JSONException("Empty text row: " + rowId);
                    displayRows.add(new ChecklistRow(rowId, type, text,
                            row.optBoolean("bold", type.equals("heading")), row.optBoolean("italic"),
                            Math.max(0, Math.min(128, row.optInt("indent"))), row.optString("alignment", "start")));
                } else throw new JSONException("Unknown checklist row type: " + type);
            }
            if (displayRows.isEmpty()) throw new JSONException("Empty checklist: " + checklistId);
            checklists.add(new Checklist(checklistId, list.getString("name"),
                    list.getString("description"), list.optString("note"), items, displayRows));
        }
        if (checklists.isEmpty() && field.equals("checklists"))
            throw new JSONException("Aircraft has no checklists: " + model.getString("id"));
        return checklists;
    }

    private static String uniqueId(JSONObject object, Set<String> seen) throws JSONException {
        String id = object.getString("id");
        if (!id.matches("[a-z0-9-]+") || !seen.add(id)) throw new JSONException("Invalid or duplicate ID: " + id);
        return id;
    }

    public static final class Aircraft {
        public final String id, manufacturer, name, operator, source, notice, legend, extendedReferenceNote;
        public final int extendedReferencePage;
        public final List<Checklist> checklists, extendedChecklists;
        public final List<ReferenceDocument> references;
        public Aircraft(String id, String manufacturer, String name, String operator, String source, String notice, String legend,
                        List<Checklist> checklists, List<Checklist> extended, List<ReferenceDocument> references,
                        String extendedReferenceNote, int extendedReferencePage) {
            this.id = id; this.name = name; this.operator = operator; this.source = source; this.notice = notice;
            this.manufacturer = manufacturer;
            this.checklists = Collections.unmodifiableList(checklists);
            this.references = Collections.unmodifiableList(references);
            this.legend = legend;
            this.extendedChecklists = Collections.unmodifiableList(extended);
            this.extendedReferenceNote = extendedReferenceNote;
            this.extendedReferencePage = extendedReferencePage;
        }
        public List<Checklist> checklistsFor(boolean extended) {
            return extended && !extendedChecklists.isEmpty() ? extendedChecklists : checklists;
        }
        public int itemCount() {
            int count = 0;
            for (Checklist checklist : checklists) count += checklist.items.size();
            return count;
        }
    }

    public static final class ReferenceDocument {
        public final String id, title, path;
        public ReferenceDocument(String id, String title, String path) throws JSONException {
            if (!path.matches("references/[a-z0-9-]+/[a-zA-Z0-9_-]+\\.(?i:pdf|png|jpg|jpeg|webp)"))
                throw new JSONException("Invalid reference path: " + path);
            this.id = id; this.title = title; this.path = path;
        }
        public boolean isPdf() { return path.toLowerCase(Locale.ROOT).endsWith(".pdf"); }
    }

    public static final class Checklist {
        public final String id, name, description, note;
        public final List<Item> items, requiredItems;
        public final List<ChecklistRow> rows;
        public Checklist(String id, String name, String description, String note, List<Item> items, List<ChecklistRow> rows) {
            this.id = id; this.name = name; this.description = description;
            this.items = Collections.unmodifiableList(items);
            List<Item> required = new ArrayList<>();
            for (Item item : items) if (!item.manualOnly) required.add(item);
            this.requiredItems = Collections.unmodifiableList(required);
            this.rows = Collections.unmodifiableList(rows);
            this.note = note;
        }
    }

    /** Text rows keep source formatting and ordering but never enter the checkable item list. */
    public static final class ChecklistRow {
        public final String id, type, text, alignment;
        public final Item item;
        public final boolean bold, italic;
        public final int indent;
        private ChecklistRow(Item item) {
            this.id = item.id; this.type = "item"; this.item = item;
            this.text = ""; this.alignment = "start"; this.bold = false; this.italic = false; this.indent = 0;
        }
        private ChecklistRow(String id, String type, String text, boolean bold, boolean italic, int indent, String alignment) {
            this.id = id; this.type = type; this.text = text; this.bold = bold; this.italic = italic;
            this.indent = indent; this.alignment = alignment; this.item = null;
        }
    }

    public static final class Item {
        public final String id, challenge, response, note, group;
        public final boolean manualOnly;
        public Item(String id, String challenge, String response, String note, String group, boolean manualOnly) {
            this.id = id; this.challenge = challenge; this.response = response; this.note = note; this.group = group;
            this.manualOnly = manualOnly;
        }
    }
}
