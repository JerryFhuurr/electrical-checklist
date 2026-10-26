# About and app-opening update checks

Date: 26 October 2026 (Europe/Paris)

Build: E-Checklist 1.20 / Android version code 22

## Change

About now has its own **Check for updates** button, status message, in-progress state, and the same Download / Skip version / Remind later prompt as Settings.

The application tracks foreground activity transitions. It forces a new GitHub check on initial opening and every return from the background, including when About or Settings is the restored page. Internal page changes and configuration recreation share the current check/result. Skip and 24-hour reminder choices still control automatic prompts, which wait for Home.

## Verification

- JVM unit tests: **20/20 passed**, no failures or errors.
- Debug, instrumentation and optimized Release APK builds passed.
- Debug and Release lint each report **0 errors**, with the same 7 existing warnings.
- Signed APK signature and ZIP alignment verification passed with the existing release key.
- Android tests: **29/29 passed** in 260.52 seconds, no failures, errors or skipped cases, including the live GitHub check.
- Signed release upgrade: **passed** from v1.19 to v1.20, with app UID retained and release startup alive without a fatal exception. See [upgrade evidence](../output/testing/2026-10-26/updates-1.20/release-upgrade-smoke.json).

| Suite | Cases | Coverage |
| --- | --- | --- |
| JVM tests | 20 | Version parsing/comparison, caching/reminder rules and checklist progress. The foreground check explicitly bypasses the periodic cache interval. |
| UpdateUiTest | 14 | Every fresh opening checks again, offline retry on the next opening, skip and reminder persistence across repeated checks, manual overrides, deferred Home prompts, browser handoff, recreation, day/night layouts, About manual success/error/retry, About recreation during a request and prompt, returning to About from the background, and no extra request for internal navigation or recreation. |
| SettingsTest | 4 | Text settings, themes, source reader, About version/author and GitHub link. |
| NavigationLifecycleTest | 1 | Background/launcher return and both Back navigation controls. |
| GitHubReleaseClientTest | 9 | Release parsing, `Main` tag/APK filename compatibility, stable-only APK selection, URL validation, HTTP success/errors/limits, malformed/oversized responses and persisted choices. |
| GitHubReleaseLiveTest | 1 | Explicit real Android HTTPS check of the public GitHub release, enabled with `-e liveGithub true`. |

Android runtime: disposable read-only API 31 / Android 12 x86_64 emulator. All device commands address `emulator-5554`; the physical phone is untouched. UI animation scales are zero for replay. Injected versions and clocks exercise future releases and 24-hour choices without publishing a release. The live test uses the real production client.

Commands:

```powershell
.\gradlew.bat --no-configuration-cache testDebugUnitTest assembleDebug assembleDebugAndroidTest assembleRelease lintDebug lintRelease
adb -s emulator-5554 shell am instrument -w -r -e liveGithub true -e class com.jerry.electrical_checklist.UpdateUiTest,com.jerry.electrical_checklist.SettingsTest,com.jerry.electrical_checklist.NavigationLifecycleTest,com.jerry.electrical_checklist.GitHubReleaseClientTest,com.jerry.electrical_checklist.GitHubReleaseLiveTest com.jerry.electrical_checklist.test/androidx.test.runner.AndroidJUnitRunner
```

## Scope

These checks cover the changed update behavior and related settings/navigation. The [v1.19 report](UPDATE-TEST-REPORT-2026-10-26.md) records the earlier 46-case update regression, and the [v1.18 report](TEST-REPORT-2026-10-26.md) records the broader aircraft/document suite. Their APKs and evidence remain unchanged.

Update checks require internet and an available public GitHub API. Automatic failures remain silent; manual checks display a result. Downloads open in the browser, and installation remains a user action. No background notification or installation service is added.

## Artifacts

- [Signed v1.20 APK](../output/E-Checklist-v1.20-release.apk)
- APK SHA-256: `3b87385f5734031d35a98f5a74274e94f67df52c8db96b21c580774331757d5d`
- [Build, lint, signing and test evidence](../output/testing/2026-10-26/updates-1.20)
