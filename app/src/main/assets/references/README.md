# Original checklist references

Create a subfolder per aircraft, using the aircraft ID (for example `ryanair-737-ng/`). Put original PDF, PNG, JPG/JPEG, or WebP documents here.

Link each file from that aircraft's JSON in `../checklists/`:

```json
"references": [
  {"id": "normal-checklist", "title": "Normal checklist", "path": "references/ryanair-737-ng/normal-checklist.pdf"}
]
```

Filenames accept letters, digits, underscores and hyphens. The View checklist button opens these bundled files offline in the native reader. Multiple entries are selectable in its dropdown. Rebuild after adding files.
