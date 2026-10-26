# E-Checklist full test report

**Date:** 26 October 2026 (Europe/Paris)

**Build:** 1.18 / version code 20

**Change under test:** the selected aircraft model replaces the app name in the toolbar on aircraft checklist screens; other navigation pages retain E-Checklist.

## Result

- **JVM unit tests:** 8 cases, 0 failures, 0 errors.
- **Complete device regression:** 79/79 passed; 0 failed/error/skipped cases.
- **Additional UI checks:** 10/10 passed.
- **Signed release checks:** 8/8 passed.
- **Builds:** Debug APK, instrumentation APK and optimized Release APK built successfully.
- **Lint:** 0 errors and 7 existing warnings in each variant (six dependency-version suggestions and one range-typography suggestion).
- **Bundled content:** all 27 source assets match both APKs; 12 aircraft entries, 4 manufacturers and 11 referenced PDFs.

These results describe the tested cases and environment. They do not establish that every possible bug or device-specific issue has been eliminated.

## Environment and execution

| Item | Configuration |
| --- | --- |
| OS / device | Android 12, API 31, x86_64; Medium_Phone_API_31 emulator |
| Isolation | Started with `-read-only -no-snapshot-save -no-window`; only emulator-5554 was addressed |
| Default phone | 1080 × 2400 px, 420 dpi (about 411dp wide) |
| Narrow phone | 960 × 1920 px, 480 dpi (320dp wide) |
| Tablet landscape / portrait | 1920 × 1200 / 1200 × 1920 px, 240 dpi |
| Appearance / text | Night and Day; default and 175% checklist text |
| Animation settings | Initial 77-case run used in-app animator scale 1; final 79-case run used all animation scales 0 for reliable replay; visible motion was also checked manually |
| Network | Airplane mode explicitly enabled for offline-reader checks |
| Source baseline | 0d551af plus the toolbar/test changes in this working tree |

The device runner completed the entire package, checked the expected case count, and saved individual results. No physical phone was installed to or reset. The read-only emulator is discarded after testing; the existing saved AVD is retained.

Commands used:

```powershell
.\gradlew.bat --no-configuration-cache testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug lintRelease
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e package com.jerry.electrical_checklist com.jerry.electrical_checklist.test/androidx.test.runner.AndroidJUnitRunner
powershell -NoProfile -ExecutionPolicy Bypass -File .signing/sign-release.ps1
```

## JVM unit cases

| ID | Case | Result |
| --- | --- | --- |
| U01 | [Optional Only Checklist Still Needs Explicit Acknowledgement And Real Rows](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L80) | PASS |
| U02 | [Reopening After Option Change Preserves Ticks And Other Aircraft Completion](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L55) | PASS |
| U03 | [Snapshots Can Be Saved And Restored Without Sharing Mutable Sets](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L43) | PASS |
| U04 | [Completion Requires Every Item And Explicit Acknowledgement](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L14) | PASS |
| U05 | [Reset Clears All Aircraft And Completions](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L33) | PASS |
| U06 | [Empty Checklist Cannot Be Completed](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L52) | PASS |
| U07 | [Aircraft And Phases Have Independent Progress](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L27) | PASS |
| U08 | [Optional Checks Do Not Satisfy Requirements Or Reopen Completed Checklist](../app/src/test/java/com/jerry/electrical_checklist/ChecklistProgressTest.java#L66) | PASS |

## Complete device cases

Total run time: 678.4 seconds. Each linked case points to the assertions that were executed.

### A340ChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D01 | [Normal Is Default And Category Buttons Open Relevant Source Pages](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L60) | PASSED | 22.16 |
| D02 | [Memory Conditions Keep Notes And Zero Applicable Items Are Not Completed](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L116) | PASSED | 20.03 |
| D03 | [Tcas And Windshear Options Are Scoped And Preserve Other Selections](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L101) | PASSED | 14.30 |
| D04 | [Manual Engine Progress Is Independent And Automatic Start Includes All Four Engines](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L127) | PASSED | 18.73 |
| D05 | [Evacuation Branches Are Exclusive And Only Affected Completion Reopens](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L88) | PASSED | 16.70 |
| D06 | [Normal Advances And Memory Completion Stays Scoped After Rotation And Reset](../app/src/androidTest/java/com/jerry/electrical_checklist/A340ChecklistTest.java#L76) | PASSED | 16.16 |

### A350ChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D07 | [In Fms Is Bold Non Clickable And Skipped During Completion](../app/src/androidTest/java/com/jerry/electrical_checklist/A350ChecklistTest.java#L66) | PASSED | 7.19 |
| D08 | [Notes Keep Original Case Italics Alignment And Formatting After Rotation](../app/src/androidTest/java/com/jerry/electrical_checklist/A350ChecklistTest.java#L105) | PASSED | 4.33 |
| D09 | [Catalog Separates Checkable Items And Ordered Text Rows](../app/src/androidTest/java/com/jerry/electrical_checklist/A350ChecklistTest.java#L42) | PASSED | 2.77 |
| D10 | [Original Three Page Pdf Opens And Returns To Same Checklist](../app/src/androidTest/java/com/jerry/electrical_checklist/A350ChecklistTest.java#L128) | PASSED | 4.80 |
| D11 | [Text Only Sections Continue Without Creating Checked Items Or Completion](../app/src/androidTest/java/com/jerry/electrical_checklist/A350ChecklistTest.java#L91) | PASSED | 4.19 |

### A380ChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D12 | [Text Only Sections Continue And In Fms Heading Never Counts As An Item](../app/src/androidTest/java/com/jerry/electrical_checklist/A380ChecklistTest.java#L74) | PASSED | 9.03 |
| D13 | [Catalog Includes All Three Pages Four Engine Checks And Formatted Notes](../app/src/androidTest/java/com/jerry/electrical_checklist/A380ChecklistTest.java#L41) | PASSED | 1.92 |
| D14 | [Engine Checks Persist After Rotation Advance And Stay Separate From Other Models](../app/src/androidTest/java/com/jerry/electrical_checklist/A380ChecklistTest.java#L100) | PASSED | 5.75 |
| D15 | [Original Three Page Pdf Returns Without Losing A380 Checks And Reset Clears Them](../app/src/androidTest/java/com/jerry/electrical_checklist/A380ChecklistTest.java#L119) | PASSED | 5.92 |

### AirbusChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D16 | [Normal And Extended Progress Stay Independent And Global Reset Clears Both](../app/src/androidTest/java/com/jerry/electrical_checklist/AirbusChecklistTest.java#L73) | PASSED | 5.50 |
| D17 | [Reader Opens Page One For Normal And Page Two For Extended And Navigates All Pages](../app/src/androidTest/java/com/jerry/electrical_checklist/AirbusChecklistTest.java#L130) | PASSED | 5.33 |
| D18 | [Normal Is Default And Reentering Aircraft Returns To Normal](../app/src/androidTest/java/com/jerry/electrical_checklist/AirbusChecklistTest.java#L59) | PASSED | 3.08 |
| D19 | [Extended Completion Advances Within Extended And Rotation Restores Mode](../app/src/androidTest/java/com/jerry/electrical_checklist/AirbusChecklistTest.java#L91) | PASSED | 6.06 |
| D20 | [Catalog Contains Normal And Extended Items With Distinct Ids And Four Page Reference](../app/src/androidTest/java/com/jerry/electrical_checklist/AirbusChecklistTest.java#L39) | PASSED | 1.50 |

### AircraftNavigationTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D21 | [Last Startup Resumes Next Checklist And Manufacturer Setting Opens Home](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L208) | PASSED | 10.39 |
| D22 | [Favorites Persist And Quick Links Keep Progress While Reset Preserves Preferences](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L115) | PASSED | 8.58 |
| D23 | [Favorite Startup Chooses Configured Plane And Rotation Keeps Current Navigation](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L190) | PASSED | 8.38 |
| D24 | [Recents Are Unique Most Recent First And Limited To Five](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L142) | PASSED | 4.67 |
| D25 | [Collection Pages Have Empty States And Restore Their Back Destination After Rotation](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L158) | PASSED | 9.55 |
| D26 | [Missing Or Stale Startup Targets Fall Back To Home](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L244) | PASSED | 6.31 |
| D27 | [Removing Startup Favorite Uses Remaining Favorite Then Falls Back To Home](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L255) | PASSED | 5.55 |
| D28 | [Last Startup Restores Extended And Memory Category Including Completion](../app/src/androidTest/java/com/jerry/electrical_checklist/AircraftNavigationTest.java#L222) | PASSED | 9.64 |

### Boeing757767Test

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D29 | [Native Reference Shows Four Pages For Each Model And Preserves Own Ticks](../app/src/androidTest/java/com/jerry/electrical_checklist/Boeing757767Test.java#L60) | PASSED | 9.56 |
| D30 | [Both Engine Groups Required And Saved Separately Across Models And Rotation](../app/src/androidTest/java/com/jerry/electrical_checklist/Boeing757767Test.java#L38) | PASSED | 10.91 |
| D31 | [Normal Flow Skips Fuel Alternate And Both Final Checklists Stay Open Then Reset](../app/src/androidTest/java/com/jerry/electrical_checklist/Boeing757767Test.java#L84) | PASSED | 24.95 |

### BoeingMaxChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D32 | [Catalog Matches8200 Source Including Elt And Below Line Divider](../app/src/androidTest/java/com/jerry/electrical_checklist/BoeingMaxChecklistTest.java#L36) | PASSED | 1.89 |
| D33 | [Max Advances Separately From Ng And Final Checklist Stays Open](../app/src/androidTest/java/com/jerry/electrical_checklist/BoeingMaxChecklistTest.java#L67) | PASSED | 11.52 |
| D34 | [Original8200 Pdf Returns To Same Max Checklist And Checks](../app/src/androidTest/java/com/jerry/electrical_checklist/BoeingMaxChecklistTest.java#L116) | PASSED | 3.97 |
| D35 | [Supplemental Flight Phases Advance In Order And Keep Ng Max Progress Separate](../app/src/androidTest/java/com/jerry/electrical_checklist/BoeingMaxChecklistTest.java#L92) | PASSED | 32.11 |

### ChecklistAppTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D36 | [Bundled Catalog Has All Source Sections And Items](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L49) | PASSED | 0.88 |
| D37 | [Progress Survives Activity Restart And Stays Scoped To Phase](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L173) | PASSED | 5.05 |
| D38 | [Completing Checklist Advances And Unchecking Reopens It](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L78) | PASSED | 5.14 |
| D39 | [Checking Bottom Visible Row Reveals Next Without Scrolling For Top Row](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L107) | PASSED | 2.61 |
| D40 | [Reset Can Be Cancelled And Then Clears Saved Progress](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L161) | PASSED | 4.05 |
| D41 | [Completing Last Checklist Stays On Last And Can Be Reopened](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L92) | PASSED | 4.89 |
| D42 | [Original Checklist Opens From Items And Returns Without Losing Progress](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L61) | PASSED | 3.41 |
| D43 | [Rotation Preserves Selected Checklist And Independent Checkbox Values](../app/src/androidTest/java/com/jerry/electrical_checklist/ChecklistAppTest.java#L148) | PASSED | 2.48 |

### DocumentReaderTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D44 | [Rotation Restores The Document And Zoom](../app/src/androidTest/java/com/jerry/electrical_checklist/DocumentReaderTest.java#L73) | PASSED | 1.74 |
| D45 | [Invalid Aircraft Has An Error And Working Back Button](../app/src/androidTest/java/com/jerry/electrical_checklist/DocumentReaderTest.java#L86) | PASSED | 2.34 |
| D46 | [Pdf Zoom Pan And Fit Work With Single Page Navigation Disabled](../app/src/androidTest/java/com/jerry/electrical_checklist/DocumentReaderTest.java#L54) | PASSED | 2.55 |

### LandscapeChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D47 | [Switching Phases And Rotation Preserve Checks And Selection](../app/src/androidTest/java/com/jerry/electrical_checklist/LandscapeChecklistTest.java#L95) | PASSED | 7.53 |
| D48 | [Extended Mode And Large Text Remain Available After Theme Change](../app/src/androidTest/java/com/jerry/electrical_checklist/LandscapeChecklistTest.java#L199) | PASSED | 11.47 |
| D49 | [A340 Categories Options And Source Reader Work From Sidebar](../app/src/androidTest/java/com/jerry/electrical_checklist/LandscapeChecklistTest.java#L174) | PASSED | 15.28 |
| D50 | [Advancement And Independent Scrolling Keep Both Panes Usable](../app/src/androidTest/java/com/jerry/electrical_checklist/LandscapeChecklistTest.java#L119) | PASSED | 26.58 |

### ManufacturerNavigationTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D51 | [Switching Manufacturers Preserves Checks And Home Reset Clears Every Group](../app/src/androidTest/java/com/jerry/electrical_checklist/ManufacturerNavigationTest.java#L92) | PASSED | 7.74 |
| D52 | [Rotation Restores Manufacturer And Back From Restored Checklist Keeps Its Group](../app/src/androidTest/java/com/jerry/electrical_checklist/ManufacturerNavigationTest.java#L75) | PASSED | 3.62 |
| D53 | [Manufacturer Selection Filters Models And Back Returns Through Each Level](../app/src/androidTest/java/com/jerry/electrical_checklist/ManufacturerNavigationTest.java#L49) | PASSED | 4.17 |

### Md11ChecklistTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D54 | [Optional Ticks Survive Rotation And Do Not Reopen Completed Checklist](../app/src/androidTest/java/com/jerry/electrical_checklist/Md11ChecklistTest.java#L92) | PASSED | 9.81 |
| D55 | [Original Pdf And Reset Preserve Then Clear Both Kinds Of Ticks](../app/src/androidTest/java/com/jerry/electrical_checklist/Md11ChecklistTest.java#L130) | PASSED | 7.19 |
| D56 | [Manual Rows Are Clickable But Button Skips Them And Completion Ignores Them](../app/src/androidTest/java/com/jerry/electrical_checklist/Md11ChecklistTest.java#L76) | PASSED | 8.77 |
| D57 | [Catalog Matches Source And Only Grey Rows Are Optional](../app/src/androidTest/java/com/jerry/electrical_checklist/Md11ChecklistTest.java#L42) | PASSED | 2.19 |
| D58 | [All Phases Complete With Every Manual Row Unchecked And Last Stays Open](../app/src/androidTest/java/com/jerry/electrical_checklist/Md11ChecklistTest.java#L113) | PASSED | 33.62 |

### NavigationLifecycleTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D59 | [Launcher Resume After Home Back Keeps System And Toolbar Back Working](../app/src/androidTest/java/com/jerry/electrical_checklist/NavigationLifecycleTest.java#L48) | PASSED | 6.62 |

### NewModelsTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D60 | [A330 Options And Checks Survive Rotation Restart And Cancel While Reset Only Clears Ticks](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L113) | PASSED | 10.45 |
| D61 | [Catalogs And Original References Match All Three Sources](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L60) | PASSED | 0.97 |
| D62 | [A330 Power Selector Requires Only Chosen Path And Reopens Affected Completion](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L82) | PASSED | 10.75 |
| D63 | [Boeing Normal And Full Procedures Use Chosen Route And Skip Reference Schedules](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L139) | PASSED | 14.98 |
| D64 | [Normal Landing Skips Go Around But Alternate Remains Selectable](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L131) | PASSED | 6.25 |
| D65 | [Atr Uses Required Checks Despite Grey Source Rows And Keeps Timing Notes](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L177) | PASSED | 10.97 |
| D66 | [Boeing Original Reader Starts On Relevant Page And Optional Fo Panel Does Not Block](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L156) | PASSED | 6.58 |
| D67 | [A330 Autothrust And Approach Selectors Hide Conflicting Alternatives](../app/src/androidTest/java/com/jerry/electrical_checklist/NewModelsTest.java#L98) | PASSED | 10.84 |

### ReferenceSourceTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D68 | [Multipage Pdf Renders Correct Pages And Rejects Invalid Index](../app/src/androidTest/java/com/jerry/electrical_checklist/ReferenceSourceTest.java#L32) | PASSED | 0.14 |
| D69 | [Unreadable Image Reports Failure And Releases Owned File](../app/src/androidTest/java/com/jerry/electrical_checklist/ReferenceSourceTest.java#L84) | PASSED | 0.00 |
| D70 | [Png Jpeg And Webp Images Are Decoded At Bounded Resolution](../app/src/androidTest/java/com/jerry/electrical_checklist/ReferenceSourceTest.java#L58) | PASSED | 0.14 |
| D71 | [Original Bundled Pdf Renders](../app/src/androidTest/java/com/jerry/electrical_checklist/ReferenceSourceTest.java#L20) | PASSED | 0.36 |

### SettingsTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D72 | [Font Preview And Saved Size Preserve Ticks And Reset Keeps Settings](../app/src/androidTest/java/com/jerry/electrical_checklist/SettingsTest.java#L88) | PASSED | 8.28 |
| D73 | [Day And Night Retain Checklist And Apply To Reader And Restart](../app/src/androidTest/java/com/jerry/electrical_checklist/SettingsTest.java#L105) | PASSED | 12.33 |
| D74 | [About Shows Author Version And Opens Repository Link](../app/src/androidTest/java/com/jerry/electrical_checklist/SettingsTest.java#L142) | PASSED | 3.73 |
| D75 | [Largest Font Wraps Long Responses And Keeps Source Notes Informational](../app/src/androidTest/java/com/jerry/electrical_checklist/SettingsTest.java#L126) | PASSED | 3.62 |

### ToolbarTitleTest

| ID | Case / coverage | Result | Seconds |
| --- | --- | --- | ---: |
| D76 | [Engine Start Title Survives Settings Reader Rotation And Recreation](../app/src/androidTest/java/com/jerry/electrical_checklist/ToolbarTitleTest.java#L118) | PASSED | 9.56 |
| D77 | [Narrow Phone Keeps Long Aircraft Names Complete And Controls Available](../app/src/androidTest/java/com/jerry/electrical_checklist/ToolbarTitleTest.java#L180) | PASSED | 7.24 |
| D78 | [Shortcut Pages Use Brand And Selected Aircraft Uses Model](../app/src/androidTest/java/com/jerry/electrical_checklist/ToolbarTitleTest.java#L141) | PASSED | 4.64 |
| D79 | [Every Aircraft Shows Its Model In Picker And Checklist And Home Restores Brand](../app/src/androidTest/java/com/jerry/electrical_checklist/ToolbarTitleTest.java#L107) | PASSED | 21.69 |

## Additional UI checks

| ID | Check performed | Result | Evidence |
| --- | --- | --- | --- |
| M01 | Boeing 757 Engine Start displays the aircraft in the phone toolbar. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/phone-757-engine-start.png) |
| M02 | Long 737 MAX model name remains readable in the normal phone toolbar. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/phone-737-max.png) |
| M03 | 320dp narrow phone shows the full model and usable toolbar actions. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/narrow-phone-737-max.png) |
| M04 | Tablet landscape retains A320 Extended selection with day mode and 175% checklist text. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/tablet-a320-day-large.png) |
| M05 | Tablet sidebar switches phase while the aircraft toolbar stays correct. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/tablet-a320-before-start.png) |
| M06 | Original A320 Extended PDF renders on page 2 without network access. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/offline-native-reader.png) |
| M07 | Cold restart restores the last Extended checklist and aircraft toolbar. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/tablet-last-startup.png) |
| M08 | Tablet portrait keeps the selected checklist, model and large text. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/tablet-portrait-a320.png) |
| M09 | Home and Recent restore app branding when page animations are disabled. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/home-branding.png) |
| M10 | After Home Back and launcher resume, Engine Start Back returns to phases (regression fixed). | PASSED | Recorded assertions |

## Signed release checks

| ID | Check performed | Result | Evidence |
| --- | --- | --- | --- |
| R01 | Actual signed Release v1.18 installs fresh and launches as a non-debuggable app. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/release-home.png) |
| R02 | Fresh release has working empty Favorites and Recent pages. | PASSED | Recorded assertions |
| R03 | Release shows Boeing 757 in the Engine Start toolbar and checks the next required row. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/release-757-engine-start.png) |
| R04 | Release renders the bundled four-page Boeing PDF while offline. | PASSED | [Screenshot](../output/testing/2026-10-26/screens/release-offline-reader.png) |
| R05 | Release check persists after force-stop and cold restart. | PASSED | Recorded assertions |
| R06 | Release Favorites/Recent shortcuts show model names and return to their originating list. | PASSED | Recorded assertions |
| R07 | Release reset cancellation retains checks; confirmation clears them while keeping favorites. | PASSED | Recorded assertions |
| R08 | Actual signed release keeps nested Back navigation working after Home Back and launcher re-entry. | PASSED | Recorded assertions |

## Findings and fixes

- The requested toolbar change is included. It uses the selected model name throughout its checklist picker and active checklist, and restores E-Checklist on Home, manufacturer/model lists, Favorites and Recent. Text can resize/wrap within the toolbar.
- Five new regression cases cover launcher re-entry and check all 12 aircraft names, picker/checklist/Home transitions, shortcut origins, Boeing 757 Engine Start, settings, native-reader return, rotation and recreation. They also assert that the toolbar text is not ellipsized at the default phone size and at 320dp width.
- Initial setup corrections: this project exposes the Debug JVM unit-test task rather than a Release unit-test task; an ambiguous Back import in the new test was corrected before device execution. These were test setup/compilation issues, not runtime failures.
- Visual review caught a truncated 737 MAX Family title on a 320dp phone. Allowing a third toolbar line resolves it; a new test checks both the MAX and A320 Family names at that real display width, then restores the display.
- Confirmed and fixed a lifecycle bug outside the initial suite: Home Back delegated to Android after permanently disabling the app callback. On Android 12 the root task could be backgrounded and reused, leaving nested Back navigation disabled. The callback is now disabled only during delegation and restored in a finally block.
- The launcher-reentry scenario failed before the fix and passed after it. A new automated case repeats launcher re-entry twice and verifies both toolbar and system Back. The complete 79-case suite was then rerun on the final build.
- No failures, errors or skips were reported by the final automated suites.

## Artifacts and evidence

| APK | Size (bytes) | SHA-256 |
| --- | ---: | --- |
| [flight-check-debug.apk](../output/flight-check-debug.apk) | 17669221 | `f9ccb93661be5551c0d55331af4b19596c0139350a8c195615aed22e42f22144` |
| [E-Checklist-v1.18-release.apk](../output/E-Checklist-v1.18-release.apk) | 9433775 | `0432f3834d24a0fc60c4f795b81347a0500c4edd4df4e0cfbb80051863dde028` |

The release is signed with the existing private release key; its SHA-256 certificate fingerprint is `85cddbb180d91649c82d4c178394d91008fc3e2933fffc5b6ea3986fc746e993`. Signature and ZIP alignment were verified. The actual signed release APK was installed fresh on the disposable emulator for release checks. Private signing material is excluded from Git.

- [Unit results](../output/testing/2026-10-26/unit-results.xml)
- [Full device log](../output/testing/2026-10-26/debug-device-run.txt)
- [Initial 77-case run](../output/testing/2026-10-26/initial-debug-device-results.xml)
- [Narrow title before fix](../output/testing/2026-10-26/screens/narrow-phone-737-max-before-fix.png) / [after fix](../output/testing/2026-10-26/screens/narrow-phone-737-max.png)
- [Lifecycle reproduction before fix](../output/testing/2026-10-26/root-back-before-fix.json) / [after fix](../output/testing/2026-10-26/root-back-after-fix.json)
- [Device JUnit results](../output/testing/2026-10-26/debug-device-results.xml)
- [Device case records](../output/testing/2026-10-26/debug-device-results.json)
- [Manual UI records](../output/testing/2026-10-26/manual-results.json)
- [Release smoke records](../output/testing/2026-10-26/release-smoke-results.json)
- [Release lifecycle reproduction](../output/testing/2026-10-26/release-root-back.json)
- [Signature verification](../output/testing/2026-10-26/release-signature.txt) / [alignment verification](../output/testing/2026-10-26/release-alignment.txt)
- [Debug lint](../output/testing/2026-10-26/lint-debug.xml) / [Release lint](../output/testing/2026-10-26/lint-release.xml)
- [Asset audit](../output/testing/2026-10-26/asset-audit.json) / [APK audit](../output/testing/2026-10-26/artifact-audit.json)

## Testing limits

- Runtime tests used the installed Android 12 emulator. Android 9–11, Android 13–17, physical hardware, vendor Android variants and every possible screen/font combination were not tested.
- Automated regression targeted the Debug build. The signed optimized Release build received the separate release checks listed above; the full instrumentation suite was not run against that non-debuggable APK.
- This run validates software behavior and bundled-file integrity. It does not re-audit every checklist instruction against the original aircraft documents or establish aviation correctness.
- Long-duration use, exhaustive memory/performance profiling, accessibility-service interaction and all possible process-death timing combinations were not covered.
