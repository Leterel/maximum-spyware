# Fortschritt

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
