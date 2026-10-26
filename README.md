# E-Checklist

E-Checklist is an Android app for interactive aircraft checklists during flight simulation. Choose your aircraft, work through each flight phase, and open the original checklist document whenever you need a reference.

The app works offline on **Android 9 and newer**, with layouts for phones and tablets. Checklist items are checked manually, and progress is saved on your device.

**For flight simulation use only. These checklists are not approved for real aircraft operations.**

## How to use

1. **Install the app.** Download the [E-Checklist v1.20 release APK](output/E-Checklist-v1.20-release.apk) to your Android device and open it to install.
2. **Choose your aircraft.** Select a manufacturer, then a plane model, such as Boeing → Boeing 737-NG.
3. **Open a checklist.** Choose the flight phase you want to work through, such as Before Start.
4. **Check the items.** Tap a row or its checkbox to check or uncheck it. You can also use **Mark item as complete** to check the next required item.
5. **Finish the checklist.** Once all required items are checked, tap **Mark checklist as complete**. Normal checklists advance to the next phase; the final checklist stays open. Separate A340 emergency and other procedures stay on the selected checklist when completed.
6. **Start a new session.** Tap **Reset all** and confirm to clear checked items and completed checklists across every aircraft. Favorites, recent aircraft, settings, and checklist options are retained.

Use **Back** to return to the previous level or **Home** to return to manufacturers. Your checks are saved automatically, so you can leave and resume later.

If you already have the earlier debug build installed, the release APK cannot update it directly because it uses a different signing key. Uninstalling the debug app clears its saved progress.

## Features

- **Offline checklists:** Use the bundled checklists and original documents without an internet connection.
- **Manufacturer and model selection:** Browse aircraft grouped by manufacturer, with models sorted by name. The selected model appears in the toolbar on its checklist screens.
- **Interactive challenge/response rows:** Check items manually, track completion, and uncheck a required item to reopen a completed checklist.
- **Optional and manual-only items:** Tick these when applicable; they do not block completion. Source headings and notes remain readable without becoming checkboxes.
- **Automatic progression and scrolling:** Completing a normal checklist opens the next phase. Checking the bottom visible item reveals the next unchecked item.
- **Saved progress:** Keep separate checks for each aircraft, phase, and checklist mode across app restarts and screen rotation.
- **Aircraft-specific checklist modes:** Switch between A320 Normal/Extended items or Boeing 777 normal checklists/full procedures. The A340 also offers evacuation, other procedures, and selected non-normal memory items.
- **Selectable procedure options:** For supported aircraft, choose the applicable power, approach, or other procedure path so only matching items are shown and required.
- **Built-in document reader:** Tap **View checklist** to read the included source document. The reader supports PDFs and images, page navigation, pinch/double-tap zoom, panning, and **Fit page**.
- **Favorites and recent aircraft:** Star your usual aircraft, then open the separate **Favorites** or **Recent aircraft** page from the home screen. Recent shows the last five opened aircraft, newest first.
- **Startup preferences:** Open the manufacturer list, a chosen favorite aircraft, or the last-used checklist when starting the app.
- **Text size and appearance:** Choose checklist text sizes from 85% to 175%, plus **Day**, **Night**, or **Use device setting** in Settings.
- **Phone and tablet layouts:** Wide landscape screens place checklist buttons on the left and items on the right, with independently scrolling panes. Page transitions respect Android's animation preferences.
- **Keep-awake behavior:** The screen stays on while using the checklists or original document reader.
- **About page:** Find the app version, author, and [GitHub repository](https://github.com/JerryFhuurr/electrical-checklist) in Settings → About.
- **GitHub updates:** Checks for a newer release each time you open or return to the app. Choose **Download update**, **Skip version**, or **Remind later**. Settings and About include manual update checks; Settings also links to releases.

Available modes and procedure options depend on the aircraft. The A340 non-normal category contains **selected memory items only**.

The [test report](docs/TEST-REPORT-2026-10-26.md) records the regression cases, results, environment, and testing limits.

## App updates

An internet connection is needed only for update checks and downloads; all bundled checklists still work offline. GitHub is checked each time you open the app or return from the background, including when returning to Settings or About. Navigating between app pages and rotating the screen do not trigger another opening check. Automatic prompts wait for Home so they do not interrupt a checklist. Skip and reminder choices still apply even though release information is checked again.

- **Download update** opens the release APK in your default browser. Open the downloaded APK to install it over your existing release app. There is no automatic installation.
- **Skip version** hides that version across restarts. A newer version can still trigger a prompt.
- **Remind later** hides that version for 24 hours, then reminds you when you next return Home and release information is available. Closing the prompt with Back has the same effect. There are no background notifications.
- **Settings → About → Check for updates** checks immediately, including versions you previously skipped or deferred. The same button remains available directly in Settings. **View GitHub releases** in Settings opens the release history.

Resetting checklist progress keeps your update choices.

For maintainers: publish a stable [GitHub release](https://github.com/JerryFhuurr/electrical-checklist/releases) with a signed release APK to announce an update. Pushing commits alone does not publish an update. Use a numeric tag such as `v1.20` and an APK named `E-Checklist-v1.20-release.apk`, with a matching app version and increased Android version code. The current `Main` release tag is also supported when its APK filename contains the version. Drafts, prereleases, debug APKs, and releases without a usable APK are excluded. Checks use GitHub's public [latest release API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release), without an account or token. Keep using the same signing key so installed releases can update without losing progress.

The [update test report](docs/UPDATE-TEST-REPORT-2026-10-26.md) records update checks and the related regression tests.

The [v1.20 test report](docs/UPDATE-TEST-REPORT-1.20-2026-10-26.md) covers the About button and checks on each app opening.

## Supported aircraft

There are currently **12 aircraft entries across 4 manufacturers**. Family entries combine the variants shown below. Boeing 757 and 767 have separate entries and saved progress, even though they share a source checklist.

| Manufacturer | Aircraft | Included checklist / source |
| --- | --- | --- |
| Boeing | Boeing 737-NG | Ryanair normal checklist with supplemental takeoff, descent, approach, and landing phases. |
| Boeing | Boeing 737 MAX Family | Ryanair 737-8200 checklist with supplemental flight phases. |
| Boeing | Boeing 757 | Shared 757/767 normal checklist and procedures. |
| Boeing | Boeing 767 | Shared 757/767 normal checklist and procedures. |
| Boeing | Boeing 777 | PMDG / JayDee normal checklists and full procedures. |
| Airbus | Airbus A320 Family (A319 / A320 / A321) | Normal checklist and Extended items. |
| Airbus | Airbus A330-300 | iniBuilds / Maverick1994 checklist with selectable autothrust, power, and approach paths. |
| Airbus | Airbus A340-300 | Normal checklists, emergency evacuation, other procedures, and selected non-normal memory items. |
| Airbus | Airbus A350 | iniBuilds checklist, including formatted source headings and notes. |
| Airbus | Airbus A380 | FlyByWire A380X Full SOP Checklist. |
| McDonnell Douglas | MD-11 | FedEx MD-11F normal procedures, including optional manual-only items. |
| ATR | ATR 42 / 72 | BritishAvgeek / Osprey Airways summarized checklist. |
