# Android-App-Bundle — 07.10.2026

## Automatisierter Offline-Build — 19.10.2026

`build-bundle.ps1` nimmt `SdkPath`, `JavaHome`, `GradleCache`, `AndroidUserHome`,
`BundleToolJar` und einen neuen `OutputDirectory` entgegen. Standard ist offline;
`-Online` erlaubt das Nachladen fehlender Abhängigkeiten. Das Skript stellt
Prozess-Umgebungsvariablen wieder her, prüft Paketname, Bundle-Inhalt und fehlende
Signatur und validiert mit bundletool. Ausgabe: `app-release-unsigned.aab`,
Prüfsumme und Protokolle im neuen Ausgabeordner.

Offline-Build und fünf Schutzprüfungen bestanden: ungültige Paketnamen, fremde
Signierschlüssel, physische Geräteseriennummern und vorhandene Belegordner werden
abgelehnt. Der fest hinterlegte lokale Entwicklungsschlüssel erzeugt ein APK-Set.
Runner: `tests/test-bundle-guards.ps1`. Keine Geräteaktion in diesen Prüfungen.
Der Schlüssel bleibt lokal; bei Schlüsselwechsel muss der öffentliche
Zertifikat-Fingerabdruck im Runner bewusst geprüft und angepasst werden.
Keine Produktionssignierung oder Store-Veröffentlichung.

## Frühere manuelle Build-Prüfung

Der bestehende Java-Produktionscode lässt sich jetzt als **unsigniertes** AAB bauen.
Kein Store-Upload erfolgt. Dies ist kein fertig geprüftes Play-Release.

Geprüfte Werkzeuge: JDK 21, Gradle 8.9, AGP 8.7.3, SDK/API und Build-Tools 35.
Bei bereits gefülltem Gradle-Cache funktioniert folgender Offline-Build:

```powershell
$env:JAVA_HOME='C:/Pfad/zum/jdk-21'
$env:ANDROID_HOME='C:/Pfad/zum/android-sdk'
$env:GRADLE_USER_HOME='C:/Pfad/zum/gradle-cache'
$env:ANDROID_USER_HOME='C:/Pfad/zum/android-benutzerordner'
$aapt=Join-Path $env:ANDROID_HOME 'build-tools/35.0.0/aapt2.exe'
./gradlew.bat --offline --no-daemon "-Pandroid.aapt2FromMavenOverride=$aapt" bundleRelease
```

Ergebnis: `app/build/outputs/bundle/release/app-release.aab`.
Der AAPT2-Override ist eine experimentelle AGP-Option und nutzt das installierte
SDK-Werkzeug, da dessen Maven-Artefakt im vorhandenen Cache fehlt.
Der erste Lauf in der Dateisandbox scheiterte am Zugriff auf SDK-JARs;
derselbe Build mit normalen Prozessrechten bestand (40 Tasks, 14 Sekunden).

Bundle-Konfiguration, Manifest, DEX und Ressourcen im Archiv geprüft.
Jarsigner bestätigt: unsigniert. Noch offen: bundletool-Validierung, aus dem AAB
erzeugte APKs im Emulator testen, aktuelle Play-Ziel-API prüfen, Produktionssignatur,
Werbe-/Consent-Integration und Store-Unterlagen. Kein physisches Gerät verwendet.

## Bundletool-Prüfung am 08.10.2026

Offizielles bundletool-all 1.18.3: validate bestanden, lokal testsignierte APKs
erzeugt, im API-34-Emulator emulator-5570 installiert und gestartet. Sichtbarer
Simulationshinweis erkannt; kein fataler App-Fehler im geprüften Logausschnitt.
Die oben offene Bundletool-/Emulator-Startprüfung ist damit erledigt. Dies ist
kein neuer vollständiger Scan-/Lebenszykluslauf und keine Produktionssignierung.
Belege: outputs/maximum-spyware/aab-verification-20261008-pass2 im Workspace.
Runner: tests/test-app-bundle.ps1 mit explizitem Emulator und QEMU-Prüfung.
