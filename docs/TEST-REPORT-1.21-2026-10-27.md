# v1.21 update-check recovery and replacement A350 checklist

Date: 27 October 2026 (Europe/Paris)

Build: E-Checklist 1.21, Android version code 23

## Results

- **25 JVM tests passed**, no failures or errors.
- **52 Android regression cases passed** on the disposable API 31 emulator in 204.248 seconds, including the live GitHub check.
- **Physical-phone acceptance passed** on the user's Mi MIX 2S running LineageOS / Android 15: live HTTPS, About manual update check, new A350 catalog and tick/untick, both original PDF pages, and return to the same checklist.
- The signed APK updated the phone from **v1.20 to v1.21 in place**. The logical saved-progress fingerprint matches before the upgrade, before acceptance and after acceptance.
- Debug, instrumentation and optimized Release builds passed. APK signature and ZIP alignment checks passed with the existing release certificate.
- Debug and Release lint report **0 errors**, with 7 existing warnings (dependency-version suggestions and range typography).

## Root cause observed on the phone

Wi-Fi worked for the system, and the app's `INTERNET` permission was granted. However, LineageOS had assigned E-Checklist the per-app `REJECT_ALL` network policy, with **Allow network access** disabled. Requests from the app failed DNS resolution immediately, while a system-side lookup of `api.github.com` succeeded.

In **Android app info → E-Checklist → Wi-Fi data usage**, enabling **Allow network access** removed that restriction. The existing v1.20 app then completed its manual check and displayed **You have the latest available version** before the new APK was installed. The v1.21 acceptance test repeated the successful check over the phone's Wi-Fi.

The app does not bypass Android network restrictions. The new build detects when no default network is available to the app, preserves DNS/timeout/TLS failure reasons, and offers **App network settings** for unavailable networks and DNS errors. Settings and About both support this recovery path; the button targets only E-Checklist's app info.

## A350 replacement

The supplied `27777699.pdf` replaces the previous three-page INI A350 full SOP reference and its interactive transcription.

- Page 1 is the default **eight normal phases**, with **58 checkbox rows** (57 required and one optional heavy-rain EXTRACT item).
- Landing ECAM memo reminders and weather notes remain informational text.
- Page 2 evacuation checks are behind **Emergency evacuation**. The MAN CAB PR option controls the conditional pressure check.
- The captain/F/O rejected-takeoff table, alternatives and additional information are preserved as non-clickable **Rejected takeoff guidance**, with the original PDF available in the reader.
- Aircraft ID `ini-a350` stays stable for favorites and model identity. Replacement checklist IDs use `normal-` so checked items or completed sections from the old SOP do not complete the new checklist accidentally. Other aircraft progress is retained.
- The bundled PDF is byte-identical to the supplied file. SHA-256: `4f6d51d93cfa92a63618c349114acea976d894a4d614009bfecff37fafd70d61`.

## Test coverage

| Suite | Cases | Verification |
| --- | --- | --- |
| JVM suites | 25 | Numeric versions, progress/completion rules, skip/reminder/cache rules, and five DNS/wrapped-DNS/timeout/TLS/other-error classifications. |
| A350ChecklistTest | 6 | Phase order and counts; separate procedures; normal progression/final phase; optional heavy-rain behavior; informational notes; evacuation branch and fixed completion; rejected-takeoff notes; old SOP isolation and other-aircraft progress; native PDF pages 1 and 2. |
| UpdateUiTest | 16 | Every-opening checks, skip/reminder choices, manual overrides, browser handoff, errors, activity recreation, About, app foreground transitions, blocked-network/DNS messages, app-info intent, settings-button visibility and recovery, timeout and secure-connection messages. |
| GitHubReleaseClientTest | 9 | Stable release and APK parsing, generic Main tag compatibility, trusted download URLs, HTTP results, response bounds, cleanup and saved choices. |
| GitHubReleaseLiveTest | 1 | Real Android HTTPS request to GitHub's latest-release API. |
| SettingsTest | 4 | Font/theme persistence and reader appearance, long text/non-interactive headings (A380), About author/version/link. |
| ManufacturerNavigationTest | 3 | Model grouping, filter/back flow and navigation restoration with the replacement A350. |
| ToolbarTitleTest | 4 | Aircraft names, source reader/settings/rotation, shortcuts and narrow-phone controls. |
| ChecklistAppTest | 8 | Catalog, interaction, progress persistence, source reader, completion/advancement, final checklist, automatic scrolling, reset cancellation/confirmation and rotation. |
| NavigationLifecycleTest | 1 | Launcher/background return and Back navigation. |

## Physical-phone method and preservation

The phone retained its installed release and data. A framework-only acceptance runner, signed with the matching private release key, avoided incompatibility between debug AndroidX test libraries and the optimized installed app. The broad regression tests that reset data ran only on the disposable emulator.

The phone runner recorded a read-only v1.20 baseline, then the release was installed with `adb install --no-incremental -r`. It exercised the real production v1.21 app, toggled one new A350 checkbox twice back to its initial state, opened both PDF pages, and compared saved progress again. No reset or uninstall of the main app was performed. Logical progress fingerprint:

```
a394091face37deaa8944f43e40de4444ffe2e20e2c64a0214664965bd272d11
```

An initial phone test stopped at a non-clickable phase-title view; the test helper was corrected to activate the clickable parent card. The app behavior did not require a change for that test issue. The final acceptance runner passed.

The physical phone's animation settings and global network settings were left unchanged. The app-specific network-access switch was enabled as part of fixing the reported failure. Navigation history can include the A350 opened during testing.

## Artifacts and commands

- [Signed v1.21 APK](../output/E-Checklist-v1.21-release.apk)
- APK SHA-256: `a4571c169a6a213040afac4ca43ee8441b090a2d32853d199b63029e6a90d93e`
- [Build, unit, emulator and phone evidence](../output/testing/2026-10-27/v1.21)
- [Phone About update result](../output/testing/2026-10-27/v1.21/phone/phone-about-update.png)
- [Phone A350 checklist](../output/testing/2026-10-27/v1.21/phone/phone-a350-before-start.png)
- [Native PDF page 1](../output/testing/2026-10-27/v1.21/phone/phone-a350-pdf-page1.png), [page 2](../output/testing/2026-10-27/v1.21/phone/phone-a350-pdf-page2.png)

```powershell
.\gradlew.bat --no-configuration-cache testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug lintRelease
adb -s emulator-5554 shell am instrument -w -r -e liveGithub true -e class com.jerry.electrical_checklist.A350ChecklistTest,com.jerry.electrical_checklist.UpdateUiTest,com.jerry.electrical_checklist.GitHubReleaseClientTest,com.jerry.electrical_checklist.GitHubReleaseLiveTest,com.jerry.electrical_checklist.SettingsTest,com.jerry.electrical_checklist.ManufacturerNavigationTest,com.jerry.electrical_checklist.ToolbarTitleTest,com.jerry.electrical_checklist.ChecklistAppTest,com.jerry.electrical_checklist.NavigationLifecycleTest com.jerry.electrical_checklist.test/androidx.test.runner.AndroidJUnitRunner
.\gradlew.bat --no-configuration-cache '-PtestRunner=com.jerry.electrical_checklist.PhoneAcceptanceInstrumentation' assembleDebugAndroidTest
# Sign the test APK with the key matching the installed release before using the phone runner.
adb -s <phone> shell am instrument -w -r -e baseline true com.jerry.electrical_checklist.test/com.jerry.electrical_checklist.PhoneAcceptanceInstrumentation
adb -s <phone> install --no-incremental -r output/E-Checklist-v1.21-release.apk
adb -s <phone> shell am instrument -w -r com.jerry.electrical_checklist.test/com.jerry.electrical_checklist.PhoneAcceptanceInstrumentation
```

These results cover the changed behavior and listed environments, not every device or network. The phone test observed the public v1.20 release before v1.21 publication; future-version prompts and skip/reminder choices were checked with fixtures on the emulator. Source PDF pages and phone renderings were visually inspected. Checklists remain for flight simulation use only.
