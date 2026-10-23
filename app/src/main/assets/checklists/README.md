# Interactive checklist definitions

Keep one JSON file per aircraft (for example `ryanair-737-ng.json`). Add its filename to the ordered `aircraft` array in `index.json`.

Aircraft fields: `id`, `name`, `operator`, `source`, `notice`, `checklists`, optional `references`.
Checklist fields: `id`, `name`, `description`, `items`.
Item fields: `id`, `challenge`, `response`, optional `note`, `group`.

Keep IDs stable to preserve saved progress. Store original PDFs/images in the sibling `references/` folder and link them through `references` entries containing `id`, `title`, and `path`. See the project README for examples.
