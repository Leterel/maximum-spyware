# Maximum Spyware 0.2.1 — echter UI-Lebenszyklustest

Am 23.09.2026, 23:38–23:39 Uhr (Europe/Berlin), bestanden **13 von 13
Prüfungen** auf `emulator-5570`, Android API 34. Verwendet wurde die bereits
veröffentlichte, unveränderte APK 0.2.1:

`AB5169EA3C6AFAD510BA0EBECCBB704C12A3A0BC5700C30FF1C2429AF319EB5D`

## Beobachteter Ablauf

| Schritt | Ergebnis aus der echten App-Oberfläche |
| --- | --- |
| Scan starten und Pause drücken | Sitzung 1, 2 %, „SPIONAGE MACHT PAUSE.“ |
| Drei Sekunden pausiert warten | Weiterhin 2 % |
| Pausiert ins Querformat drehen | 1920 × 1080; weiterhin Sitzung 1, pausiert, 2 % |
| Weiter und erneut Pause | Fortschritt steigt auf 9 % |
| Weiter, Home, 25.001 ms warten, zurück und Pause | Erst 10 %; die 25 Sekunden zählen nicht zum 22-Sekunden-Scan |
| Weiter bis Abschluss, Bericht öffnen | 100 %, „WIR HABEN NICHTS GEGEN DICH.“ |
| Neuer Scan und Pause | Sitzung 2, neuer Fortschritt bei 1 %; Pause funktioniert |

Der Test bestätigt außerdem, dass die App während des Hintergrundintervalls
tatsächlich nicht das aktive Fenster war. Er bedient Schaltflächen über Androids
Accessibility-Schnittstelle, scrollt zu ihnen und liest den resultierenden
UI-Baum. Die Produktion wird weder instrumentiert noch um einen Testmodus
erweitert. Die Prozentangaben stammen aus dem UI-Baum; im Querformat liegen
einige davon unterhalb des sichtbaren Ausschnitts. Sieben Screenshots ergänzen
die JSON-Belege; Pause und Bericht wurden zusätzlich visuell geprüft.

## Wiederholen und Belege

[Testhelfer und Befehle](tests/runtime/README.md). Der Runner benötigt ausdrücklich
eine `emulator-*`-Seriennummer und prüft vor jedem ADB-Befehl `ro.kernel.qemu=1`.
Der separate Testhelfer wird nach dem Lauf entfernt; Emulatoreinstellungen
werden wiederhergestellt. Es wurde kein physisches Gerät verwendet.

Der erfolgreiche Lauf liegt lokal unter
`build/lifecycle-20260923-233853/`: `verification.json`, `instrumentation.log`
und `lifecycle/` mit vollständigem JSON und sieben PNGs. Frühere Fehlversuche
betrafen ausschließlich den neuen Testhelfer (Null-Prüfung und Messung der
Displayrotation); die Produktions-App musste nicht geändert werden.

Das ergänzt die 13 Display-Matrixprüfungen und 3.797 Zustandsprüfungen. Es bleibt
ein API-34-Emulator: Prozessbeendigung durch Android, Hersteller-Sonderfälle und
weitere Android-Versionen sind damit nicht getestet.
