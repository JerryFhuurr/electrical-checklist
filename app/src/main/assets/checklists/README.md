# Interactive checklist definitions

Keep one JSON file per aircraft (for example `ryanair-737-ng.json`). Add its filename to the ordered `aircraft` array in `index.json`.

Aircraft fields: `id`, `manufacturer`, `name`, `operator`, `source`, `notice`, `checklists`, optional `references`.
Use a consistent manufacturer name such as `Boeing` or `Airbus`. The app generates manufacturer groups from the catalog in first-appearance order and shows only that manufacturer's models after selection. Future models and manufacturers need no navigation code changes.
Checklist fields: `id`, `name`, `description`, `items`.
Item fields: `id`, `challenge`, `response`, optional `note`, `group`, `manualOnly`.

`optional: true` creates a tickable **Optional** row excluded from completion, alongside the existing manual-only subtype. Aircraft `options` define stable IDs, labels, defaults, and value/label pairs. A row or checklist `when` map matches option IDs to selected value IDs; only matching content is shown. See the A330 JSON for power, autothrust, and approach branches, and the 777 JSON for route setup. Unknown option IDs/values are rejected when loading the catalog. Choices are saved per aircraft; changing requirements reopens only affected completions and retains checked items.

`alternate: true` on a checklist keeps it selectable but skips it during automatic next-checklist navigation. Use it for go-around procedures and reference schedules. Optional aircraft `showExtendedLabel`, `showNormalLabel`, `normalModeLabel`, and `extendedModeLabel` customize the two checklist views. `normalInitialPage`/`extendedInitialPage` set the reader's starting page (one-based, default 1/2). These are separate from `extendedReferencePage`, which links a reference chart page.

`manualOnly: true` retains a clickable checkbox but excludes it from completion requirements and the bottom button's automatic checking. Such rows have grey backgrounds and a **Manual only** label. Their checked state is saved and reset normally. Unticking an optional row does not reopen a completed checklist; unticking a required row does. Default `manualOnly` is false, preserving existing aircraft behavior. See the MD-11 JSON for manual-system groups and the distinction between grey manual-only rows and '+' complete-silently markers.

The `items` array also supports non-interactive text rows in source order. These rows do not enter progress counts, saved checks, or completion requirements:

```json
{
  "id": "in-fms",
  "type": "heading",
  "text": "IN FMS",
  "bold": true,
  "italic": false,
  "indent": 28,
  "alignment": "start"
}
```

Use `type: "note"` for body notes. Text retains its capitalization and line breaks. `bold` defaults to true for headings and false for notes; `italic` defaults to false. `indent` adds 0-128dp to the text column's base indentation; `alignment` is `start` (default) or `end`. A section containing only text rows displays **Reference notes** and a **Continue to next checklist** button, disabled on the final section. See `ini-a350.json` for the 31-section, three-page A350 example.

Keep IDs stable to preserve saved progress. Store original PDFs/images in the sibling `references/` folder and link them through `references` entries containing `id`, `title`, and `path`. See the project README for examples.

A380 Full SOP example: `fbw-a380x.json`. All three source pages are represented in its default 31-section list: 269 required checkbox rows and 30 non-interactive heading/note rows. Four-engine start checks and source-specific notes are separate from the A350 catalog and saved progress. Original source-page metadata is retained on each row and section.

A320 Normal/Extended example: `airbus-a320-family.json`. `checklists` is the default Normal set; optional `extendedChecklists` adds a toggleable Extended set. Checklist IDs must be unique across both sets (prefix Extended IDs with `extended-`). Optional `note` on a checklist holds its phase prerequisite. The aircraft `legend` holds aircraft-specific source markers. `extendedReferenceNote` and the one-based `extendedReferencePage` add a link to reference charts in the PDF. Source-page metadata in the A320 JSON supports review against the supplied four-page document.
