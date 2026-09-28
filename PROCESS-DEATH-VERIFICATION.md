# Maximum Spyware 0.2.1 — echter Prozessverlust geprüft

Am 25.09.2026, 23:52–23:53 Uhr (Europe/Berlin), bestanden **27 von 27
Prüfungen** auf `emulator-5570`, Android API 34. Getestet wurde die bereits
veröffentlichte, unveränderte APK 0.2.1:

`AB5169EA3C6AFAD510BA0EBECCBB704C12A3A0BC5700C30FF1C2429AF319EB5D`

## Methode und tatsächliche Ergebnisse

Der separate Testhelfer bedient die Produktions-App über Accessibility. Er
bringt sie mit Home in den Hintergrund und beendet den Hintergrundprozess mit
Androids `am kill`. Er prüft, dass die App wirklich im Hintergrund ist, ihre PID
anschließend fehlt, sie mindestens fünf Sekunden beendet bleibt und ein normaler
Launcher-Aufruf einen anderen Prozess erzeugt. Dabei bleibt der vorhandene
Android-Task mit seinem gespeicherten Activity-Zustand erhalten.

| Zustand vor Beendigung | Nach neuem Prozess |
| --- | --- |
| Sitzung 2, pausiert bei 11 %; PID 2253 | PID 2343: Sitzung 2, pausiert bei genau 11 %; auch zwei Sekunden später 11 % |
| Sitzung 2, Weiter; zuvor bei 17 % pausiert; PID 2343 | PID 2427: läuft weiter; erneute Pause bei 21 %, Sitzung 2 bleibt erhalten |
| Bericht bei 100 %, Sitzung 2; PID 2427 | PID 2512: Bericht bleibt bei 100 % und Sitzung 2 |
| Neuer Scan nach wiederhergestelltem Bericht | Sitzung 3 bei 1 %; Pause funktioniert |

Die nachgewiesenen Wartezeiten ohne App-Prozess betrugen 5.024, 5.026 und
5.021 ms. Die laufende Variante erlaubt höchstens 20 Prozentpunkte für die
zusätzliche echte Vordergrundzeit beim Bedienen; tatsächlich waren es vier.
Die fünf Sekunden ohne Prozess würden allein schon über 22 Prozentpunkte
ausmachen und werden nicht fälschlich als Scanzeit gezählt.

Es wurden keine Zustände in die App injiziert, keine Produktionsdateien geändert
und kein neues APK gebaut. `am force-stop` erzeugt ausschließlich den sauberen
Start vor dem Test, nicht die drei geprüften Prozessbeendigungen.

## Wiederholen und Belege

[Befehle im Testhelfer-README](tests/runtime/README.md), Szenario `process-death`.
Vor jedem ADB-Befehl wird `ro.kernel.qemu=1` geprüft; die Seriennummer muss mit
`emulator-` beginnen. Der Helper prüft die Emulator-Eigenschaft zusätzlich selbst.
Der Runner entfernt den Testhelfer und stellt die Display-Einstellungen wieder her.
Es wurde kein physisches Gerät verwendet.

Lokale Belege: `build/process-death-20260925-235219/`, mit `verification.json`,
`instrumentation.log` und `process-death/` (vollständige Prüfergebnisse, PIDs,
UI-Bäume und acht Screenshots). Das ergänzt die Display-Matrix, den separaten
Lebenszyklustest und die 3.797 Zustandsprüfungen.

## Belegprüfung am 27.09.2026

Die Originalbelege vom 25.09. wurden erneut gelesen: alle 27 Detailprüfungen
bestanden, die acht Screenshots und drei unterschiedlichen Prozesswechsel sind
vorhanden. Elf Originaldateien wurden hashgleich nach
`outputs/maximum-spyware/process-death-verification-20260925/` im gemeinsamen
Auslieferungsverzeichnis kopiert. `review-20260927.json` kennzeichnet diesen
Schritt ausdrücklich als Belegprüfung, nicht als neuen Emulatorlauf.
Der SHA-256 der ausgelieferten APK 0.2.1 stimmt weiterhin mit dem Test überein.

Am 28.09. wurden der Testcode und dieselben Originalbelege zur Auslieferung
geprüft. Alle elf Originaldateien stimmen weiterhin bytegenau überein. Das
zusätzliche Archiv
`maximum-spyware-0.2.1-process-death-verification-20260928.zip` enthält diese
Belege, dieses Dokument und interne Dateiprüfsummen. Dieser Abschluss ist kein
neuer Lauf und verändert die APK nicht.

## Grenzen

Dies ist echte Prozessbeendigung und Wiederherstellung auf einem API-34-Emulator,
aber kein erzeugter Speichermangel und kein Hersteller-Gerätetest. Neustart des
Geräts, Force-stop-Persistenz, Task-Entfernen aus der Übersicht, andere Android-
Versionen und herstellerspezifische Hintergrundregeln sind hier nicht geprüft.
