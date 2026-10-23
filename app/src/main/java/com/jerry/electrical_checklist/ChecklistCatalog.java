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
import java.util.List;
import java.util.Set;
import java.util.Locale;

/** Aircraft and checklist definitions are bundled data, independent of progress and UI. */
public final class ChecklistCatalog {
    public final List<Aircraft> aircraft;

    private ChecklistCatalog(List<Aircraft> aircraft) {
        this.aircraft = Collections.unmodifiableList(aircraft);
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
            List<ReferenceDocument> references = new ArrayList<>();
            Set<String> referenceIds = new HashSet<>();
            JSONArray documents = model.optJSONArray("references");
            if (documents != null) for (int d = 0; d < documents.length(); d++) {
                JSONObject document = documents.getJSONObject(d);
                references.add(new ReferenceDocument(uniqueId(document, referenceIds),
                        document.getString("title"), document.getString("path")));
            }
            List<Checklist> checklists = parseChecklists(model);
            aircraft.add(new Aircraft(aircraftId, model.getString("name"), model.getString("operator"),
                    model.getString("source"), model.getString("notice"), checklists, references));
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

    private static List<Checklist> parseChecklists(JSONObject model) throws JSONException {
        List<Checklist> checklists = new ArrayList<>();
        Set<String> checklistIds = new HashSet<>();
        JSONArray lists = model.getJSONArray("checklists");
        for (int c = 0; c < lists.length(); c++) {
            JSONObject list = lists.getJSONObject(c);
            String checklistId = uniqueId(list, checklistIds);
            List<Item> items = new ArrayList<>();
            Set<String> itemIds = new HashSet<>();
            JSONArray rows = list.getJSONArray("items");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                items.add(new Item(uniqueId(row, itemIds), row.getString("challenge"),
                        row.getString("response"), row.optString("note"), row.optString("group")));
            }
            if (items.isEmpty()) throw new JSONException("Empty checklist: " + checklistId);
            checklists.add(new Checklist(checklistId, list.getString("name"),
                    list.getString("description"), items));
        }
        if (checklists.isEmpty()) throw new JSONException("Aircraft has no checklists: " + model.getString("id"));
        return checklists;
    }

    private static String uniqueId(JSONObject object, Set<String> seen) throws JSONException {
        String id = object.getString("id");
        if (!id.matches("[a-z0-9-]+") || !seen.add(id)) throw new JSONException("Invalid or duplicate ID: " + id);
        return id;
    }

    public static final class Aircraft {
        public final String id, name, operator, source, notice;
        public final List<Checklist> checklists;
        public final List<ReferenceDocument> references;
        public Aircraft(String id, String name, String operator, String source, String notice,
                        List<Checklist> checklists, List<ReferenceDocument> references) {
            this.id = id; this.name = name; this.operator = operator; this.source = source; this.notice = notice;
            this.checklists = Collections.unmodifiableList(checklists);
            this.references = Collections.unmodifiableList(references);
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
        public final String id, name, description;
        public final List<Item> items;
        public Checklist(String id, String name, String description, List<Item> items) {
            this.id = id; this.name = name; this.description = description;
            this.items = Collections.unmodifiableList(items);
        }
    }

    public static final class Item {
        public final String id, challenge, response, note, group;
        public Item(String id, String challenge, String response, String note, String group) {
            this.id = id; this.challenge = challenge; this.response = response; this.note = note; this.group = group;
        }
    }
}
