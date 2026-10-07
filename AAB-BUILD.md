# Android-App-Bundle — 07.10.2026

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
