# Emulator lifecycle and process-death tests

This helper exercises the installed production APK through Android accessibility
actions. It does not call `FakeScanState` directly, inject application state, or
modify the APK. The separate helper targets its own package for instrumentation.

`UiAutomation.getRootInActiveWindow()` lets the test observe the active animated
scan without waiting for the whole window to become idle. See the official
[UiAutomation API](https://developer.android.com/reference/android/app/UiAutomation)
and [accessibility actions](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo).

## Run

Requires the same JDK, API-36 SDK and build-tools 36.0.0 as the production build,
plus a booted Android emulator. From the repository root:

```powershell
./tests/runtime/build-probe.ps1 -SdkPath C:\Android\Sdk `
  -JavaHome 'C:\Program Files\Eclipse Adoptium\jdk-21'
./tests/runtime/run-lifecycle.ps1 -Serial emulator-5570 `
  -AdbPath C:\Android\Sdk\platform-tools\adb.exe `
  -ApkPath ./build/outputs/maximum-spyware-0.2.1.apk

# Reuse the helper to exercise real Android process recreation:
./tests/runtime/run-lifecycle.ps1 -Serial emulator-5570 `
  -AdbPath C:\Android\Sdk\platform-tools\adb.exe `
  -ApkPath ./build/outputs/maximum-spyware-0.2.1.apk -Scenario process-death
```

The runner rejects non-`emulator-*` serials and checks `ro.kernel.qemu=1` before
every ADB operation. The helper also checks this property before operating.
There is no default-device fallback. Only the emulator is changed: the runner
saves/restores its display, density, font and rotation configuration. It installs
the supplied production APK, starts a fresh scan, and removes the separate test
helper after collecting evidence. The production app remains installed.

## Assertions and evidence

The real UI must pause, remain unchanged for three seconds, preserve paused
progress/session on rotation, advance after Continue, suspend for 25 seconds in
the background, resume, finish its report, and start session 2. The background
duration is longer than the whole scan; the test also bounds the progress added
by foreground transitions. Screenshot dimensions verify actual landscape.

JSON, instrumentation output and seven emulator screenshots are written under
`build/lifecycle-<timestamp>/`. Failed assertions produce a nonzero runner exit.

The separate `process-death` scenario enters session 2 with nonzero progress,
moves the real app to the background, calls Android's `am kill` and checks that
its process is absent. After five seconds it resumes the existing task through
the normal launcher intent and requires a different process ID. It repeats this
for a paused scan, a running scan and a completed report. Assertions cover exact
paused progress, pause/continue, excluded dead time, the restored report and a
working new scan in session 3. Eight screenshots, process IDs and JSON assertions
go under `build/process-death-<timestamp>/`.

`am force-stop` is used only to establish each test's initial clean launch; it is
not used to simulate the tested deaths. The separate helper remains alive while
the production app dies, and no production state is injected. These scenarios
cover one emulator image. They do not reproduce actual low-memory pressure,
device reboot, force-stop persistence, manufacturer background restrictions or
other Android versions.
