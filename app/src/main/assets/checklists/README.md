# Interactive checklist definitions

Keep one JSON file per aircraft (for example `ryanair-737-ng.json`). Add its filename to the ordered `aircraft` array in `index.json`.

Aircraft fields: `id`, `manufacturer`, `name`, `operator`, `source`, `notice`, `checklists`, optional `references`.
Use a consistent manufacturer name such as `Boeing` or `Airbus`. The app generates manufacturer groups from the catalog in first-appearance order and shows only that manufacturer's models after selection. Future models and manufacturers need no navigation code changes.
Checklist fields: `id`, `name`, `description`, `items`.
Item fields: `id`, `challenge`, `response`, optional `note`, `group`.

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

A320 Normal/Extended example: `airbus-a320-family.json`. `checklists` is the default Normal set; optional `extendedChecklists` adds a toggleable Extended set. Checklist IDs must be unique across both sets (prefix Extended IDs with `extended-`). Optional `note` on a checklist holds its phase prerequisite. The aircraft `legend` holds aircraft-specific source markers. `extendedReferenceNote` and the one-based `extendedReferencePage` add a link to reference charts in the PDF. Source-page metadata in the A320 JSON supports review against the supplied four-page document.
