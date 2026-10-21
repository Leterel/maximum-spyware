# Werbeintegration — optionales Testprofil implementiert

Der normale Build bleibt offline ohne Werbe-/Consent-SDKs. Das separate
`.adstest`-Profil integriert GMA Next-Gen 1.5.0 und UMP 4.0.0 mit offiziellen
Testkennungen. Banner erst am fertigen Ergebnis, Consent vor SDK-Initialisierung
und Anzeigenanfrage, erreichbare erforderliche Privacy Options, keine
Start-/Exit-Interstitials. Anleitung: [ADS-TEST.md](ADS-TEST.md).

Produktiv offen: eigene AdMob-/Betreiberkonfiguration und Privacy-Nachrichten,
Datenschutz-/Data-Safety-Angaben, Produktionssignatur und Store-Veröffentlichung.
Keine Produktivanzeigen oder Einnahmen. Die Sample-App-ID ersetzt keine eigene
Consent-Konfiguration; bei Formular-/Netzfehlern gibt es keine Anzeigenanfrage.
Android-Tests nur im Emulator. Private Schlüssel bleiben außerhalb von Git.
