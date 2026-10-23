# E-Checklist for Android

Native Java / Material Android app for offline, manual flight-simulator checklists.

## Use

Choose a manufacturer, choose an aircraft model, choose a checklist, and tap the item or its checkbox to check/uncheck it. Boeing contains the 737-NG, 737 MAX Family (Ryanair 737-8200 source), 777, 757, and 767; Airbus contains the A320 Family, A330-300, A340-300, A350, and A380; McDonnell Douglas contains the MD-11; ATR contains the ATR 42 / 72. Back moves through checklist, phases, aircraft models, and manufacturers. Rotation restores the selected manufacturer and model. The bottom **Mark item as complete** button checks the first unchecked required item. When every required item is checked, **Mark checklist as complete** becomes available. Pressing it saves completion and immediately opens the next checklist. Completing the final checklist leaves it open with a disabled **Checklist complete** button. Unchecking a required item in a completed checklist reopens it.

Grey **Manual only** rows are tickable and saved independently, but do not block completion. The bottom action skips them. Their checked count is shown separately from required checks. Ticking or unticking them after completion keeps the checklist complete. **Reset all** clears both kinds of ticks across every aircraft.

Rows explicitly marked **Optional** work the same way. **Checklist options**, available for aircraft with alternate paths, selects which rows and procedures apply. Apply saves the choices; Cancel leaves them unchanged. Ticks are retained when changing paths, while affected completed checklists reopen for explicit acknowledgement. Reset all clears ticks and completion, retaining the selected options. Alternate go-around procedures and reference schedules remain selectable in the phase list; automatic advancement skips them.

Checklist rows follow a compact challenge/response layout: checkbox, challenge, connecting line, and right-aligned response. Long text wraps without truncation. Source markers stay alongside challenges; other applicability notes remain visible underneath. Checking the bottom visible row scrolls the next unchecked item into view. The bottom action also reveals the next unchecked item. Other row taps and unchecking items preserve the reading position.

Landscape windows at least 720dp wide use a full-width workspace: checklist buttons on the left, selected checklist items on the right, and a completion button fixed below the items. The two panes scroll independently. The active checklist is highlighted; completed checklists show a checkmark. The sidebar includes Normal/Extended mode, additional categories, applicable checklist options, source notes, and the original document reader. Portrait and narrower windows retain the existing layout. Switching phases or rotating preserves checks, completion, the selected category, and the active checklist.

Page navigation uses a short 220ms slide and fade, reversing direction for Back and Home. Selecting an earlier phase in the same checklist set also reverses the entrance. On wide landscape screens, phase/category changes animate the detail pane while the sidebar stays still. Checklist ticks, favorite toggles, reset, font updates, and restoring an existing page do not trigger page motion. Transitions can be interrupted by another navigation action; they never delay saving progress. In-app motion respects Android's animator setting, and Settings, About, and the reference reader use matching native window transitions governed by the system's transition animation setting.

Progress is saved automatically on the device across restarts and rotations. **Reset all**, available on interactive checklist screens, asks for confirmation and clears items and completion status across all aircraft. Cancel leaves progress intact. The screen stays awake while the app is in use.

## Favorites, recent aircraft and startup

The home screen shows the manufacturer list with **Favorites** and **Recent aircraft** buttons above it. Each button opens a separate page of compact aircraft shortcuts, with an empty-state message when needed. Tap the star on a model card or shortcut row to add or remove a favorite. Favorites are sorted by model name; Recent contains the five most recently opened aircraft, newest first, with no duplicates. Opening a shortcut starts that aircraft's Normal checklist picker and retains existing checks. Back from that aircraft returns to the Favorites or Recent page it came from, including after rotation. Back from either collection returns to manufacturers. **Home** in the toolbar returns directly to the manufacturer list from any page. The introductory slogan and Browse manufacturers scroll button have been removed.

Settings offers three startup choices: **Manufacturer list** (the default home screen), **Favorite aircraft** with a selector for the preferred favorite, or **Last-used checklist**. The last choice restores the selected aircraft, actual checklist, Normal/Extended view or additional category, current branch options, and saved progress. Browsing another aircraft without opening a checklist does not overwrite the last-used checklist. Automatic advancement remembers the next checklist. Startup choices apply when opening a fresh app activity; returning from Settings, rotation, and resuming an existing activity retain the current page.

If a selected favorite is removed, the next available favorite opens; with none, the home screen opens. Missing aircraft, category, checklist, or an inapplicable saved checklist also fall back to home. Favorites, recent history, startup choices, and the last-used checklist are saved offline in `NavigationPreferences`, separately from display settings and checklist progress. Reset all preserves this navigation data.

## Checklist data

Aircraft definitions and original documents are stored separately:

```text
app/src/main/assets/
  checklists/
    index.json                  # Ordered list of aircraft JSON filenames
    ryanair-737-ng.json          # Ryanair 737-NG items
    airbus-a320-family.json      # A319/A320/A321 Normal and Extended items
    ini-a350.json                # A350 items, subheadings and notes
    ryanair-737-max.json         # Ryanair 737-8200 / 737 MAX family items
    mcdonnell-douglas-md11.json  # MD-11 required and optional manual-only items
    fbw-a380x.json               # FlyByWire A380X Full SOP items and source notes
    inibuilds-a330-300.json      # A333 HTML import with AT/power/approach selectors
    pmdg-777.json                # Normal checklist boxes and full procedure flow
    atr-42-72.json               # BritishAvgeek ATR 42/72 checklist
    boeing-757.json              # Shared 757/767 source, independent 757 progress
    boeing-767.json              # Shared 757/767 source, independent 767 progress
    airbus-a340-300.json         # Normal, evacuation, engine procedures, memory items
  references/
    ryanair-737-ng/
      normal-checklist.pdf      # Original supplied PDF, unchanged
    airbus-a320-family/
      normal-extended-v26.pdf    # Full four-page A320 family source
    ini-a350/
      ini-a350.pdf              # Full three-page A350 source, unchanged
    ryanair-737-max/
      normal-checklist.pdf      # Original single-page Ryanair 737-8200 PDF
    mcdonnell-douglas-md11/
      normal-procedures.pdf     # Original two-page MD-11F PDF
    fbw-a380x/
      full-sop-checklist.pdf     # Original three-page FBW A380X PDF
    inibuilds-a330-300/
      normal-checklist.pdf      # Supplied companion PDF, two pages
      original-checklist.html   # Original supplied HTML, unchanged
    pmdg-777/
      checklist-procedures.pdf  # Original seven-page JayDee source
    atr-42-72/
      normal-checklist.pdf      # Original two-page BritishAvgeek source
    boeing-757-767/
      normal-checklist.pdf      # Shared original four-page 757/767 PDF
    airbus-a340-300/
      normal-checklist.pdf      # Original ten-page A340-300 source
```

The Ryanair 737-NG includes Safety Inspection (6), Before Start (38), Before Taxi (12), Shutdown (22), and Secure (7): 85 items transcribed from the supplied `RYANAIR BOEING 737.pdf`. The supplemental cockpit placard adds Before Takeoff (2), After Takeoff (4), Descent (5), Approach (1), and Landing (4) between Before Taxi and Shutdown, for 10 phases and 101 items total. Blank responses, applicability markers, and the Before Start divider are retained. Existing aircraft/checklist/item IDs remain unchanged, preserving saved progress when updating from v1.1.

The Boeing 737 MAX Family entry uses the supplied Ryanair 737-8200 PDF: Safety Inspection (6), Before Start (39), Before Taxi (12), Shutdown (23), and Secure (8), totaling 88 PDF items. The same five supplemental cockpit-placard phases add 16 items between Before Taxi and Shutdown, for 10 phases and 104 items total. It preserves the three ELT checks, through-flight and applicability markers, fill-in prompts, the Before Start divider, and the original wording. The source-specific 737-8200 label appears on the model card and in source notes. MAX progress is independent of the NG, and **View checklist** opens the unchanged original single-page PDF. Both models retain blank fill-in prompts from the supplemental placard; its image is not bundled and the original PDFs do not contain the five new phases.

McDonnell Douglas / MD-11 uses the supplied FedEx MD-11F Normal Procedures, Version 0824V1. Nine phases contain 97 checkbox rows: 71 required and 26 manual-only, identified by grey source shading. A '+' denotes **Complete silently** and does not make a row optional. Crew roles, multi-crew responses, blank prompts, thru-flight amplified-procedure reminders, and manual-system groups are retained. Required/manual counts by phase: Before Start 16/6, After Start 3/7, Before Takeoff 11/2, After Takeoff 6/6, In Range 6/3, Approach 4/0, Before Landing 5/0, After Landing 8/1, Shut Down 12/1. The original two-page PDF includes its administrative approval/date fields in the reader.

The Airbus A320 Family (A319/A320/A321) uses page 1 of the supplied v26 PDF as the default Normal view: 11 phases and 69 items. **Show extended items** opens the separate Extended phase list from pages 2-4: 22 phases and 252 items. **Show normal checklist** switches back. The two views have independent progress, and Reset all clears both for all aircraft. Completing a checklist advances only within the current view. Re-entering the aircraft starts in Normal view; rotation and returning from the PDF reader retain the current view.

The mode button is available above the phase list and in a checklist header. Switching from a checklist returns to the other view's phase picker because the source views have different phase structures. The Taxi section from pages 2 and 3 is combined in source order. Crew roles (B/PF/PM), phase prerequisites, conditional notes, and blank responses are preserved. The original page 4 charts/limits remain reference data in the PDF; a **View reference data (page 4)** button below the Extended phase list opens that page directly.

To add an aircraft:

1. Add `checklists/<model-id>.json` following `ryanair-737-ng.json`. Include `id`, `manufacturer` (for example `Boeing` or `Airbus`), `name`, `operator`, `source`, `notice`, `checklists`, and optionally `references`, `legend`, `extendedChecklists`, `extendedReferenceNote`, and `extendedReferencePage`. Manufacturer groups are generated from this field, in first-appearance order; models are sorted by name within each manufacturer. Use the same manufacturer spelling for all its models. Adding a new manufacturer requires no UI changes.
2. Append that filename to the `aircraft` array in `checklists/index.json`. Manufacturer order follows first appearance; model display order is determined by name.
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

Use stable lowercase aircraft/checklist/item/reference IDs with digits/hyphens. Each checklist contains `id`, `name`, `description`, `items` and an optional phase `note`; each item contains `id`, `challenge`, `response` and optional `note`/`group`. Do not reuse IDs for different content. Document filenames accept letters, digits, underscores and hyphens. Supported reference formats: PDF, PNG, JPG/JPEG and WebP. Aircraft without references hide the viewer button. Rebuild the APK after adding bundled files; no UI code changes are required.

Set an item's `manualOnly` to `true` to make its checkbox optional for completion. It defaults to `false` for existing aircraft. `Checklist.items` contains all checkbox rows, while `Checklist.requiredItems` contains only completion requirements; informational text rows remain separate. Optional-only sections still require explicit completion acknowledgement, and text-only sections continue without creating completion state.

The iniBuilds Airbus A350 includes all three supplied PDF pages: 31 sections, 258 checkable items, and 26 informational text rows. Subheadings such as **IN FMS** and embedded notes retain their source order, capitalization, bold/italic style, indentation, and start/end alignment. They have no checkbox or tap action and do not contribute to progress. The two text-only sections use **Continue to next checklist** without recording checked items or completion. The original three-page PDF is available through **View checklist**.

The FlyByWire Airbus A380 includes the complete three-page FBW A380X Full SOP Checklist: 31 sections, 269 required checkbox rows, and 30 informational text rows. Four-engine fire/start checks, multi-line checklist challenges, source spelling and INOP markers are retained. Subheadings such as **IN FMS**, clearance reminders, the SUPER taxi note, and approach/landing notes preserve their source order and formatting without becoming checkboxes. PRE FLIGHT and Initialise FMS contain only reference text and use **Continue to next checklist**. All three pages are included in the default phase list; **View checklist** opens the unchanged original PDF. Progress is independent of the A320 and A350.

Airbus A330-300 uses the supplied Maverick1994 / iniBuilds HTML: 26 sections and all 280 source challenge/response rows. Adjacent continuation blocks are combined. Options select autothrust on/off, APU now/GPU with APU later, and ILS/RNAV/NPA approach. Only matching rows and option headings are shown and required. The defaults are AT on, APU now, and ILS (264 applicable check rows). The original HTML is bundled for traceability, and the supplied two-page companion PDF opens in the native reader.

Boeing 777 uses JayDee's PMDG 1.37.3 source. Ten normal checklist boxes (38 items) are the default view. **Show full procedures** opens the separate procedure flow, including FMC setup, phase guidance, reference flap schedules, and an alternate go-around procedure. Choose With/Without SimBrief in Checklist options to show the matching FMC setup. Source optional actions remain tickable without blocking completion. The native reader starts on page 4 for normal checklists and page 2 for procedures, with all seven original pages accessible.

ATR 42 / 72 uses the BritishAvgeek v1 summarised checklist: 12 phases, 126 required check rows, and 20 informational headings/reminders. The source's alternating grey bands are row shading and do not make items optional. Hotel Mode, prop-brake, altitude, clearance, and timed shutdown reminders are preserved. The original two-page PDF is included.

An `items` entry may instead use `type: "heading"` or `type: "note"`, with `id`, `text`, and optional `bold`, `italic`, `indent` (dp), and `alignment` (`start`/`end`). Such entries only affect display. See `ini-a350.json` and the checklist folder README for examples.

Boeing 757 and Boeing 767 are separate Boeing model entries, imported from the same supplied `B757-767 Checklist.pdf`. Each contains 17 sections and 202 tickable rows (200 required, 2 explicitly conditional APU rows optional). Grey preparation blocks and black formal checklist boxes are distinct phases. Source selector subheadings and the FMA mode line remain informational. Engine Start has independent First engine and Second engine groups, each repeating all seven source actions in the user’s chosen start order. Fuel Balancing (Fuel leak) remains manually selectable and is skipped during automatic normal progression. The final checklist is Secure. All checks, completion, and reset behavior are scoped independently for the two aircraft. Both entries reference one unchanged shared four-page PDF; the Boeing 757-labelled charts on pages 3-4 remain in the reader.

Airbus A340-300 opens the nine normal checklists from page 1 by default (59 required checks). Three separate category buttons provide **Emergency evacuation** (opens its checklist directly), **Other procedures** (automatic start, independent manual-start checklists for engines 1-4, and the takeoff CG/trim reference), and **Non-normal checklists (memory items only)** (seven procedures from pages 5-10). The memory-items label and description make clear that this is a selected subset. Extra procedures stay on the chosen checklist when completed, rather than advancing to a different emergency or engine-start method. Normal completion still advances through the normal phases and stays on the final phase.

Source ECAM memo bullets, monitoring tables, headings, conditions, notes, and cautions remain non-interactive. Procedure-scoped **Checklist options** select evacuation/pressure branches, braking outcomes, emergency-descent conditions, stall configuration, TCAS availability/RA/AP state, and windshear phase/AP state. Only matching rows are shown and required. Changing options retains ticks, preserves other procedures’ choices, and reopens affected completed checklists. With no applicable items, the action is disabled and the screen says so. Reset all clears ticks and completion across every category. Category/checklist selection survives rotation, and re-entering the aircraft starts in Normal. The unchanged ten-page PDF opens on the relevant source page for each procedure, including the trim chart on page 2.

## Settings and About

The gear in the app toolbar opens **Settings** from any manufacturer, model, phase, or interactive checklist screen. Checklist text sizes are Small (85%), Default (100%), Large (125%), Extra large (150%), and Largest (175%), with an immediate preview. The size applies to item challenges/responses, item notes, source headings, groups, phase notes, and the checklist legend. It combines with the device text size. Returning to the checklist applies the change while retaining the selected phase, ticks, and scroll position.

Appearance offers **Day**, **Night**, and **Use device setting**. Night is the default for existing app appearance. Day/night colors cover navigation, cards, checklist rows, checked/optional states, dialogs, settings, system bars, and reader controls. Original PDF/image pages retain their original colors. Display settings persist separately from checklist progress; Reset all leaves them intact.

**About**, inside Settings, shows the installed app version, author Jerry Cao (JerryFhuurr), and the project GitHub URL. **Open on GitHub** opens the repository in the device browser. Settings and About use native XML layouts in `res/layout/activity_settings.xml` and `activity_about.xml`.

## Original checklist reader

**View checklist** appears above the flight-phase list and above interactive checklist items. It opens the selected aircraft's bundled reference inside a separate native Android screen. For A320 it starts on page 1 in Normal view and page 2 in Extended view; Previous/Next still provide access to all four pages. Use pinch/double-tap or +/- buttons to zoom, drag to pan, and **Fit page** to reset the view. PDF files have Previous/Next controls and a page counter. Images are single-page documents. If an aircraft has several documents, select one from the dropdown. Back returns to the same interactive checklist and reading position without changing saved checks.

The reader uses Android `PdfRenderer` and `ImageDecoder`, with a shared native `DocumentPageView` for display and gestures. File loading, decoding, rendering, and PDF cleanup run on one background worker. Only the current page is rendered; bitmap dimensions are bounded to 3200px per edge and 8 million pixels. PDF descriptors and temporary cache files are released on reader destruction or document changes. Document/page/zoom state is restored after rotation. Files open offline without another reader app, network access, or storage permissions. Failed loads show an error and retry action.

Each aircraft has its own source legend. Ryanair through-flight/applicability markers and Airbus crew-role markers are informational; the app never automatically skips these items. Fill-in responses remain prompts for the user to verify. Source notes are available in the app. This is a simulator tool; the transcription is not approved for aircraft operation.

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

Screens and reusable rows are XML resources in `app/src/main/res/layout/`, with the compact landscape checklist body in `res/layout-w720dp-land/`:

- `activity_main.xml`: app toolbar, back/reset buttons and screen container.
- `screen_manufacturers.xml`, `row_manufacturer.xml`: home, manufacturer selection and Favorites/Recent buttons.
- `screen_aircraft_shortcuts.xml`, `row_home_aircraft.xml`: separate Favorites and Recent pages, shared shortcut rows and empty states.
- `screen_aircraft.xml`, `row_aircraft.xml`: models belonging to the selected manufacturer.
- `screen_checklist_workspace.xml`, `row_sidebar_phase.xml`, `screen_checklist_empty.xml`: landscape sidebar, phase buttons and selection prompt.
- `layout-w720dp-land/screen_checklist.xml`: landscape item pane with a compact header and fixed completion action. `values-w720dp-land/bools.xml` enables the workspace for wide landscape windows.
- `screen_checklists.xml`, `row_phase.xml`: checklist selection and per-phase status.
- `screen_checklist.xml`: compact checklist header, scrolling rows and pinned outlined action.
- `row_checklist_item.xml`, `row_checklist_group.xml`: challenge/response rows and section dividers.
- `row_checklist_text.xml`: non-interactive source subheadings and notes.
- `dialog_checklist_options.xml`, `row_checklist_option.xml`: native saved path selectors.
- `activity_document_reader.xml`: reference viewer toolbar, document selector, viewport and page/zoom controls.

Shared styles, colors and user-facing strings are in `res/values/`. `MainActivity` inflates/binds these layouts and handles navigation and state. Checking an item updates the existing row and status in place, preserving scroll and focus. Text column width limits adapt to the available row width, leaving the XML leader to fill the gap. Aircraft definitions and saved-progress IDs are unchanged, so existing progress survives an app update.

Page motion is defined in `res/animator/`, native activity transitions in `res/anim/`, and shared timing, distance, and window animation style in `res/values/motion.xml`. `PageTransitions` cancels and resets interrupted entrances, including when the activity leaves the foreground.

The state tests cover completion gating, reopening, isolation between aircraft/phases, restoring snapshots, and global reset. Device tests cover the bundled catalog, persistence, automatic next-checklist navigation, staying on the final checklist, bottom-row automatic scrolling, rotation, and reset cancellation/confirmation.

Version 1.17 (version code 19). The packaged debug build is copied to `output/flight-check-debug.apk`, with screenshots in `output/screens/`.

The Settings build was verified on 2026-10-23: all 53 device tests passed on Android 12 / API 31, including the 737 supplement and Settings update. The 8 state unit tests also passed with the aircraft-options update; the state engine is unchanged in the Settings update. New aircraft tests cover source counts and native PDF pages, A330 selected paths and completion reopening, option persistence/rotation/cancellation, reset retaining selections, alternate go-around skipping, B777 normal/full procedures and FMC route filtering, optional F/O checks, and ATR required rows and timing notes. A380 tests cover every section count, four-engine checks, non-clickable formatted notes, text-only sections, completion advancing past IN FMS, rotation and model-specific progress, all three original PDF pages, and global reset. MD-11 tests verify grey-row classification (71 required / 26 optional), clickable optional rows, required-only bottom actions, all nine phases completing with every optional row unchecked, optional ticks surviving rotation, optional unchecking preserving completion, required unchecking reopening it, global reset, and two-page PDF navigation. Existing aircraft tests also pass. MAX tests cover all five source section counts, variant-specific ELT checks and wording, the Before Start divider, progress isolation from the NG, advancement and staying on the final checklist, global reset, and returning from the original PDF without losing checks. Manufacturer navigation tests cover model filtering, toolbar/system back navigation through all levels, restoration after rotation, preserving checks across manufacturer switches, and global reset from the home screen. A350 tests cover the imported catalog, non-clickable formatted subheadings, progress and advancement excluding text rows, text-only sections, rotation, and original PDF navigation. Existing Ryanair NG, A320 Normal/Extended, and PDF/image reader tests pass. Lint: 0 errors (6 existing dependency update warnings and 1 range-typography suggestion). The MAX and A380 transcriptions were reviewed against every source page; JSON `sourcePage`/`sourcePages` fields preserve traceability.

On Windows, if the unit test JVM fails with a PATH-related `ClassNotFoundException`, remove literal quotes from PATH for the current shell and start a fresh Gradle process:

```powershell
$env:Path = $env:Path.Replace('"', '')
.\gradlew.bat --no-daemon --no-configuration-cache testDebugUnitTest
```

Version 1.3.1 removes the aircraft-card completion bar/count and the aggregate completion panel above the phase list. Per-item and per-phase progress are still saved.

737 cockpit-placard supplement verified on 2026-10-23: all 12 NG/MAX device tests pass, including automatic progression from Before Taxi through the five added phases to Shutdown, completion prompts, and independent NG/MAX progress. Both models retain their original PDF references, with no supplied screenshot asset added. Lint remains at 0 errors and 7 existing warnings.

Settings verified on 2026-10-23: the full 53-test device suite passes. Settings tests cover live font preview, saved size and appearance, recreation/restart, checklist ticks and reset isolation, day/night switching in the checklist and native reader, following the device theme, large text wrapping without truncation, non-clickable source notes, and the About author/version/GitHub action. Lint: 0 errors and 7 existing warnings.

757/767 update verified on 2026-10-23: 7 targeted device tests passed (3 new 757/767 flow/reference/isolation tests, 3 manufacturer navigation tests, and the expanded catalog/reference test). The three checklist pages were visually reviewed and all body text matched the transcription in source reading order after ignoring spacing/punctuation. Both JSON definitions contain identical rows under separate aircraft IDs. The shared original PDF matches the supplied bytes. Lint: 0 errors and 7 existing warnings.

A340 update verified on 2026-10-23: all 62 device tests verified through the full regression run and focused reruns of the updated MD-11 catalog assertion and six A340 tests. The MD-11 assertion checks its manual-only subtype while allowing generic conditional rows on other aircraft. A340 tests cover default Normal mode, all three category buttons, per-procedure source pages, normal advancement, staying on a selected memory/engine procedure, category restoration, independent manual-engine progress, evacuation and TCAS/windshear branches, preserving unrelated option choices, conditional memory items, zero applicable items, reset, and the ten-page PDF. Source body text was audited across all ten pages against the transcription. Lint: 0 errors and 7 existing warnings.

Long checklist option values wrap across lines. The A340 TCAS/windshear and existing A330 approach/autothrust selector tests pass after this layout change; both A340 option dialogs were visually checked. The packaged APK matches the final build and retains the supplied PDF byte-for-byte.

Landscape update verified on 2026-10-23: the full 66-test device regression passed 65 checks; one new sidebar-scroll assertion ran before its scroll animation finished. After making the scrolling check deterministic, all four landscape tests passed against the unchanged app build. Coverage includes phase switching, checks and selection across portrait/landscape rotation, independent pane scrolling and automatic item reveal, normal advancement and final-checklist behavior, reset, A340 categories/options/source pages, A320 Normal/Extended charts, and day mode with 175% checklist text. Tablet screenshots at 1920×1200 and 1200×1920 were visually reviewed; the emulator's original display settings were restored. Lint: 0 errors and 7 existing warnings. APK v1.14 matches the final build, with all 12 aircraft and all 11 referenced original documents verified.

Favorites/startup update verified on 2026-10-23: seven new navigation tests pass. The full 73-test device regression passed 71 checks; two existing home assertions assumed manufacturers were within the initial viewport. After adjusting those assertions for the favorites/recent sections above manufacturers, all three manufacturer navigation tests pass against the unchanged app build. All 73 distinct device checks are verified across these runs, with no unresolved failures. Coverage includes favorite persistence/removal and shortcuts, five-entry unique recent history and ordering, all startup settings, chosen-favorite fallback, normal advancement, Extended and A340 memory-category restoration, completed checks, portrait/landscape rotation, reset isolation, and stale saved targets. Visual checks include real force-stop/relaunch of Favorite aircraft and Last-used checklist, and Browse manufacturers navigation. Lint: 0 errors and 7 existing warnings. The v1.15 APK matches the final build and preserves all 12 aircraft and original reference files.

Page transitions verified on 2026-10-23: 40 distinct device checks pass across targeted runs. Checklist, favorites/startup, manufacturer navigation, Settings/About, native reader, A320 modes, and A340 category/option checks passed with system and in-app animation scales at 1. Four landscape tests initially missed their first injected tap immediately after rotation; all four passed with system window/transition animations disabled while in-app animators remained enabled. Manual visual checks covered detail-pane motion with a stationary tablet sidebar, native Settings transitions, disabled-animation navigation, and interrupted back navigation. The emulator's original display and animation settings were restored. Lint: 0 errors and 7 existing warnings. The v1.16 APK matches the final build, with all 12 aircraft and all 13 reference-folder assets unchanged in the package.

E-Checklist home update verified on 2026-10-23: all 27 targeted device tests pass (8 aircraft-navigation tests and 19 checklist/manufacturer/settings/tablet checks). Coverage includes the launcher label and toolbar name, removal of the introductory slogan, separate collection pages and empty states, shortcut Back destinations across rotation, stars, recent ordering, startup behavior, saved ticks, reset, advancement, original-reader return, day/night and large text. Regression checks ran with in-app animations enabled and system window animations disabled. Phone night/day layouts, Favorites, Recent, empty Favorites, About, and tablet home/Favorites were visually reviewed. The original emulator settings were restored. Lint: 0 errors and 7 existing warnings. APK v1.17 matches the final build; all 12 aircraft and all 13 reference-folder assets are retained unchanged.
