# Emulator lifecycle test

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
This is one emulator lifecycle scenario. It does not test process death,
manufacturer background restrictions or multiple Android versions.
