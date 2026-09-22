# Fortschritt

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
