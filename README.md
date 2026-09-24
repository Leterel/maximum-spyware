# MAXIMUM SPYWARE

Satirical Android app parodying surveillance capitalism.

**This app collects nothing.** All surveillance UI is fake/local.

## Milestone 0.2.1

- Giant dystopian disclaimer screen
- German disclosure and "OKAY, SPIONIER MICH FAKE AUS" button
- Basic fake surveillance dashboard
- Animated local radar, fake terminal timeline, pause/restart and fake report
- No INTERNET permission
- No analytics, ads, or tracking SDKs
- Header and metric cards stack on narrow displays or with large accessibility text

## Build

The checked-in prototype is a dependency-free native Android build. This keeps
the APK reproducible offline while the original Compose sketches remain in the
source tree as design notes.

From the project root on Windows:

```powershell
./build-native.ps1 -SdkPath C:\Android\Sdk -JavaHome "C:\Program Files\Eclipse Adoptium\jdk-21"
```

APK output: `build/outputs/maximum-spyware-0.2.1.apk` (the deliverable copy is
in `outputs/maximum-spyware/`).

## Requirements

- JDK 17+
- Android SDK API 36 and build-tools 36.0.0

The build runs `tests/ScanScriptTest.java` against the production `FakeScanState`
class. It checks timing, pause/resume, background suspension, report/restart,
restoration after rotation and event visibility. It also verifies the APK
signature and alignment. See [PROGRESS.md](PROGRESS.md) for the actual test scope.

## Install and use

[Download APK](https://github.com/Leterel/maximum-spyware/releases/tag/v0.2.1)
(private repository; GitHub sign-in required).

Copy `maximum-spyware-0.2.1.apk` to an Android 8.0+ device and open it. Allow
installation from your file manager if Android asks. Launch **MAXIMUM SPYWARE**,
read the simulation disclosure and tap the green button. The invented scan
takes 22 seconds of foreground time. Pause freezes it; **NEUER SCAN** resets
the timeline. The fake report becomes available at completion.

The APK uses a local development key, not a Play Store publishing key. Keep
the same ignored keystore to update an existing installation.

The app has no permission declarations. Every apparent finding is hard-coded
fiction and the UI labels it as a simulation.

## Verified prototype

On 2026-09-23, all 3,797 production-state assertions passed, and the 0.2.1 APK
passed V2/V3 signature verification and alignment checks. Its manifest requests
no permissions. The expanded API-34 emulator matrix covers phone, small screen
with 200% text, landscape and tablet display configurations. Completed report,
new scan and actual rotation passed in every layout (13 automated checks).

See [EMULATOR-VERIFICATION.md](EMULATOR-VERIFICATION.md) for evidence and the
emulator-only test command. These are four display configurations on one system
image, not four devices. A separate real UI lifecycle run on the same unchanged
APK passed all 13 checks: pause/resume, 25 seconds in the background, rotation
while paused, report and restart. See [LIFECYCLE-VERIFICATION.md](LIFECYCLE-VERIFICATION.md)
and the [emulator-only lifecycle runner](tests/runtime/README.md). Process death,
manufacturer background restrictions and other Android versions remain untested.
