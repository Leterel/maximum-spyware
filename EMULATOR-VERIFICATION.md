# Maximum Spyware 0.2.1 — Emulatortest am 23.09.2026

Die neue APK verbessert Titel, Simulationshinweis und Kennzahlen bei schmalen
Displays und großer Schrift. Statt sehr schmaler Spalten werden die Inhalte
untereinander angeordnet. Die Scan-Logik und die leere Berechtigungsliste bleiben
unverändert.

SHA-256: `AB5169EA3C6AFAD510BA0EBECCBB704C12A3A0BC5700C30FF1C2429AF319EB5D`.

## Bestanden

| Konfiguration | Ergebnis |
| --- | --- |
| 1080 × 1920, 420 dpi, normale Schrift | Hinweis → Scan → Bericht → neuer Scan |
| 720 × 1280, 360 dpi, 200 % Schrift | Derselbe Ablauf; Titel vollständig lesbar |
| Handy quer, gemessene Rotation 1 | Bericht und Neustart erreichbar |
| 1600 × 2560, 320 dpi, 800 dp kurze Kante | Bericht und Neustart erreichbar |

13 automatische Matrixprüfungen bestanden (Start, Bericht, zweite Sitzung und
Rotation). Screenshots zusätzlich visuell geprüft. 3.797 Prüfungen gegen
`FakeScanState` bestanden beim Build. APK-Signatur V2/V3, Alignment und Manifest
ohne angeforderte Berechtigungen geprüft.

Alle vier Displaykonfigurationen laufen auf **einem API-34-Emulator**. Der Test
ersetzt keine Android-Versionsmatrix, Hersteller-, Foldable- oder Tablet-Hardware-
Prüfung. Pause/Hintergrund und Zustandswiederherstellung haben Unit-Test-Abdeckung,
wurden in dieser UI-Matrix jedoch nicht erneut automatisiert durchlaufen.

## Wiederholen

Den gewünschten API-34-Emulator vorher starten. Aus dem Repository:

```powershell
./tests/emulator-matrix.ps1 -Serial emulator-5570 -App spyware `
  -AdbPath C:\Android\Sdk\platform-tools\adb.exe `
  -ApkPath ./build/outputs/maximum-spyware-0.2.1.apk `
  -OutputDirectory ./build/emulator-matrix
```

Das Skript prüft vor jedem Aufruf die explizite `emulator-*`-Seriennummer und
`ro.kernel.qemu=1`. Es greift nicht auf Standardgeräte zurück. Geänderte
Display-/Schrift-/Rotationseinstellungen stellt es im `finally`-Block wieder her.
UIAutomator benötigt ein ruhendes Fenster; deshalb liest der Test die Hierarchie
nach dem Abschluss des 22-Sekunden-Scans. Die zweite Sitzung wird ausdrücklich
anhand des Sitzungszählers geprüft.

Das Release enthält die APK, dieses Dokument und ein ZIP mit Matrix-JSON,
Screenshots, UI-Hierarchien und Buildlog. Kein physisches Gerät wurde benutzt.
