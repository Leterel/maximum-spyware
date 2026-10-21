# Fortschritt

## 2026-09-30 — Lokale Regression geprüft

- Produktionsklasse und JVM-Test neu kompiliert: alle 3.797 Prüfungen bestanden.
- Ausgelieferte APK 0.2.1 erneut geprüft: SHA-256
  `AB5169EA3C6AFAD510BA0EBECCBB704C12A3A0BC5700C30FF1C2429AF319EB5D`,
  Signatur V2/V3 gültig, ZIP-Alignment gültig und keine Berechtigungen deklariert.
- Kein neuer Geräte- oder Emulatorlauf; keine neue APK erstellt.
- Werbung ist weiterhin nur geplant. Ad-SDK, Consent und Play-AAB fehlen.


## 2026-09-28 — Teständerungen und Auslieferungspaket geprüft

- Die neuen Prozessverlust-Prüfungen bedienen ausschließlich die Produktions-UI;
  sie prüfen echte PID-Wechsel, exakt wiederhergestellten Pausenstand und den
  erhaltenen Sitzungszähler. Der Emulator-Schutz bleibt im Runner und Helper aktiv.
- Alle 27 Prüfergebnisse und acht Screenshots des Laufs vom 25.09. sind im
  Prüfpaket enthalten. Die Originaldateien wurden erneut hashgleich abgeglichen.
- APK 0.2.1 bleibt unverändert; es wurde weder neu gebaut noch ein Emulator
  gestartet. Neue datierte Prüfsummen kennzeichnen nur das ergänzte Belegpaket.

## 2026-09-27 — Prozessverlust-Belege für die Auslieferung gesichert

- Die Originalbelege vom 25.09. erneut geprüft: 27/27 Detailprüfungen bestanden,
  acht Screenshots und drei echte Prozesswechsel vorhanden.
- Elf Originaldateien hashgleich in das gemeinsame Auslieferungsverzeichnis
  `outputs/maximum-spyware/process-death-verification-20260925/` kopiert.
- Der Hash der ausgelieferten APK 0.2.1 entspricht weiterhin dem geprüften Build.
  Keine neue APK und kein erneuter Prozessverlust-Test am 27.09. behauptet.
- Details: PROCESS-DEATH-VERIFICATION.md.

## 2026-09-25, 23:53 — Prozessverlust auf dem Emulator geprüft

- Neuer wiederholbarer Black-box-Test beendet die echte App dreimal im Hintergrund
  mit Androids `am kill`. Fehlende PID und andere PID nach Launcher-Rückkehr belegen
  die tatsächliche Prozessneuerstellung; kein Produktions-Testmodus.
- 27/27 Prüfungen: Sitzung 2 pausiert bei 11 % bleibt exakt erhalten, Weiter
  funktioniert, laufender Scan zählt tote Zeit nicht mit, fertiger Bericht bleibt
  bei 100 %, neuer Scan startet anschließend in Sitzung 3.
- Unveränderte APK 0.2.1 getestet. Keine Produktionsänderung nötig; keine
  physischen Geräte verwendet. Runner stellt Emulatoreinstellungen wieder her
  und entfernt den Testhelfer.
- Belege, PIDs, Grenzen und Wiederholung: PROCESS-DEATH-VERIFICATION.md und
  tests/runtime/README.md. API-34-Emulator; echter Speichermangel, Neustart und
  herstellerspezifische Hintergrundregeln bleiben offen.

## 2026-09-23, 23:39 — Lebenszyklus der echten UI geprüft

- Separater, emulatorgeschützter Instrumentation-Helfer bedient die originale
  APK über Accessibility und umgeht UIAutomators Idle-Warteproblem bei Animationen.
- 13/13 echte UI-Prüfungen: Pause, drei Sekunden Stillstand, Rotation während
  Pause, Weiter, 25.001 ms Hintergrund, Bericht und neuer Scan in Sitzung 2.
- Pause/Rotation blieben bei 2 %. Nach Weiter 9 %, nach 25 Sekunden Hintergrund
  erst 10 %. Anschließend Bericht bei 100 % und neuer Scan bei 1 %.
- Keine Änderung an Produktionscode oder APK notwendig. Testhelfer entfernt,
  Emulatoreinstellungen wiederhergestellt. Kein physisches Gerät verwendet.
- Belege und Grenzen: LIFECYCLE-VERIFICATION.md; reproduzierbare Befehle unter
  tests/runtime/README.md. Weitere Android-Versionen und Prozessverlust offen.

## 2026-09-23 abends — 0.2.1 und erweiterte Emulatormatrix

- Bei schmalen Displays/großer Schrift stehen Titel und Simulationshinweis jetzt
  untereinander. Die drei Kennzahlen bekommen ebenfalls volle Breite, statt
  durch große Schrift in schmale Spalten gedrängt zu werden.
- Neue APK 0.2.1 mit bisherigem lokalem Entwicklungsschlüssel gebaut.
  Produktionszustand 3.797/3.797; V2/V3-Signatur und Alignment bestanden;
  Manifest weiterhin ohne Berechtigungen.
- Vier API-34-Layouts: 411-dp-Handy, 320 dp mit 200 % Schrift, Querformat und
  800-dp-Tablet. Bericht und zweiter Scan erreichbar; echte Rotation geprüft.
- 13 Matrixprüfungen bestanden. Screenshots/Hierarchien/JSON als Release-Belege;
  Darstellung bei großer Schrift zusätzlich visuell geprüft.
- Nur Emulator verwendet. Hintergrund/Rotation während Pause sind hier nicht
  als neuer UI-Lauf behauptet; dafür gelten die Produktionszustandstests.
- Details und Prüfsumme: EMULATOR-VERIFICATION.md. Alte Version bleibt verfügbar.

Die folgenden Notizen betreffen den älteren Stand 0.2.0.

## 2026-09-23 — Prototyp 0.2.0 geprüft und gesichert

- Native, offline reproduzierbare Android-APK mit Disclaimer und Fake-
  Überwachungs-Dashboard.
- Radar, Terminal-Timeline, Pause/Neustart und Fake-Report sind rein lokale
  Animationen. Es gibt keine Berechtigungen, kein Netzwerk und keine Tracker.
- `FakeScanState` trennt die Scan-Uhr von der Darstellung: Pause und Hintergrund
  zählen nicht zur Laufzeit; Rotation stellt Zustand, Pause und Scan-Nummer wieder her.
- 3.797 Prüfungen gegen diese Produktionsklasse am 23.09. bestanden: Zeitverlauf,
  Pause/Fortsetzen, Hintergrund, Bericht/Neustart, Wiederherstellung und keine
  vorzeitig sichtbaren Timeline-Meldungen.
- APK vom 20.09. gebaut; Signatur (V2/V3), 4-Byte-Ausrichtung und leere
  Berechtigungsliste am 23.09. erneut an der ausgelieferten Datei geprüft.
- API-34-Emulatorbelege vom 21.09. zeigen die abgeschlossene Timeline, den
  Fake-Bericht und einen neuen Scan. Das ist kein Test auf einem physischen Gerät.
- Am 23.09. erstmals auf einem angeschlossenen Pixel 10a installiert (`Success`).
  Der App-Ablauf wurde dort nicht geprüft, weil das Gerät parallel benutzt wurde.

Auslieferung: `maximum-spyware-0.2.0.apk` (25.236 Bytes).
SHA-256: `0B74F4A32B67B6FDA2F2CA193CE55DB8128688C32A0669186887BBDC9E92AF29`.

Screenshots und `VERIFICATION.md` liegen neben der ausgelieferten APK. Bei der
erneuten ADB-Abfrage am 23.09. war der Emulator offline; die UI-Belege sind vom
21.09. Die Abfrage außerhalb der Sandbox funktionierte, nachdem die erste
Abfrage in der Sandbox am Benutzerverzeichnis gescheitert war.

Die vorhandenen Compose-Dateien bleiben als frühere UI-Skizzen im Quellbaum;
der getestete Build kompiliert bewusst die dependency-freie Java-Variante, damit
er ohne Online-Gradle-Abhängigkeiten funktioniert. Vollständige Gerätetests, insbesondere
Hersteller-Lebenszyklusverhalten und Bedienung mit großer Schrift, stehen noch aus.
Der lokale Entwicklungsschlüssel bleibt aus Git ausgeschlossen.

## 20.10.2026 — Android-Werkzeugkette und Upload-Signierung

Gradle 8.11.1 (offizielle SHA-256 im Wrapper) und AGP 8.10.1 eingesetzt;
unsigned AAB neu gebaut und mit bundletool 1.18.3 validiert. AGP unterstützt
API 36; die frühere Warnung durch AGP 8.7.3 entfällt. Bestehende APKs erhalten.
Separates tools/sign-bundle.mjs mit echter jarsigner- und Zertifikatsprüfung
ergänzt; alle drei Signierregressionen mit echtem AAB und isoliertem temporärem
RSA-3072-Testschlüssel bestanden. Debugalias, bereits signierter Input, ungültiges
Bundle und vorhandene Ausgabe werden abgewiesen. Keine Produktionsschlüssel
erzeugt oder übertragen, kein Play-Upload. Anleitung: SIGNING.md.

## 21.10.2026 — Optionales Google-Testwerbeprofil, Offlineprofil erhalten

Eigenes .adstest-Profil mit GMA Next-Gen 1.5.0 und UMP 4.0.0 implementiert;
ausschließlich offizielle Sample-App-/Banner-IDs. Banner erst nach Ergebnis bzw.
im geöffneten Fake-Bericht, keine Start-/Exit-Interstitials. Consent-Gate vor
SDK-/WorkManager-Initialisierung und Anzeigenanfragen, erforderliche Privacy
Options, begrenzte Netzwerkprüfung, sichere Pause-/Destroy-/Late-Callback-Pfade.
Telefonstatus, Advertising-ID und AdServices-Rechte sowie automatische
ContentProvider explizit aus dem Testmanifest entfernt und beim Export geprüft.
Google-SDK bedeutet weiterhin Netzwerk-/Metadatenverarbeitung, kein Null-Daten-
Versprechen für dieses Profil. Die normale Offline-APK bleibt SDK-/permissionfrei.

Beide Profile erfolgreich kompiliert: ads-test-20261021-3 sowie manueller
Offlinebuild. Unsigned Baseline-AAB mit -PadsTest=false offline gebaut und
bundletool-validiert. 18 reine Consent-/Lifecycle-Gatechecks bestanden;
produktspezifische Offline-Logiktests ebenfalls bestanden. Keine physischen
Geräte verwendet. Eine echte UMP-Formular-/Bannerlieferung wird daraus nicht
abgeleitet: Googles Sample-App-ID ersetzt keine eigene AdMob-Konfiguration.
Buildpfade, Bedienung und Grenzen: ADS-TEST.md. Emulator-QA getrennt dokumentieren.

Maximum Spyware zusätzlich konsistent auf compile/target API 36, Buildtools36,
versionCode4 und Version0.2.2 aktualisiert (Gradle, Manifest, manueller APK-Dateiname).
3797 Scan-Statechecks bestanden. Baseline-AAB hat target36 und keinerlei
uses-permission; APK-Signatur V2/V3 und Alignment bestanden.
