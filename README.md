# Flight Check for Android

Native Java / Material Android app for offline, manual flight-simulator checklists.

## Use

Choose an aircraft, choose a checklist, and tap the item or its checkbox to check/uncheck it. The bottom **Mark item as complete** button checks the first unchecked item. When every item is checked, **Mark checklist as complete** becomes available. Pressing it saves completion and immediately opens the next checklist. Completing the final checklist leaves it open with a disabled **Checklist complete** button. Unchecking an item in a completed checklist reopens it.

Checklist rows follow a compact challenge/response layout: checkbox, challenge, connecting line, and right-aligned response. Long text wraps without truncation. Source markers stay alongside challenges; other applicability notes remain visible underneath. Checking the bottom visible row scrolls the next unchecked item into view. The bottom action also reveals the next unchecked item. Other row taps and unchecking items preserve the reading position.

Progress is saved automatically on the device across restarts and rotations. **Reset all**, available on interactive checklist screens, asks for confirmation and clears items and completion status across all aircraft. Cancel leaves progress intact. The screen stays awake while the app is in use.

## Checklist data

Aircraft definitions and original documents are stored separately:

```text
app/src/main/assets/
  checklists/
    index.json                  # Ordered list of aircraft JSON filenames
    ryanair-737-ng.json          # Aircraft metadata and interactive checklist items
  references/
    ryanair-737-ng/
      normal-checklist.pdf      # Original supplied PDF, unchanged
```

The current aircraft includes Safety Inspection (6), Before Start (38), Before Taxi (12), Shutdown (22), and Secure (7): 85 items transcribed from the supplied `RYANAIR BOEING 737.pdf`. Blank responses, applicability markers, and the Before Start divider are retained. Existing aircraft/checklist/item IDs remain unchanged, preserving saved progress when updating from v1.1.

To add an aircraft:

1. Add `checklists/<model-id>.json` following `ryanair-737-ng.json`. Include `id`, `name`, `operator`, `source`, `notice`, `checklists`, and optionally `references`.
2. Append that filename to the `aircraft` array in `checklists/index.json`. This controls aircraft order.
3. Add original documents under `references/<model-id>/` and list them in the aircraft definition:

```json
"references": [
  {
    "id": "normal-checklist",
    "title": "Normal checklist",
    "path": "references/model-id/normal-checklist.pdf"
  },
  {
    "id": "checklist-image",
    "title": "Checklist image",
    "path": "references/model-id/checklist.png"
  }
]
```

Use stable lowercase aircraft/checklist/item/reference IDs with digits/hyphens. Each checklist contains `id`, `name`, `description`, `items`; each item contains `id`, `challenge`, `response` and optional `note`/`group`. Do not reuse IDs for different content. Document filenames accept letters, digits, underscores and hyphens. Supported reference formats: PDF, PNG, JPG/JPEG and WebP. Aircraft without references hide the viewer button. Rebuild the APK after adding bundled files; no UI code changes are required.

## Original checklist reader

**View checklist** appears above the flight-phase list and above interactive checklist items. It opens the selected aircraft's bundled reference inside a separate native Android screen. Use pinch/double-tap or +/- buttons to zoom, drag to pan, and **Fit page** to reset the view. PDF files have Previous/Next controls and a page counter. Images are single-page documents. If an aircraft has several documents, select one from the dropdown. Back returns to the same interactive checklist and reading position without changing saved checks.

The reader uses Android `PdfRenderer` and `ImageDecoder`, with a shared native `DocumentPageView` for display and gestures. File loading, decoding, rendering, and PDF cleanup run on one background worker. Only the current page is rendered; bitmap dimensions are bounded to 3200px per edge and 8 million pixels. PDF descriptors and temporary cache files are released on reader destruction or document changes. Document/page/zoom state is restored after rotation. Files open offline without another reader app, network access, or storage permissions. Failed loads show an error and retry action.

The source legend (* Through flights, ** If applicable) is informational; the app never automatically skips these items. Fill-in responses remain prompts for the user to verify. Source notes are available in the app. This is a simulator tool; the transcription is not approved for aircraft operation.

Manual interaction reference: https://docs.flybywiresim.com/aircraft/common/flypados3/checklists/

## Build and test

Open this folder in Android Studio with SDK 37 installed and its bundled JDK 25 (per `gradle/gradle-daemon-jvm.properties`). Minimum Android version is 9 / API 28.

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
# With an Android emulator or device connected:
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Layouts and UI code

All screens and reusable rows are XML resources in `app/src/main/res/layout/`:

- `activity_main.xml`: app toolbar, back/reset buttons and screen container.
- `screen_aircraft.xml`, `row_aircraft.xml`: aircraft selection.
- `screen_checklists.xml`, `row_phase.xml`: checklist selection and progress.
- `screen_checklist.xml`: compact checklist header, scrolling rows and pinned outlined action.
- `row_checklist_item.xml`, `row_checklist_group.xml`: challenge/response rows and section dividers.
- `activity_document_reader.xml`: reference viewer toolbar, document selector, viewport and page/zoom controls.

Shared styles, colors and user-facing strings are in `res/values/`. `MainActivity` inflates/binds these layouts and handles navigation and state. Checking an item updates the existing row and status in place, preserving scroll and focus. Text column width limits adapt to the available row width, leaving the XML leader to fill the gap. Aircraft definitions and saved-progress IDs are unchanged, so existing progress survives an app update.

The state tests cover completion gating, reopening, isolation between aircraft/phases, restoring snapshots, and global reset. Device tests cover the bundled catalog, persistence, automatic next-checklist navigation, staying on the final checklist, bottom-row automatic scrolling, rotation, and reset cancellation/confirmation.

Version 1.2 (version code 3). The packaged debug build is copied to `output/flight-check-debug.apk`, with screenshots in `output/screens/`.

Verified on 2026-10-23: all 15 device tests passed on Android 12 / API 31, covering checklist regressions, opening/returning from the reader, zoom/pan/rotation, original and multi-page PDF rendering, PNG/JPEG/WebP decoding, and unreadable references. Android lint: 0 errors, 6 existing dependency-update warnings. The bundled original PDF matches the supplied source by SHA-256.

On Windows, if the unit test JVM fails with a PATH-related `ClassNotFoundException`, remove literal quotes from PATH for the current shell and start a fresh Gradle process:

```powershell
$env:Path = $env:Path.Replace('"', '')
.\gradlew.bat --no-daemon --no-configuration-cache testDebugUnitTest
```
